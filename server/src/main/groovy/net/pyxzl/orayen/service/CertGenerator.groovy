package net.pyxzl.orayen.service

import groovy.util.logging.Slf4j

import java.security.Key
import java.security.KeyStore
import java.security.Security
import java.security.cert.Certificate

import javax.security.auth.x500.X500PrivateCredential

import net.pyxzl.orayen.Config.Setting

@Slf4j
@Singleton
class CertGenerator {
	private static char[] SERVER_PASSWORD = Setting.CERTPASS.value as char[]

	/**
	 * Create the key and trust stores if they don't already exist.
	 */
	CertGenerator() {
		final File trustStoreFile = new File(Setting.TRUSTSTORE.value)
		final File keyStoreFile = new File(Setting.KEYSTORE.value)

		if (trustStoreFile.exists() && keyStoreFile.exists()) {
			log.trace "Skipping creating key and trust store, as both already exist"
			return
		}

		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider())

		final X500PrivateCredential rootCredential = KeyService.instance.rootCredential
		final X500PrivateCredential interCredential = KeyService.instance.interCredential
		final X500PrivateCredential endCredential = KeyService.instance.endCredential

		trustStoreFile.parentFile.mkdirs()
		final KeyStore trustStore = KeyStore.getInstance("JKS")
		trustStore.load(null, null)
		trustStore.setCertificateEntry(KeyService.ROOT_ALIAS, rootCredential.getCertificate())
		trustStore.store(new FileOutputStream(trustStoreFile), SERVER_PASSWORD)
		log.info "Created new trust store at ${Setting.TRUSTSTORE}"


		keyStoreFile.parentFile.mkdirs()
		final Certificate[] serverChain = new Certificate[1]
		serverChain[0] = rootCredential.getCertificate()

		final Certificate[] interChain = new Certificate[2]
		interChain[0] = endCredential.getCertificate()
		interChain[1] = interCredential.getCertificate()

		final Certificate[] clientChain = new Certificate[3]
		clientChain[0] = endCredential.getCertificate()
		clientChain[1] = interCredential.getCertificate()
		clientChain[2] = rootCredential.getCertificate()

		final KeyStore keyStore = KeyStore.getInstance("JKS")
		keyStore.load(null, null)
		keyStore.setKeyEntry(KeyService.ROOT_ALIAS, rootCredential.getPrivateKey(), SERVER_PASSWORD, serverChain)
		keyStore.setKeyEntry(KeyService.INTERMEDIATE_ALIAS, interCredential.getPrivateKey(), SERVER_PASSWORD, interChain)
		keyStore.setKeyEntry(KeyService.END_ENTITY_ALIAS,endCredential.getPrivateKey(),SERVER_PASSWORD,clientChain)
		keyStore.store(new FileOutputStream(keyStoreFile), SERVER_PASSWORD)
		log.info "Created new trust store at ${Setting.KEYSTORE}"
	}

	/**
	 * Generate a new key for a new client that can be used to establish a https connection
	 * @param clientName
	 */
	void createClientKey(String clientId) {
		final File clientStoreFile = new File(Setting.CERTSTORE.value + clientId + ".p12")
		if (!clientStoreFile.exists()) {
			clientStoreFile.parentFile.mkdirs()
			final KeyStore keyStore = KeyStore.getInstance("JKS")
			keyStore.load(new FileInputStream(Setting.KEYSTORE.value), SERVER_PASSWORD)
			final Certificate[] clientCerts = keyStore.getCertificateChain(KeyService.END_ENTITY_ALIAS)
			final Key privateKey = keyStore.getKey(KeyService.END_ENTITY_ALIAS, SERVER_PASSWORD)
			final KeyStore clientStore = KeyStore.getInstance("PKCS12")
			clientStore.load(null, null)
			clientStore.setKeyEntry(clientId, privateKey, SERVER_PASSWORD, clientCerts)
			clientStore.store(new FileOutputStream(clientStoreFile), SERVER_PASSWORD)
			log.info "Created new client key file at ${Setting.CERTSTORE.value}${clientId}.p12"
		} else {
			log.trace "Skipped creating client key for ${clientId} as it already existed"
		}
	}
}
