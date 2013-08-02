package net.pyxzl.orayen.client;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.security.KeyStore;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.security.auth.x500.X500Principal;

public class Connection {
	static char[]	SERVER_PASSWORD	= "OrayenServer".toCharArray();
	static char[]	CLIENT_PASSWORD	= "OrayenClient".toCharArray();

	private static class Validator implements HostnameVerifier {
		@Override
		public boolean verify(final String hostName, final SSLSession session) {
			try {
				final X500Principal hostID = (X500Principal) session.getPeerPrincipal();
				return hostID.getName().equals("CN=Orayen CA Certificate");
			} catch (Exception e) {
				return false;
			}
		}
	}

	/**
	 * Connect to a server and ask for sample data.
	 * 
	 * @throws Exception
	 */
	public void connect() throws Exception {
		final SSLContext sslContext = createSSLContext();
		final SSLSocketFactory fact = sslContext.getSocketFactory();

		final URL url = new URL("https://localhost:7443/config/1");
		HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
		connection.setSSLSocketFactory(fact);
		connection.setHostnameVerifier(new Validator());
		connection.connect();

		// read the response
		final InputStream in = connection.getInputStream();
		int ch = 0;
		while ((ch = in.read()) >= 0) {
			System.out.print((char) ch);
		}
	}

	/**
	 * Create an SSL context with both identity and trust store.
	 */
	SSLContext createSSLContext() throws Exception {
		final KeyManagerFactory mgrFact = KeyManagerFactory.getInstance("SunX509");
		final KeyStore clientStore = KeyStore.getInstance("PKCS12");
		clientStore.load(new FileInputStream("OrayenClient.p12"), CLIENT_PASSWORD);
		mgrFact.init(clientStore, CLIENT_PASSWORD);

		final TrustManagerFactory trustFact = TrustManagerFactory.getInstance("SunX509");
		final KeyStore trustStore = KeyStore.getInstance("JKS");
		trustStore.load(new FileInputStream("trustStore.jks"), SERVER_PASSWORD);
		trustFact.init(trustStore);

		final SSLContext sslContext = SSLContext.getInstance("TLS");
		sslContext.init(mgrFact.getKeyManagers(), trustFact.getTrustManagers(), null);

		return sslContext;
	}
}
