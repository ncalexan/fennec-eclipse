package org.mozilla.ide.eclipse.fennec;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class FennecActivator implements BundleActivator {

	private static BundleContext context;
	public static final String PLUGIN_ID = "org.mozilla.ide.eclipse.fennec";

	static BundleContext getContext() {
		return context;
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext bundleContext) throws Exception {
		FennecActivator.context = bundleContext;
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext bundleContext) throws Exception {
		FennecActivator.context = null;
	}

}
