package net.pyxzl.orayen.restcomponents

import org.restlet.resource.Get
import org.restlet.resource.Post
import org.restlet.resource.Put
import org.restlet.resource.ResourceException
import org.restlet.resource.ServerResource

/**
 * @author Ravi Gairola (mallox@pyxzl.net)
 */
class KeystoreResource extends ServerResource {
	private String format = 'jks'
	private String clientId

	@Override
	protected void doInit() throws ResourceException {
		this.format = (String) request.attributes.get('format')
		this.clientId = (String) request.attributes.get('clientid')
	}

	/**
	 * Returns the (public) certificate that can be used by a client to register itself.
	 * @return
	 */
	@Get
	String getCertificate() {
		null
	}


	/**
	 * Returns the stored (public) certificate for a client.
	 * @return
	 */
	@Post
	String getClientCertificate() {
		null
	}

	@Put
	boolean createKeystore() {
		true
	}
}
