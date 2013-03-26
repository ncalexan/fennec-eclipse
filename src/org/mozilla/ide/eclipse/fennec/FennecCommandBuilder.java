package org.mozilla.ide.eclipse.fennec;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
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

    // XXX: will each project have its own builder (i.e., is this set necessary?)
    private HashSet<IProject> mUnbuiltProjects = new HashSet<IProject>();

    @Override
    protected IProject[] build(
            int kind,
            @SuppressWarnings("rawtypes") Map args,
            IProgressMonitor monitor)
            throws CoreException {

        System.err.println("building..." + kind + ", " + getMarkerId());
        IProject project = getProject();

        if (kind != FULL_BUILD) {
            mUnbuiltProjects.add(project);
            return null;
        }

        if (!mUnbuiltProjects.contains(project)) {
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

        monitor.beginTask("buildtask", 30);

        try {
            // XXX: make this generic
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
}
