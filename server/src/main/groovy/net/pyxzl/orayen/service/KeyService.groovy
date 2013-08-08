package net.pyxzl.orayen.service

import groovy.util.logging.Slf4j

import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
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
import org.elasticsearch.groovy.client.GClient

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
	private static final String ES_TYPE = 'ssl'
	private static final int VALIDITY_PERIOD = 10 * 365 * 24 * 60 * 60 * 1000 // ten years
	private final X500PrivateCredential rootCredential
	private final X500PrivateCredential interCredential
	private final X500PrivateCredential endCredential

	private KeyService() {
		final GClient client = EsService.instance.client

		def root = client.get {
			index Setting.ES_INDEX.value
			type ES_TYPE
			id ROOT_ALIAS
		}

		def inter = client.get {
			index Setting.ES_INDEX.value
			type ES_TYPE
			id INTERMEDIATE_ALIAS
		}

		def end = client.get {
			index Setting.ES_INDEX.value
			type ES_TYPE
			id END_ENTITY_ALIAS
		}

		if (root.response.exists && inter.response.exists && end.response.exists) {
			this.rootCredential = this.deserializeCred(root.response.source)
			this.interCredential = this.deserializeCred(inter.response.source)
			this.endCredential = this.deserializeCred(end.response.source)
			return
		}

		this.rootCredential = storeCred(ROOT_ALIAS, createRootCredential())
		this.interCredential = storeCred(INTERMEDIATE_ALIAS, createIntermediateCredential(rootCredential.privateKey, rootCredential.certificate))
		this.endCredential = storeCred(END_ENTITY_ALIAS, createEndEntityCredential(interCredential.privateKey, interCredential.certificate))
	}

	private X500PrivateCredential deserializeCred(def source) {
		new X500PrivateCredential(bytea2obj(source.cert), bytea2obj(source.key), source.alias)
	}

	private X500PrivateCredential storeCred(String credId, X500PrivateCredential cred) {
		EsService.instance.client.index {
			index Setting.ES_INDEX.value
			type ES_TYPE
			id credId
			source {
				alias = cred.alias
				cert = obj2bytea(cred.cert)
				key = obj2bytea(cred.key)
			}
		}
		cred
	}

	private byte[] obj2bytea(Object o) {
		final ByteArrayOutputStream bos = new ByteArrayOutputStream()
		ObjectOutput output = null
		try {
			output = new ObjectOutputStream(bos)
			output.writeObject(o)
			return bos.toByteArray()
		} finally {
			output.close()
			bos.close()
		}
	}

	private Object bytea2obj(byte[] ba) {
		final ByteArrayInputStream bis = new ByteArrayInputStream(ba)
		ObjectInput input = null
		try {
			input = new ObjectInputStream(bis)
			return input.readObject()
		} finally {
			bis.close()
			input.close()
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

	static X509Certificate generateIntermediateCert(PublicKey intKey, PrivateKey caKey, X509Certificate caCert) {
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

	static KeyPair generateRSAKeyPair() {
		final KeyPairGenerator  kpGen = KeyPairGenerator.getInstance('RSA')
		kpGen.initialize(1024, new SecureRandom())
		kpGen.generateKeyPair()
	}
}
