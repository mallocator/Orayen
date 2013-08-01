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

import org.bouncycastle.asn1.x509.BasicConstraints
import org.bouncycastle.asn1.x509.KeyUsage
import org.bouncycastle.asn1.x509.X509Extensions
import org.bouncycastle.x509.X509V1CertificateGenerator
import org.bouncycastle.x509.X509V3CertificateGenerator
import org.bouncycastle.x509.extension.AuthorityKeyIdentifierStructure
import org.bouncycastle.x509.extension.SubjectKeyIdentifierStructure

@Slf4j
@Singleton
@SuppressWarnings("deprecation")
class CertGenerator {
	static char[] SERVER_PASSWORD = "OrayenServer".toCharArray()
	static char[] CLIENT_PASSWORD = "OrayenClient".toCharArray()

	public static String ROOT_ALIAS = "root"
	public static String INTERMEDIATE_ALIAS = "intermediate"
	public static String END_ENTITY_ALIAS = "end"

	private static final int VALIDITY_PERIOD = 10 * 365 * 24 * 60 * 60 * 1000 // ten years

	/**
	 * Create the key and trust stores if the don't already exist.
	 * @param clientName
	 */
	void createKeyStores() {
		final File trustStoreFile = new File(Setting.TRUSTSTORE.value)
		final File keyStoreFile = new File(Setting.KEYSTORE.value)

		if (trustStoreFile.exists() && keyStoreFile.exists()) {
			log.trace "Skipping creating key and trust store, as both already exist"
			return
		}

		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider())

		final X500PrivateCredential    rootCredential = createRootCredential()
		final X500PrivateCredential    interCredential = createIntermediateCredential(rootCredential.getPrivateKey(), rootCredential.getCertificate())
		final X500PrivateCredential    endCredential = createEndEntityCredential(interCredential.getPrivateKey(), interCredential.getCertificate())


