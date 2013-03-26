package org.mozilla.ide.eclipse.fennec;

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
}
