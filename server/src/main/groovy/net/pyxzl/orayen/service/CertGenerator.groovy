package net.pyxzl.orayen.service

import groovy.util.logging.Slf4j

import java.security.Key
import java.security.KeyStore
import java.security.cert.Certificate

import net.pyxzl.orayen.Config.Setting

/**
 * @author Ravi Gairola (mallox@pyxzl.net)
 */
@Slf4j
@Singleton
class CertGenerator {
	private static final char[] SERVER_PASSWORD = Setting.CERTPASS.value as char[]

	/**
	 * Create the key and trust stores if they don't already exist.
	 */
	CertGenerator() {
		final File trustStoreFile = new File(Setting.TRUSTSTORE.value)
		final File keyStoreFile = new File(Setting.KEYSTORE.value)

		if (trustStoreFile.exists() && keyStoreFile.exists()) {
			log.trace 'Skipping creating key and trust store, as both already exist'
			KeyService.instance.storeKeys(keyStoreFile)
			return
		}

		trustStoreFile.parentFile.mkdirs()
		final FileOutputStream trustStoreOut = new FileOutputStream(trustStoreFile)
		trustStoreOut.write KeyService.instance.rootCertificateJKS
		trustStoreOut.close()
		log.info "Created new trust store at ${Setting.TRUSTSTORE}"

		keyStoreFile.parentFile.mkdirs()
		final FileOutputStream keyStoreOut = new FileOutputStream(keyStoreFile)
		keyStoreOut.write KeyService.instance.rootKeysJKS
		keyStoreOut.close()
		log.info "Created new trust store at ${Setting.KEYSTORE}"
	}

	/**
	 * Generate a new key for a new client that can be used to establish a https connection
	 * @param clientName
	 * @deprecated The client key should not be stored on the file system, but made available for download to an authenticated user.
	 */
	@Deprecated
	void createClientKey(String clientId) {
		final File clientStoreFile = new File(Setting.CERTSTORE.value + clientId + '.p12')
		if (!clientStoreFile.exists()) {
			clientStoreFile.parentFile.mkdirs()
			final Certificate[] clientCerts = [
				KeyService.instance.endCredential.cert,
				KeyService.instance.interCredential.cert,
				KeyService.instance.rootCredential.cert
			]
			final Key privateKey = KeyService.instance.endCredential.key
			final KeyStore clientStore = KeyStore.getInstance('PKCS12')
			clientStore.load(null, null)
			clientStore.setKeyEntry(clientId, privateKey, SERVER_PASSWORD, clientCerts)
			clientStore.store(new FileOutputStream(clientStoreFile), SERVER_PASSWORD)
			log.info "Created new client key file at ${Setting.CERTSTORE.value}${clientId}.p12"
		} else {
			log.trace "Skipped creating client key for ${clientId} as it already existed"
		}
	}
}
