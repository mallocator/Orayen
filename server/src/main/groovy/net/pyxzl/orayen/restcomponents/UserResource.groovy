/**
 *
 */
package net.pyxzl.orayen.restcomponents

import net.pyxzl.orayen.dao.UserDAO
import net.pyxzl.orayen.dto.UserDTO

import org.restlet.resource.Delete
import org.restlet.resource.Get
import org.restlet.resource.Put
import org.restlet.resource.ResourceException
import org.restlet.resource.ServerResource

/**
 * @author Ravi Gairola (mallox@pyxzl.net)
 */
class UserResource extends ServerResource {
	private String userId

	@Override
	protected void doInit() throws ResourceException {
		this.userId = (String) request.attributes.get('userId')
	}

	@Get
	public UserDTO getUser() {
		UserDAO.instance.get(this.userId)
	}

	@Put
	public void storeUser(UserDTO user) {
		UserDAO.instance.put(user)
	}

	@Delete
	public void deleteUser() {
		UserDAO.instance.delete(this.userId)
	}
}
