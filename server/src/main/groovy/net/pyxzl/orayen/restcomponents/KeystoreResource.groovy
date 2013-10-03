package net.pyxzl.orayen.restcomponents

import net.pyxzl.orayen.service.KeyService

import org.restlet.resource.Get
import org.restlet.resource.ResourceException
import org.restlet.resource.ServerResource

/**
 * @author Ravi Gairola (mallox@pyxzl.net)
 */


class KeystoreResource extends ServerResource {
	private String format
	private String clientId

	private static enum CertType {
		K_C_P12, 	// Client Private Key and Certificate Chain in .p12 format
		K_PEM,		// Client Private Key in .pem format
		C_PEM,		// Client Certificate Chain in .pem format
		K_C_PEM,	// Combined Client Private Key and Certificate Chain in .pem format
		RC_JKS,		// Root Certificate in .jks format
		RC_PEM,		// Root Certificate in .pem format
		RK_JKS,		// Root Private Keys in .jks format
	}

	@Override
	protected void doInit() throws ResourceException {
		this.format = (String) request.attributes.get('format')
	}


	/**
	 * Returns the stored certificates and keys for a client.
	 * @return
	 */
	@Get
	String getClientCertificate() {
		switch (CertType.valueOf(format.toUpperCase())) {
			case CertType.C_PEM:
				return KeyService.instance.clientKeyPEM
			case CertType.K_PEM:
				return KeyService.instance.clientCertificateChainPEM
			case CertType.K_C_PEM:
				return KeyService.instance.clientKeyAndChainPEM
			case CertType.K_C_P12:
				return KeyService.instance.clientKeyAndChainP12
			case CertType.RC_JKS:
				return KeyService.instance.rootCertificateJKS
			case CertType.RC_PEM:
				return KeyService.instance.rootCertificateCRT
			case CertType.RK_JKS:
				return KeyService.instance.rootKeysJKS
		}
		null
	}
}
