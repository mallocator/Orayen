package net.pyxzl.orayen.client;

/**
 * Interface to be implemented by objects that are supposed to be passed to
 * {@link OrayenClient#registerNotificationListener(NotificationListener)}.
 * 
 * @author Ravi Gairola (mallox@pyxzl.net)
 */
public interface NotificationListener {
	/**
	 * The returned message will either be null or the String set by the server.
	 * 
	 * @param message
	 */
	void onNotification(final String message);
}
