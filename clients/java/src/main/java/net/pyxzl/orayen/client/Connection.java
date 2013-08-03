package net.pyxzl.orayen.client;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.security.auth.x500.X500Principal;

/**
 * This class is used to create an SSL context and a HTTPS connection with which the client can communicate with the server,
 * using client certificate authentication.
 * 
 * @author Ravi Gairola (mallox@pyxzl.net)
 */
class Connection {
	private final OrayenClient	config;

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

	Connection(final OrayenClient orayenClient) {
		this.config = orayenClient;
	}

	/**
	 * Connect to a server and ask for sample data.
	 * 
	 * @throws IOException
	 * @throws GeneralSecurityException
	 * @throws Exception
	 */
	String connect(final String uri) throws GeneralSecurityException, IOException {
		final SSLContext sslContext = createSSLContext();
		final SSLSocketFactory fact = sslContext.getSocketFactory();
		final URL url = new URL("https://" + this.config.getHost() + ":" + this.config.getPort() + uri);
		final HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
		connection.setSSLSocketFactory(fact);
		connection.setHostnameVerifier(new Validator());
		connection.connect();

		final InputStream in = connection.getInputStream();
		Scanner s = new Scanner(in, "UTF-8");
		s.useDelimiter("\\A");
		final String response = s.hasNext() ? s.next() : null;
		s.close();
		connection.disconnect();
		return response;
	}

	/**
	 * Create an SSL context with both identity and trust store.
	 * 
	 * @throws NoSuchAlgorithmException
	 * @throws GeneralSecurityException
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	private SSLContext createSSLContext() throws GeneralSecurityException, IOException {
		final KeyManagerFactory mgrFact = KeyManagerFactory.getInstance("SunX509");
		final KeyStore clientStore = KeyStore.getInstance("PKCS12");
		clientStore.load(new FileInputStream(this.config.getClientStore()), this.config.getPassword());
		mgrFact.init(clientStore, this.config.getPassword());

		final TrustManagerFactory trustFact = TrustManagerFactory.getInstance("SunX509");
		final KeyStore trustStore = KeyStore.getInstance("JKS");
		trustStore.load(new FileInputStream(this.config.getTrustStore()), this.config.getPassword());
		trustFact.init(trustStore);

		final SSLContext sslContext = SSLContext.getInstance("TLS");
		sslContext.init(mgrFact.getKeyManagers(), trustFact.getTrustManagers(), null);
		return sslContext;
	}
}
