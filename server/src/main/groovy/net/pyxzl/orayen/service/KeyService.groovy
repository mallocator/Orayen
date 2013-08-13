package net.pyxzl.orayen.service

import groovy.util.logging.Slf4j

import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.cert.X509Certificate

import javax.security.auth.x500.X500Principal
import javax.security.auth.x500.X500PrivateCredential

import net.pyxzl.orayen.Config.Setting
import net.pyxzl.orayen.dao.CredentialsDAO

import org.bouncycastle.asn1.x509.BasicConstraints
import org.bouncycastle.asn1.x509.KeyUsage
import org.bouncycastle.asn1.x509.X509Extensions
import org.bouncycastle.x509.X509V1CertificateGenerator
import org.bouncycastle.x509.X509V3CertificateGenerator
import org.bouncycastle.x509.extension.AuthorityKeyIdentifierStructure
import org.bouncycastle.x509.extension.SubjectKeyIdentifierStructure

/**
 * @author Ravi Gairola (mallox@pyxzl.net)
 */
@SuppressWarnings('deprecation')
@Slf4j
@Singleton
class KeyService {
	static final String ROOT_ALIAS = 'root'
	static final String INTERMEDIATE_ALIAS = 'intermediate'
	static final String END_ENTITY_ALIAS = 'end'
	private static final char[] SERVER_PASSWORD = Setting.CERTPASS.value as char[]
	private static final String ES_TYPE = 'ssl'
	private static final int VALIDITY_PERIOD = 10 * 365 * 24 * 60 * 60 * 1000 // ten years
	private final X500PrivateCredential rootCredential
	private final X500PrivateCredential interCredential
	private final X500PrivateCredential endCredential
	private final boolean keyExist = false

	private KeyService() {
		final X500PrivateCredential root = CredentialsDAO.instance.get(ROOT_ALIAS)
		final X500PrivateCredential inter = CredentialsDAO.instance.get(ROOT_ALIAS)
		final X500PrivateCredential end = CredentialsDAO.instance.get(ROOT_ALIAS)

		if (root != null && inter != null && end != null) {
			this.keyExist = true
			this.rootCredential = root
			this.interCredential = inter
			this.endCredential = end
			return
		}

		this.rootCredential = CredentialsDAO.instance.put(createRootCredential())
		this.interCredential = CredentialsDAO.instance.put(createIntermediateCredential(rootCredential.privateKey, rootCredential.certificate))
		this.endCredential = CredentialsDAO.instance.put(createEndEntityCredential(interCredential.privateKey, interCredential.certificate))
	}

	/**
	 * Stores the given keys in the database if they didn't exist there in the first place.
	 * This allows a user to add his own keys from a custom trust store, when the keys have been deleted from the database.
	 *
	 * @param keyStoreFile The Java File where the key store file with the existing credentials can be found.
	 */
	void storeKeys(final File keyStoreFile) {
		if (keyExist) {
			return
		}
		final KeyStore keyStore = KeyStore.getInstance('JKS')
		keyStore.load(new FileInputStream(keyStoreFile), SERVER_PASSWORD)
		[
			ROOT_ALIAS,
			INTERMEDIATE_ALIAS,
			END_ENTITY_ALIAS
		].each { String keyName ->
			final PrivateKey rootKey = keyStore.getKey(keyName, SERVER_PASSWORD)
			final X509Certificate rootCert = keyStore.getCertificate(keyName)
			final X500PrivateCredential credential = new X500PrivateCredential(rootCert, rootKey, keyName)
			CredentialsDAO.instance.put(credential)
		}
	}

	private static X500PrivateCredential createRootCredential() {
		final KeyPair rootPair = generateRSAKeyPair()
		final X509Certificate rootCert = generateRootCert(rootPair)
		new X500PrivateCredential(rootCert, rootPair.private, ROOT_ALIAS)
	}

	private static X500PrivateCredential createIntermediateCredential(PrivateKey caKey, X509Certificate caCert) {
		final KeyPair interPair = generateRSAKeyPair()
		final X509Certificate interCert = generateIntermediateCert(interPair.public, caKey, caCert)
		new X500PrivateCredential(interCert, interPair.private, INTERMEDIATE_ALIAS)
	}

