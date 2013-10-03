package net.pyxzl.orayen.restcomponents

import net.pyxzl.orayen.dao.ClientDAO
import net.pyxzl.orayen.dto.ClientDTO

import org.restlet.resource.Delete
import org.restlet.resource.Get
import org.restlet.resource.Put
import org.restlet.resource.ResourceException
import org.restlet.resource.ServerResource

/**
 * @author Ravi Gairola (mallox@pyxzl.net)
 */
class ClientResource extends ServerResource {
	private String id

	@Override
	protected void doInit() throws ResourceException {
		this.id = (String) request.attributes.get('clientid')
	}

	@Get
	public ClientDTO getClient() {
		ClientDAO.instance.get(this.id)
	}

	@Put
	public void storeClient(final ClientDTO client) {
		ClientDAO.instance.put(client)
	}

	@Delete
	public void deleteClient() {
		ClientDAO.instance.delete(this.id)
	}
}
