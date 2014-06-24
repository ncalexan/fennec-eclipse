package org.mozilla.ide.eclipse.fennec;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;

import com.android.ide.eclipse.adt.AdtPlugin;
import com.android.ide.eclipse.adt.internal.build.builders.BaseBuilder;
import com.android.ide.eclipse.adt.internal.preferences.AdtPrefs;
import com.android.ide.eclipse.adt.internal.preferences.AdtPrefs.BuildVerbosity;

@SuppressWarnings("restriction")
public class FennecMakeBuilder extends BaseBuilder {
    public static final String ID = "org.mozilla.ide.eclipse.fennec.FennecMakeBuilder";
    public static final String MARKER_ID = "org.mozilla.ide.eclipse.fennec.MakeProblemMarker";

	private static final String KEY_COMMAND = "command";
	private static final String KEY_WORKING_DIRECTORY = "workingDirectory";
	private static final File DEFAULT_WORKING_DIRECTORY = null; // Current process working directory.

	private static final String KEY_IGNORE_INPUTS_MATCHING = "ignoreInputsMatching";
	private static final String DEFAULT_IGNORE_INPUTS_MATCHING = "DERIVED|bin/.*$";

	private Process getProcess(@SuppressWarnings("rawtypes") Map args) throws IOException, CoreException {
		final File workingDirectoryFile;
		final String workingDirectory = (String) args.get(KEY_WORKING_DIRECTORY);
		if (workingDirectory != null && !workingDirectory.trim().isEmpty()) {
			workingDirectoryFile = new File(workingDirectory);
		} else {
			workingDirectoryFile = DEFAULT_WORKING_DIRECTORY;
		}
		final String[] commandArgs = ((String) args.get(KEY_COMMAND)).trim().split("\\s+");
		return Runtime.getRuntime().exec(commandArgs, null, workingDirectoryFile);
	}

	@SuppressWarnings("rawtypes")
	protected String getIgnoreInputsMatching(Map args) {
		String ignoreInputsMatching = (String) args.get(KEY_IGNORE_INPUTS_MATCHING);
		if (ignoreInputsMatching == null) {
			return DEFAULT_IGNORE_INPUTS_MATCHING;
		}
		return ignoreInputsMatching;
	}

	@Override
	protected IProject[] build(
			int kind,
			@SuppressWarnings("rawtypes") Map args,
			final IProgressMonitor monitor)
					throws CoreException {
		if (monitor.isCanceled()) {
			throw new CoreException(new Status(Status.CANCEL, FennecActivator.PLUGIN_ID,
					"Build interrupted!"));
		}

		final IProject project = getProject();
		final IResourceDelta delta = getDelta(project);

		final String ignoreInputsMatching = getIgnoreInputsMatching(args);

		String reason = null;
		if (delta == null || kind == FULL_BUILD) {
			reason = "full build";
		} else {
			IResource resource = inputResourceChanged(project, ignoreInputsMatching);
			if (resource != null) {
				reason = resource.getFullPath().toPortableString() + " changed";
			}
		}

		if (reason == null) {
			final String message = "Project " + project.getName() + " does not need resource generation.";
			AdtPlugin.printBuildToConsole(BuildVerbosity.ALWAYS, project, message);
			return null;
		}

		final String message = "Project " + project.getName() + " needs resource generation: " + reason + ".";
		AdtPlugin.printBuildToConsole(BuildVerbosity.ALWAYS, project, message);

		// Clear any markers from this given build type.
		getProject().deleteMarkers(MARKER_ID, true, IResource.DEPTH_INFINITE);

		monitor.beginTask("Building", 10);
		Timer cancelCheckTimer = null;

		try {
			final Process process = getProcess(args);

			TimerTask cancelTimerTask = new TimerTask() {
				@Override
				public void run() {
					if (monitor.isCanceled()) {
						process.destroy();
					}
				}
			};

			cancelCheckTimer = new Timer(true);
			cancelCheckTimer.scheduleAtFixedRate(cancelTimerTask, 0, 1000);

			ArrayList<String> out = new ArrayList<String>();
			ArrayList<String> err = new ArrayList<String>();
			int returnCode = grabProcessOutput(process, project, out, err);

			if (returnCode != 0) {
				IMarker marker = project.createMarker(MARKER_ID);
				marker.setAttribute(IMarker.MESSAGE, "Error(s) generating resources for " + project.getName() + ". Check console log for details.");
						marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
						AdtPlugin.printErrorToConsole(getProject(), out.toArray());
						return null;
			} else {
				AdtPlugin.printBuildToConsole(BuildVerbosity.NORMAL, project, out.toArray());
			}

			getProject().refreshLocal(IResource.DEPTH_INFINITE, monitor);

			monitor.worked(10);
		} catch (Exception e) {
			if (monitor.isCanceled()) {
				AdtPlugin.printErrorToConsole(getProject(), "Build interrupted!");
				throw new CoreException(new Status(Status.CANCEL, FennecActivator.PLUGIN_ID,
						e.getMessage(), e));
			} else {
				IMarker marker = project.createMarker(MARKER_ID);
				marker.setAttribute(IMarker.MESSAGE, e.getMessage());
				marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
				throw new CoreException(new Status(Status.ERROR, FennecActivator.PLUGIN_ID,
						e.getMessage(), e));
			}
		} finally {
			monitor.done();
			if (cancelCheckTimer != null) {
				cancelCheckTimer.cancel();
			}
		}

		return null;
	}