	private static X500PrivateCredential createEndEntityCredential(PrivateKey caKey, X509Certificate caCert) {
		final KeyPair endPair = generateRSAKeyPair()
		final X509Certificate endCert = generateEndEntityCert(endPair.public, caKey, caCert)
		new X500PrivateCredential(endCert, endPair.private, END_ENTITY_ALIAS)
	}

	private static X509Certificate generateRootCert(KeyPair pair) {
		final X509V1CertificateGenerator  certGen = new X509V1CertificateGenerator()
		certGen.serialNumber = BigInteger.valueOf(1)
		certGen.setIssuerDN(new X500Principal('CN=Orayen CA Certificate'))
		certGen.notBefore = new Date(System.currentTimeMillis())
		certGen.notAfter = new Date(System.currentTimeMillis() + VALIDITY_PERIOD)
		certGen.setSubjectDN(new X500Principal('CN=Orayen CA Certificate'))
		certGen.publicKey = pair.public
		certGen.signatureAlgorithm = 'SHA1WithRSAEncryption'
		certGen.generateX509Certificate(pair.private)
	}

	private static X509Certificate generateIntermediateCert(PublicKey intKey, PrivateKey caKey, X509Certificate caCert) {
		final X509V3CertificateGenerator  certGen = new X509V3CertificateGenerator()

		certGen.serialNumber = BigInteger.valueOf(1)
		certGen.setIssuerDN(caCert.subjectX500Principal)
		certGen.notBefore = new Date(System.currentTimeMillis())
		certGen.notAfter = new Date(System.currentTimeMillis() + VALIDITY_PERIOD)
		certGen.setSubjectDN(new X500Principal('CN=Orayen Intermediate Certificate'))
		certGen.publicKey = intKey
		certGen.signatureAlgorithm = 'SHA1WithRSAEncryption'

		certGen.addExtension(X509Extensions.AuthorityKeyIdentifier, false, new AuthorityKeyIdentifierStructure(caCert))
		certGen.addExtension(X509Extensions.SubjectKeyIdentifier, false, new SubjectKeyIdentifierStructure(intKey))
		certGen.addExtension(X509Extensions.BasicConstraints, true, new BasicConstraints(0))
		certGen.addExtension(X509Extensions.KeyUsage, true, new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyCertSign | KeyUsage.cRLSign))

		certGen.generateX509Certificate(caKey)
	}

	private static X509Certificate generateEndEntityCert(PublicKey entityKey, PrivateKey caKey, X509Certificate caCert) {
		final X509V3CertificateGenerator  certGen = new X509V3CertificateGenerator()

		certGen.serialNumber = BigInteger.valueOf(1)
		certGen.setIssuerDN(caCert.subjectX500Principal)
		certGen.notBefore = new Date(System.currentTimeMillis())
		certGen.notAfter = new Date(System.currentTimeMillis() + VALIDITY_PERIOD)
		certGen.setSubjectDN(new X500Principal('CN=Orayen End Certificate'))
		certGen.publicKey = entityKey
		certGen.signatureAlgorithm = 'SHA1WithRSAEncryption'

		certGen.addExtension(X509Extensions.AuthorityKeyIdentifier, false, new AuthorityKeyIdentifierStructure(caCert))
		certGen.addExtension(X509Extensions.SubjectKeyIdentifier, false, new SubjectKeyIdentifierStructure(entityKey))
		certGen.addExtension(X509Extensions.BasicConstraints, true, new BasicConstraints(false))
		certGen.addExtension(X509Extensions.KeyUsage, true, new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment))

		certGen.generateX509Certificate(caKey)
	}

	private static KeyPair generateRSAKeyPair() {
		final KeyPairGenerator  kpGen = KeyPairGenerator.getInstance('RSA')
		kpGen.initialize(1024, new SecureRandom())
		kpGen.generateKeyPair()
	}
}
