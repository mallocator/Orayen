package net.pyxzl.orayen.restcomponents

import groovy.util.logging.Slf4j
import net.pyxzl.orayen.dao.ConfigDAO
import net.pyxzl.orayen.dto.ConfigDTO

import org.restlet.resource.Delete
import org.restlet.resource.Get
import org.restlet.resource.Put
import org.restlet.resource.ResourceException
import org.restlet.resource.ServerResource

/**
 * @author Ravi Gairola (mallox@pyxzl.net)
 */
@Slf4j
class ConfigResource extends ServerResource {
	private String id
	private String version

	@Override
	protected void doInit() throws ResourceException {
		this.id = (String) request.attributes.get('configType')
		this.version = (String) request.attributes.get('version')
	}

	/**
	 * Fetches the {@link ConfigDTO} with the given id;
	 * @return
	 */
	@Get
	ConfigDTO getConfig() {
		ConfigDAO.instance.get(this.id, this.version)
	}

	@Put
	storeConfig(final ConfigDTO config) {
		ConfigDAO.instance.put(config)
	}

	@Delete
	deleteConfig() {
		ConfigDAO.instance.delete(id, version)
	}
}
