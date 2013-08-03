package net.pyxzl.orayen.client;

/**
 * Interface to be implemented by objects that are supposed to be passed to
 * {@link OrayenClient#registerConfigListener(ConfigListener)}.
 * 
 * @author Ravi Gairola (mallox@pyxzl.net)
 */
public interface ConfigListener {
	/**
	 * Will get passed on the current configuration stored on the server.
	 * 
	 * @param config
	 */
	void onConfigChanged(final String config);
}
