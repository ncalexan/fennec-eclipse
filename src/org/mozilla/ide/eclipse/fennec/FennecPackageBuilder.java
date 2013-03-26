package org.mozilla.ide.eclipse.fennec;

import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import com.android.ide.eclipse.adt.internal.project.ApkInstallManager;

@SuppressWarnings("restriction")
public class FennecPackageBuilder extends FennecCommandBuilder {
    protected static final String ERROR_MSG = "Error(s) packaging Fennec. Check log for details.";
    protected static final String MARKER_ID = "org.mozilla.ide.eclipse.fennec.PackageProblemMarker";
    public static final String ID = "org.mozilla.ide.eclipse.fennec.FennecPackageBuilder";

    @Override
    protected String[] getCommands() {
        // XXX: make this generic
        return new String[] {"/usr/bin/make", "-C", "/home/brian/mozilla/central/objdir-droid", "package"};
    }

    @Override
    protected String getErrorMessage() {
        return ERROR_MSG;
    }

    @Override
    protected String getMarkerId() {
        return MARKER_ID;
    }

    @Override
    protected IProject[] build(int kind, @SuppressWarnings("rawtypes") Map args, IProgressMonitor monitor)
            throws CoreException {
        super.build(kind, args, monitor);

        // force the new apk to be installed
        ApkInstallManager.getInstance().resetInstallationFor(getProject());

        return null;
    }
}
