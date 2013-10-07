package net.pyxzl.orayen.service

import groovy.util.logging.Slf4j
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
}