		trustStoreFile.parentFile.mkdirs()
		final KeyStore trustStore = KeyStore.getInstance("JKS")
		trustStore.load(null, null)
		trustStore.setCertificateEntry(ROOT_ALIAS, rootCredential.getCertificate())
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
		keyStore.setKeyEntry(ROOT_ALIAS, rootCredential.getPrivateKey(), SERVER_PASSWORD, serverChain)
		keyStore.setKeyEntry(INTERMEDIATE_ALIAS, interCredential.getPrivateKey(), SERVER_PASSWORD, interChain)
		keyStore.setKeyEntry(END_ENTITY_ALIAS,endCredential.getPrivateKey(),SERVER_PASSWORD,clientChain)
		keyStore.store(new FileOutputStream(keyStoreFile), SERVER_PASSWORD)
		log.info "Created new trust store at ${Setting.KEYSTORE}"
	}

	/**
	 * Generate a new key for a new client that can be used to establish a https connection
	 * @param clientName
	 */
	void createClientKey(String clientName) {
		final File clientStoreFile = new File(Setting.CERTSTORE.value + clientName + ".p12")
		if (!clientStoreFile.exists()) {
			clientStoreFile.parentFile.mkdirs()
			final KeyStore clientKeyStore = KeyStore.getInstance("JKS")
			clientKeyStore.load(new FileInputStream(Setting.KEYSTORE.value), SERVER_PASSWORD)
			final Certificate[] clientCerts = clientKeyStore.getCertificateChain(END_ENTITY_ALIAS)
			final Key privateKey = clientKeyStore.getKey(END_ENTITY_ALIAS, SERVER_PASSWORD)
			final KeyStore clientStore = KeyStore.getInstance("PKCS12")
			clientStore.load(null, null)
			clientStore.setKeyEntry(clientName,privateKey,CLIENT_PASSWORD,clientCerts)
			clientStore.store(new FileOutputStream(clientStoreFile), CLIENT_PASSWORD)
			log.info "Created new client key file at ${Setting.CERTSTORE.value}${clientName}.p12"
		} else {
			log.trace "Skipped creating client key for ${clientName} as it already existed"
		}
	}

	private static X500PrivateCredential createRootCredential() {
		final KeyPair rootPair = generateRSAKeyPair()
		final X509Certificate rootCert = generateRootCert(rootPair)
		return new X500PrivateCredential(rootCert, rootPair.getPrivate(), ROOT_ALIAS)
	}

	private static X500PrivateCredential createIntermediateCredential(PrivateKey caKey, X509Certificate caCert) {
		final KeyPair interPair = generateRSAKeyPair()
		final X509Certificate interCert = generateIntermediateCert(interPair.getPublic(), caKey, caCert)
		return new X500PrivateCredential(interCert, interPair.getPrivate(), INTERMEDIATE_ALIAS)
	}

	private static X500PrivateCredential createEndEntityCredential(PrivateKey caKey, X509Certificate caCert) {
		final KeyPair endPair = generateRSAKeyPair()
		final X509Certificate endCert = generateEndEntityCert(endPair.getPublic(), caKey, caCert)
		return new X500PrivateCredential(endCert, endPair.getPrivate(), END_ENTITY_ALIAS)
	}

	private static X509Certificate generateRootCert(KeyPair pair) {
		final X509V1CertificateGenerator  certGen = new X509V1CertificateGenerator()
		certGen.serialNumber = BigInteger.valueOf(1)
		certGen.setIssuerDN(new X500Principal("CN=Orayen CA Certificate"))
		certGen.notBefore = new Date(System.currentTimeMillis())
		certGen.notAfter = new Date(System.currentTimeMillis() + VALIDITY_PERIOD)
		certGen.setSubjectDN(new X500Principal("CN=Orayen CA Certificate"))
		certGen.publicKey = pair.getPublic()
		certGen.signatureAlgorithm = "SHA1WithRSAEncryption"
		return certGen.generateX509Certificate(pair.getPrivate())
	}

	public static X509Certificate generateIntermediateCert(PublicKey intKey, PrivateKey caKey, X509Certificate caCert) {
		final X509V3CertificateGenerator  certGen = new X509V3CertificateGenerator()

		certGen.serialNumber = BigInteger.valueOf(1)
		certGen.issuerDN = caCert.getSubjectX500Principal()
		certGen.notBefore = new Date(System.currentTimeMillis())
		certGen.notAfter = new Date(System.currentTimeMillis() + VALIDITY_PERIOD)
		certGen.setSubjectDN(new X500Principal("CN=Orayen Intermediate Certificate"))
		certGen.publicKey = intKey
		certGen.signatureAlgorithm = "SHA1WithRSAEncryption"

		certGen.addExtension(X509Extensions.AuthorityKeyIdentifier, false, new AuthorityKeyIdentifierStructure(caCert))
		certGen.addExtension(X509Extensions.SubjectKeyIdentifier, false, new SubjectKeyIdentifierStructure(intKey))
		certGen.addExtension(X509Extensions.BasicConstraints, true, new BasicConstraints(0))
		certGen.addExtension(X509Extensions.KeyUsage, true, new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyCertSign | KeyUsage.cRLSign))

		return certGen.generateX509Certificate(caKey)
	}

	private static X509Certificate generateEndEntityCert(PublicKey entityKey, PrivateKey caKey, X509Certificate caCert) {
		X509V3CertificateGenerator  certGen = new X509V3CertificateGenerator()

		certGen.serialNumber = BigInteger.valueOf(1)
		certGen.issuerDN = caCert.getSubjectX500Principal()
		certGen.notBefore = new Date(System.currentTimeMillis())
		certGen.notAfter = new Date(System.currentTimeMillis() + VALIDITY_PERIOD)
		certGen.setSubjectDN(new X500Principal("CN=Orayen End Certificate"))
		certGen.publicKey = entityKey
		certGen.signatureAlgorithm = "SHA1WithRSAEncryption"

		certGen.addExtension(X509Extensions.AuthorityKeyIdentifier, false, new AuthorityKeyIdentifierStructure(caCert))
		certGen.addExtension(X509Extensions.SubjectKeyIdentifier, false, new SubjectKeyIdentifierStructure(entityKey))
		certGen.addExtension(X509Extensions.BasicConstraints, true, new BasicConstraints(false))
		certGen.addExtension(X509Extensions.KeyUsage, true, new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment))

		return certGen.generateX509Certificate(caKey)
	}

	public static KeyPair generateRSAKeyPair() {
		final KeyPairGenerator  kpGen = KeyPairGenerator.getInstance("RSA")
		kpGen.initialize(1024,new SecureRandom())
		return kpGen.generateKeyPair()
	}
}
