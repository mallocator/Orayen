package net.pyxzl.orayen.client;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Orayen client, that bundles communicating with the Orayen configuration server.
 * 
 * @TODO Document example on how to use client.
 * @author Ravi Gairola (mallox@pyxzl.net)
 */
public class OrayenClient {
	private static final Logger				log						= LoggerFactory.getLogger(OrayenClient.class);
	private String							password				= "Orayen";
	private String							host;
	private int								port					= 7443;
	private File							trustStore;
	private File							clientStore;
	private String							clientId;
	private int								pollingInterval;
	private final Connection				connection				= new Connection(this);
	private final Set<ConfigListener>		configListeners			= new HashSet<>();
	private final Set<NotificationListener>	notificationListeners	= new HashSet<>();

	/**
	 * Fetch the current configuration for this client.
	 * 
	 * @return
	 * @throws IOException
	 * @throws GeneralSecurityException
	 */
	public String getConfig() throws GeneralSecurityException, IOException {
		return this.connection.connect("/config/" + this.clientId);
	}

	/**
	 * Fetch the current unread notification for this client (if any).
	 * 
	 * @return Null or the String message sent by the server.
	 * @throws IOException
	 * @throws GeneralSecurityException
	 */
	public String getNotification() throws GeneralSecurityException, IOException {
		return this.connection.connect("/notification/" + this.clientId);
	}

	/**
	 * Add a listener that will be called whenever a configuration has changed. The listener will either be called when
	 * polling is active, or if polling is deactivated, when a push is coming from the server. (the client environment needs
	 * to support open sockets for this).
	 * 
	 * @param listener
	 * @return
	 */
	public OrayenClient registerConfigListener(final ConfigListener listener) {
		if (!verifyConfig()) {
			throw new RuntimeException("The configuration has not been set up properly. Please check the logs and refer to the documentation if necessary");
		}
		this.configListeners.add(listener);
		return this;
	}

	/**
	 * Add a listener that will be called whenever a new notification is available. The listener will either be called when
	 * polling is active, or if polling is deactivated, when a push is coming from the server. (the client environment needs
	 * to support open sockets for this).
	 * 
	 * @param listener
	 * @return
	 */
	public OrayenClient registerNotificationListener(final NotificationListener listener) {
		if (!verifyConfig()) {
			throw new RuntimeException("The configuration has not been set up properly. Please check the logs and refer to the documentation if necessary");
		}
		this.notificationListeners.add(listener);
		return this;
	}

	private boolean verifyConfig() {
		boolean valid = true;
		if (this.password == null || this.password.trim().isEmpty()) {
			log.warn("No password has been set!");
			valid = false;
		}
		if (this.host == null || this.host.trim().isEmpty()) {
			log.warn("No host has been set!");
			valid = false;
		}
		if (this.port < 1 || this.port > 65535) {
			log.warn("No valid port has been set!");
			valid = false;
		}
		if (this.trustStore == null || !this.trustStore.exists() || !this.trustStore.isFile() || !this.trustStore.canRead()) {
			log.warn("TrustStore file is invalid or not set!");
			valid = false;
		}
		if (this.clientStore == null || !this.clientStore.exists() || !this.clientStore.isFile() || !this.clientStore.canRead()) {
			log.warn("TrustStore file is invalid or not set!");
			valid = false;
		}
		if (this.clientId == null || this.clientId.trim().isEmpty()) {
			log.warn("Client ID has not been set!");
			valid = false;
		}
		return valid;
	}

	/**
	 * Set the password that has been configured on the server as keystore password.
	 * 
	 * @param password Default is set to the default password of the server
	 * @return
	 */
	public OrayenClient setKeyPassword(final String password) {
		this.password = password;
		return this;
	}

	/**
	 * Set the host address under which the Orayen server is reachable. For example "localhost" or "192.168.1.1".
	 * 
	 * @param host
	 * @return
	 */
	public OrayenClient setHost(final String host) {
		this.host = host;
		return this;
	}

	/**
	 * Set the port on which the Orayen server is listening for https connections.
	 * 
	 * @param port Default is set to 7443
	 * @return
	 */
	public OrayenClient setPort(final int port) {
		this.port = port;
		return this;
	}

	/**
	 * Pass the trust store file (the .jks file) given by the server here.
	 * 
	 * @param trustStore
	 * @return
	 */
	public OrayenClient setTrustStore(final File trustStore) {
		this.trustStore = trustStore;
		return this;
	}

	/**
	 * Set the path to the trust store file (the .jks file) given by the server.
	 * 
	 * @param trustStorePath
	 * @return
	 * @throws FileNotFoundException
	 */
	public OrayenClient setTrustStore(final String trustStorePath) throws FileNotFoundException {
		this.setTrustStore(new File(trustStorePath));
		return this;
	}

	/**
	 * Pass the client store file (the .p12 file) given by the server here.
	 * 
	 * @param trustStore
	 * @return
	 */
	public OrayenClient setClientStore(final File clientStore) {
		this.clientStore = clientStore;
		return this;
	}

	/**
	 * Set the path to the client store file (the .p12 file) given by the server.
	 * 
	 * @param trustStorePath
	 * @return
	 * @throws FileNotFoundException
	 */
	public OrayenClient setClientStore(final String clientStorePath) throws FileNotFoundException {
		this.setClientStore(new File(clientStorePath));
		return this;
	}

	/**
	 * Set the clientId with which this client will authenticate with the server.
	 * 
	 * @param clientId
	 * @return
	 */
	public OrayenClient setClientId(final String clientId) {
		this.clientId = clientId;
		return this;
	}

	/**
	 * If you want to poll the server for changes, then use this setting to set the interval in seconds. This setting is
	 * deactivated by default (interval = 0)
	 * 
	 * @param interval
	 * @return
	 */
	public OrayenClient setPollingInterval(final int interval) {
		this.pollingInterval = interval;
		return this;
	}

	public char[] getPassword() {
		return this.password.toCharArray();
	}

	public String getHost() {
		return this.host;
	}

	public int getPort() {
		return this.port;
	}

	public File getTrustStore() {
		return this.trustStore;
	}

	public File getClientStore() {
		return this.clientStore;
	}

	public String getClientId() {
		return this.clientId;
	}

	public int getPollingInterval() {
		return this.pollingInterval;
	}
}
