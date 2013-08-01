package net.pyxzl.orayen.restcomponents

import org.restlet.resource.Get
import org.restlet.resource.Post
import org.restlet.resource.Put
import org.restlet.resource.ResourceException
import org.restlet.resource.ServerResource

public class KeystoreResource extends ServerResource {
	private String format = "jks"
	private String clientId

	@Override
	protected void doInit() throws ResourceException {
		this.format = (String) getRequest().getAttributes().get("format")
		this.clientId = (String) getRequest().getAttributes().get("clientid")
	}

	/**
	 * Returns the (public) certificate that can be used by a client to register itself.
	 * @return
	 */
	@Get
	String getCertificate() {
		return null
	}


	/**
	 * Returns the stored (public) certificate for a client.
	 * @return
	 */
	@Post
	String getClientCertificate() {
		return null
	}

	@Put
	boolean createKeystore() {
		return true
	}
}
