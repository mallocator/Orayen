package net.pyxzl.orayen.restcomponents

import groovy.util.logging.Slf4j
import net.pyxzl.orayen.dto.ConfigDTO

import org.elasticsearch.common.joda.time.DateTime
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
	private ConfigDTO	config

	@Override
	protected void doInit() throws ResourceException {
		final String id = (String) request.attributes.get('clientid')
		final String version = (String) request.attributes.get('version')
		this.config = new ConfigDTO(id, DateTime.now(), '{"config":"example"}')
	}

	/**
	 * Fetches the {@link ConfigDTO} with the given id;
	 * @return
	 */
	@Get
	ConfigDTO getConfig() {
		this.config
	}

	@Put
	storeConfig(final ConfigDTO config) {
	}

	@Delete
	deleteConfig(final ConfigDTO config) {
	}
}
