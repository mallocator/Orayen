package net.pyxzl.orayen.service

import groovy.util.logging.Slf4j

import javax.security.auth.x500.X500PrivateCredential

import mockit.Mock
import mockit.MockUp
import net.pyxzl.orayen.dao.CredentialsDAO

import org.testng.Assert
import org.testng.annotations.AfterClass
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test

@Slf4j
class KeyServiceTest {
	MockUp credentialsDAO
	CredentialsDAO credDao

	@BeforeClass
	void setUpMocks() {
		this.credentialsDAO = new MockUp<CredentialsDAO>() {
					@Mock
					X500PrivateCredential get(String alias) {
						null
					}

					@Mock
					X500PrivateCredential put(X500PrivateCredential cred) {
						cred
					}
				}
	}

	@AfterClass
	void tearDownMocks() {
		this.credentialsDAO.tearDown()
	}

	@Test
	void testGetRootCertificateCRT() {
		final String crt = new String(KeyService.instance.rootCertificateCRT)
		Assert.assertTrue(crt.startsWith('-----BEGIN CERTIFICATE-----'), 'Resulting Certificate does not contain block delimiter start')
		Assert.assertTrue(crt.trim().endsWith('-----END CERTIFICATE-----'), 'Resulting Certificate does not contain block delimiter end')
		Assert.assertTrue(crt.length() > 54, 'Resulting containter does not contain data block')
	}

	@Test
	void testGetClientKeyAndChainPEM() {
		final String keyAndChain = new String(KeyService.instance.clientKeyAndChainPEM)
		Assert.assertTrue(keyAndChain.startsWith('-----BEGIN RSA PRIVATE KEY-----'), 'Resulting Certificate does not contain block delimiter start')
		Assert.assertTrue(keyAndChain.trim().endsWith('-----END CERTIFICATE-----'), 'Resulting Certificate does not contain block delimiter end')
		Assert.assertTrue(keyAndChain.length() > 54, 'Resulting containter does not contain data block')
		int certCount = 0
		keyAndChain.eachMatch('-----END CERTIFICATE-----') { certCount++ }
		Assert.assertEquals(certCount, 3)
		int emptyBlocks = 0
		keyAndChain.split("-----BEGIN ").each { String s -> if (s.length() > 0 && s.length() < 60) emptyBlocks++ }
		Assert.assertEquals(emptyBlocks, 0)
	}
}
