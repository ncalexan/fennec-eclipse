package org.mozilla.ide.eclipse.fennec;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;

import com.android.ide.eclipse.adt.internal.project.ApkInstallManager;

@SuppressWarnings("restriction")
public class FennecPackageBuilder extends FennecCommandBuilder {
    protected static final String MARKER_ID = "org.mozilla.ide.eclipse.fennec.PackageProblemMarker";
    public static final String ID = "org.mozilla.ide.eclipse.fennec.FennecPackageBuilder";

    @Override
    protected String[] getCommands() throws CoreException {
        // XXX: make "make" location generic
        return new String[] {"/usr/bin/make", "-C", getObjDir(), "package"};
    }

    @Override
    protected String getMarkerId() {
        return MARKER_ID;
    }

    @Override
    protected void postBuild() throws CoreException {
        IProject project = getProject();

        // check objdir/dist
        File dir = new File(getObjDir(), "dist");
        if (!dir.exists()) {
            throw new CoreException(new Status(Status.ERROR, FennecActivator.PLUGIN_ID,
                    dir.getPath() + " does not exist."));
        }

        // find the most recent apk
        File fennecApk = null;
        for (File file : dir.listFiles()) {
            String fileName = file.getName();
            if (fileName.matches("^fennec-.*\\.apk$")) {
                if (fennecApk == null || fennecApk.lastModified() < file.lastModified()) {
                    fennecApk = file;
                }
            }
        }
        if (fennecApk == null) {
            throw new CoreException(new Status(Status.ERROR, FennecActivator.PLUGIN_ID,
                    "No APK found in " + dir.getPath()));
        }

        // copy the moz build apk into the project
        File dest = FennecLauncher.getApplicationPackage(project);
        if (dest == null) {
            throw new CoreException(new Status(Status.ERROR, FennecActivator.PLUGIN_ID,
                    "Could not determine APK destination."));
        }
        try {
            Files.copy(fennecApk.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new CoreException(new Status(Status.ERROR, FennecActivator.PLUGIN_ID,
                    e.getMessage(), e));
        }

        // force the new apk to be installed
        ApkInstallManager.getInstance().resetInstallationFor(getProject());

        super.postBuild();
    }
}