	protected int grabProcessOutput(Process process, final IProject project, final ArrayList<String> out, final ArrayList<String> err) throws InterruptedException {
		return GrabProcessOutput.grabProcessOutput(
				process,
				GrabProcessOutput.Wait.WAIT_FOR_PROCESS,
				new GrabProcessOutput.IProcessOutput() {
					@Override
					public void out(String line) {
						if (line != null) {
							out.add(line);
							if (BuildVerbosity.VERBOSE == AdtPrefs.getPrefs().getBuildVerbosity()) {
								AdtPlugin.printBuildToConsole(BuildVerbosity.VERBOSE, project, line);
							}
						}
					}

					@Override
					public void err(String line) {
						if (line != null) {
							err.add(line);
							if (BuildVerbosity.VERBOSE == AdtPrefs.getPrefs().getBuildVerbosity()) {
								AdtPlugin.printErrorToConsole(project, line);
							}
						}
					}
				});
	}

	protected static class ResourceDeltaVisitor implements IResourceDeltaVisitor {
		public IResource mResource = null;

		protected final Pattern mIgnorePattern;
		protected final boolean mIgnoreDerived;

		public ResourceDeltaVisitor(String ignoreInputsMatching) {
			this.mIgnorePattern = Pattern.compile(ignoreInputsMatching);
			this.mIgnoreDerived = this.mIgnorePattern.matcher("DERIVED").matches();
		}

		@Override
		public boolean visit(IResourceDelta delta) {
			if (mResource != null) {
				// Early abort if possible.
				return false;
			}

			final IResource resource = delta.getResource();

			// We only care about source inputs.
			if (mIgnoreDerived && resource.isDerived()) {
				return false;
			}

			// We only care about files.
			if (resource.getType() != IResource.FILE) {
				return true;
			}

			// And things we haven't been told to ignore.
			if (mIgnorePattern.matcher(resource.getProjectRelativePath().toPortableString()).matches()) {
				return true;
			}

			// And we only care about things that have actually changed.
			int flags = delta.getFlags();
			if (flags != IResourceDelta.NO_CHANGE) {
				mResource = resource;
				return false;
			}

			return true;
		}
	}

	protected IResource inputResourceChanged(IProject project, String ignoreInputsMatching) throws CoreException {
		IResourceDelta delta = getDelta(project);

		final ResourceDeltaVisitor visitor = new ResourceDeltaVisitor(ignoreInputsMatching);
		delta.accept(visitor);

		return visitor.mResource;
	}
}
