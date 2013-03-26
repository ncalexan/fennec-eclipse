package org.mozilla.ide.eclipse.fennec;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import com.android.ide.eclipse.adt.AdtPlugin;
import com.android.ide.eclipse.adt.internal.build.builders.BaseBuilder;
import com.android.ide.eclipse.adt.internal.preferences.AdtPrefs.BuildVerbosity;
import com.android.ide.eclipse.adt.internal.project.ProjectHelper;

@SuppressWarnings("restriction")
public abstract class FennecCommandBuilder extends BaseBuilder {
    protected abstract String[] getCommands();
    protected abstract String getErrorMessage();
    protected abstract String getMarkerId();

    private static final LinkedList<String> sWatchedResources = new LinkedList<String>();

    static {
        sWatchedResources.add("AndroidManifest.xml");
        sWatchedResources.add("res");
        sWatchedResources.add("src");
    }

    // XXX: will each project have its own builder (i.e., is this set necessary?)
    private HashSet<IProject> mUnbuiltProjects = new HashSet<IProject>();

    @Override
    protected IProject[] build(
            int kind,
            @SuppressWarnings("rawtypes") Map args,
            IProgressMonitor monitor)
            throws CoreException {

        IProject project = getProject();
        boolean needsBuild = mUnbuiltProjects.contains(project);

        if (kind != FULL_BUILD) {
            if (!needsBuild) {
                // If a watched resource changed, mark the project as build
                // needed. The next time a full build occurs, the moz
                // make/package will execute.
                IResourceDelta delta = getDelta(project);
                if (delta == null || watchedResourceChanged(delta)) {
                    mUnbuiltProjects.add(project);
                }
            }
            return null;
        }

        if (!needsBuild) {
            // no changes; do nothing
            return null;
        }

        // clear any markers from this given build type
        getProject().deleteMarkers(getMarkerId(), true, IResource.DEPTH_INFINITE);

        // don't build if there are any errors
        if (ProjectHelper.hasError(project, true)) {
            return null;
        }

        // clear any previous errors from the console
        AdtPlugin.getDefault().getAndroidConsole().clearConsole();

        monitor.beginTask("Building", 10);

        try {
            Process proc = Runtime.getRuntime().exec(getCommands());
            ArrayList<String> err = new ArrayList<String>();
            int returnCode = grabProcessOutput(proc, err);
            if (returnCode != 0) {
                AdtPlugin.printErrorToConsole(getProject(), err.toArray());
                IMarker marker = project.createMarker(getMarkerId());
                marker.setAttribute(IMarker.MESSAGE, getErrorMessage());
                marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
            } else {
                AdtPlugin.printBuildToConsole(BuildVerbosity.NORMAL,
                        project, err.toArray());
            }
            monitor.worked(10);
        } catch (IOException | InterruptedException e) {
            //XXX: handle this gracefully
            e.printStackTrace();
        } finally {
            monitor.done();
        }

        // build completed successfully
        mUnbuiltProjects.remove(project);

        return null;
    }

    private boolean watchedResourceChanged(IResourceDelta delta) {
        IResourceDelta[] children = delta.getAffectedChildren();
        for (IResourceDelta child : children) {
            if (sWatchedResources.contains(child.getResource().getName())) {
                return true;
            }
        }
        return false;
    }
}
