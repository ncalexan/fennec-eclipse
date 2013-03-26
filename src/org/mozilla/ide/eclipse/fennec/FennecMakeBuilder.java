package org.mozilla.ide.eclipse.fennec;

public class FennecMakeBuilder extends FennecCommandBuilder {
    protected static final String ERROR_MESSAGE = "Error(s) making Fennec. Check log for details.";
    protected static final String MARKER_ID = "org.mozilla.ide.eclipse.fennec.MakeProblemMarker";
    public static final String ID = "org.mozilla.ide.eclipse.fennec.FennecMakeBuilder";

    @Override
    protected String[] getCommands() {
        // XXX: make this generic
        return new String[] {"/usr/bin/make", "-C", "/home/brian/mozilla/central/objdir-droid/mobile/android/base"};
    }

    @Override
    protected String getMarkerId() {
        return MARKER_ID;
    }

    @Override
    protected String getErrorMessage() {
        return ERROR_MESSAGE;
    }
}
