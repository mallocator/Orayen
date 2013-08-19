package net.pyxzl.orayen.service

import groovy.util.logging.Slf4j

import java.security.Key
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.Security
import java.security.cert.Certificate
import java.security.cert.X509Certificate

import javax.security.auth.x500.X500Principal
import javax.security.auth.x500.X500PrivateCredential

import net.pyxzl.orayen.Config.Setting
import net.pyxzl.orayen.dao.CredentialsDAO

import org.bouncycastle.asn1.x509.BasicConstraints
import org.bouncycastle.asn1.x509.KeyUsage
import org.bouncycastle.asn1.x509.X509Extensions
import org.bouncycastle.util.io.pem.PemWriter
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
	private static final String ROOT_ALIAS = 'root'
	private static final String INTERMEDIATE_ALIAS = 'intermediate'
	private static final String END_ENTITY_ALIAS = 'end'
	private static final String CLIENT_ALIAS = 'client'
	private static final int KEY_SIZE = 1024
	private static final String KEY_ALG = 'RSA'
	private static final String SIG_ALG = 'SHA1WithRSAEncryption'
	private static final char[] SERVER_PASSWORD = Setting.CERTPASS.value as char[]
	private static final String ES_TYPE = 'ssl'
	private static final Date NOW = new Date()
	private static final Date UNTIL = new Date(NOW.year + 10, NOW.month, NOW.day)
	private final X500PrivateCredential rootCredential
	private final X500PrivateCredential interCredential
	private final X500PrivateCredential endCredential
	private final boolean keyExist = false

	private KeyService() {
		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider())

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

	/**
	 * Returns the root certificate that is used by the client to authenticate the host.
	 * @return JKS format, byte encoded trust store
	 */
	byte[] getRootCertificateJKS() {
		final KeyStore certStore = KeyStore.getInstance('JKS')
		certStore.load(null, null)
		certStore.setCertificateEntry(ROOT_ALIAS, this.rootCredential.certificate)
		final ByteArrayOutputStream baos = new ByteArrayOutputStream()
		certStore.store(baos, SERVER_PASSWORD)
		baos.toByteArray()
	}

	/**
	 * Returns the root certificate that is used by the client to authenticate the host.
	 * @return CRT/PEM format, byte encoded certificate
	 */
	byte[] getRootCertificateCRT() {
		final StringWriter writer = new StringWriter()
		final PemWriter pemWriter = new PemWriter(writer)
		pemWriter.writePreEncapsulationBoundary("CERTIFICATE")
		pemWriter.writeEncoded this.rootCredential.cert.encoded
		pemWriter.writePostEncapsulationBoundary("CERTIFICATE")
		pemWriter.close()
		writer.toString().bytes
	}

	/**
	 * Returns all root keys and their certificate chains. This is only returned as JKS as this should not be shared outside of the Java server.
	 * @return JKS encoded Root Keys and Certificate Chains
	 */
	byte[] getRootKeysJKS() {
		final Certificate[] serverChain = new Certificate[1]
		serverChain[0] = rootCredential.certificate

		final Certificate[] interChain = new Certificate[2]
		interChain[0] = interCredential.certificate
		interChain[1] = rootCredential.certificate

		final Certificate[] clientChain = new Certificate[3]
		clientChain[0] = endCredential.certificate
		clientChain[1] = interCredential.certificate
		clientChain[2] = rootCredential.certificate

		final KeyStore keyStore = KeyStore.getInstance('JKS')
		keyStore.load(null, null)
		keyStore.setKeyEntry(ROOT_ALIAS, rootCredential.privateKey, SERVER_PASSWORD, serverChain)
		keyStore.setKeyEntry(INTERMEDIATE_ALIAS, interCredential.privateKey, SERVER_PASSWORD, interChain)
		keyStore.setKeyEntry(END_ENTITY_ALIAS, endCredential.privateKey, SERVER_PASSWORD, clientChain)
		final ByteArrayOutputStream baos = new ByteArrayOutputStream()
		keyStore.store(baos, SERVER_PASSWORD)
		baos.toByteArray()
	}

	/**
	 * Returns the client private key and server certificate that signed the key for the client to authenticate against the server.
	 * @return Client Key and Server Certificate in P12 format
	 */
	byte[] getClientKeyAndChainP12() {
		final Certificate[] clientCerts = [
			this.endCredential.cert,
			this.interCredential.cert,
			this.rootCredential.cert
		]
		final Key privateKey = endCredential.key
		final KeyStore keyStore = KeyStore.getInstance('PKCS12')
		keyStore.load(null, null)
		keyStore.setKeyEntry(CLIENT_ALIAS, privateKey, SERVER_PASSWORD, clientCerts)
		final ByteArrayOutputStream baos = new ByteArrayOutputStream()
		keyStore.store(baos, SERVER_PASSWORD)
		baos.toByteArray()
	}

	/**
	 * Returns the client private key and server certificate that signed the key for the client to authenticate against the server.
	 * @return Client Key and Server Certificate in PEM format
	 */
	byte[] getClientKeyAndChainPEM() {
		final StringWriter writer = new StringWriter()
		final PemWriter pemWriter = new PemWriter(writer)
		pemWriter.writePreEncapsulationBoundary(KEY_ALG + ' PRIVATE KEY')
		pemWriter.writeEncoded this.endCredential.key.encoded
		pemWriter.writePostEncapsulationBoundary(KEY_ALG + ' PRIVATE KEY')
		pemWriter.writePreEncapsulationBoundary("CERTIFICATE")
		pemWriter.writeEncoded this.endCredential.cert.encoded
		pemWriter.writePostEncapsulationBoundary("CERTIFICATE")
		pemWriter.writePreEncapsulationBoundary("CERTIFICATE")
		pemWriter.writeEncoded this.interCredential.cert.encoded
		pemWriter.writePostEncapsulationBoundary("CERTIFICATE")
		pemWriter.writePreEncapsulationBoundary("CERTIFICATE")
		pemWriter.writeEncoded this.rootCredential.cert.encoded
		pemWriter.writePostEncapsulationBoundary("CERTIFICATE")
		pemWriter.close()
		writer.toString().bytes
	}

	/**
	 * Returns the client private key that can be used to authenticate against the server.
	 * @return Client Key in CRT/PEM format.
	 */
	byte[] getClientKeyPEM() {
		final StringWriter writer = new StringWriter()
		final PemWriter pemWriter = new PemWriter(writer)
		pemWriter.writePreEncapsulationBoundary(KEY_ALG + ' PRIVATE KEY')
		pemWriter.writeEncoded this.endCredential.key.encoded
		pemWriter.writePostEncapsulationBoundary(KEY_ALG + ' PRIVATE KEY')
		pemWriter.close()
		writer.toString().bytes
	}

	/**
	 * Returns the certificate chain that was used to sign the client private key, used to authenticate against the server.
	 * @return Certificate Chain in CRT/PEM format
	 */
	byte[] getClientCertificateChainPEM() {
		final StringWriter writer = new StringWriter()
		final PemWriter pemWriter = new PemWriter(writer)
		pemWriter.writePreEncapsulationBoundary("CERTIFICATE")
		pemWriter.writeEncoded this.endCredential.cert.encoded
		pemWriter.writePostEncapsulationBoundary("CERTIFICATE")
		pemWriter.writePreEncapsulationBoundary("CERTIFICATE")
		pemWriter.writeEncoded this.interCredential.cert.encoded
		pemWriter.writePostEncapsulationBoundary("CERTIFICATE")
		pemWriter.writePreEncapsulationBoundary("CERTIFICATE")
		pemWriter.writeEncoded this.rootCredential.cert.encoded
		pemWriter.writePostEncapsulationBoundary("CERTIFICATE")
		pemWriter.close()
		writer.toString().bytes
	}

	private static X500PrivateCredential createRootCredential() {
		final KeyPair rootPair = generateKeyPair()
		final X509Certificate rootCert = generateRootCert(rootPair)
		new X500PrivateCredential(rootCert, rootPair.private, ROOT_ALIAS)
	}

	private static X500PrivateCredential createIntermediateCredential(PrivateKey caKey, X509Certificate caCert) {
		final KeyPair interPair = generateKeyPair()
		final X509Certificate interCert = generateIntermediateCert(interPair.public, caKey, caCert)
		new X500PrivateCredential(interCert, interPair.private, INTERMEDIATE_ALIAS)
	}

	private static X500PrivateCredential createEndEntityCredential(PrivateKey caKey, X509Certificate caCert) {
		final KeyPair endPair = generateKeyPair()
		final X509Certificate endCert = generateEndEntityCert(endPair.public, caKey, caCert)
		new X500PrivateCredential(endCert, endPair.private, END_ENTITY_ALIAS)
	}

	private static X509Certificate generateRootCert(KeyPair pair) {
		final X509V1CertificateGenerator  certGen = new X509V1CertificateGenerator()
		certGen.serialNumber = BigInteger.valueOf(1)
		certGen.setIssuerDN(new X500Principal('CN=Orayen CA Certificate'))
		certGen.setSerialNumber(new BigInteger(System.currentTimeMillis()))
		certGen.notBefore = NOW
		certGen.notAfter = UNTIL
		certGen.setSubjectDN(new X500Principal('CN=Orayen CA Certificate'))
		certGen.publicKey = pair.public
		certGen.signatureAlgorithm = SIG_ALG
		certGen.generateX509Certificate(pair.private)
	}

	private static X509Certificate generateIntermediateCert(PublicKey intKey, PrivateKey caKey, X509Certificate caCert) {
		final X509V3CertificateGenerator  certGen = new X509V3CertificateGenerator()

		certGen.serialNumber = BigInteger.valueOf(1)
		certGen.setIssuerDN(caCert.subjectX500Principal)
		certGen.setSerialNumber(new BigInteger(System.currentTimeMillis()))
		certGen.notBefore = NOW
		certGen.notAfter = UNTIL
		certGen.setSubjectDN(new X500Principal('CN=Orayen Intermediate Certificate'))
		certGen.publicKey = intKey
		certGen.signatureAlgorithm = SIG_ALG

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
		certGen.setSerialNumber(new BigInteger(System.currentTimeMillis()))
		certGen.notBefore = NOW
		certGen.notAfter = UNTIL
		certGen.setSubjectDN(new X500Principal('CN=Orayen End Certificate'))
		certGen.publicKey = entityKey
		certGen.signatureAlgorithm = SIG_ALG

		certGen.addExtension(X509Extensions.AuthorityKeyIdentifier, false, new AuthorityKeyIdentifierStructure(caCert))
		certGen.addExtension(X509Extensions.SubjectKeyIdentifier, false, new SubjectKeyIdentifierStructure(entityKey))
		certGen.addExtension(X509Extensions.BasicConstraints, true, new BasicConstraints(false))
		certGen.addExtension(X509Extensions.KeyUsage, true, new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment))

		certGen.generateX509Certificate(caKey)
	}

	private static KeyPair generateKeyPair() {
		final KeyPairGenerator  kpGen = KeyPairGenerator.getInstance(KEY_ALG)
		kpGen.initialize(KEY_SIZE, new SecureRandom())
		kpGen.generateKeyPair()
	}
}
