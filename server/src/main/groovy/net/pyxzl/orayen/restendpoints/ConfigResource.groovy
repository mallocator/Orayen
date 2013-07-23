package net.pyxzl.orayen.restendpoints;

import net.pyxzl.orayen.dto.ConfigDTO

import org.elasticsearch.common.joda.time.DateTime
import org.restlet.resource.Delete
import org.restlet.resource.Get
import org.restlet.resource.Put
import org.restlet.resource.ResourceException
import org.restlet.resource.ServerResource

public class ConfigResource extends ServerResource {

	private ConfigDTO	config;

	@Override
	protected void doInit() throws ResourceException {
		final String id = (String) getRequest().getAttributes().get("id");
		this.config = new ConfigDTO(id, DateTime.now(), "{\"config\":\"example\"}");
		super.doInit();
	}

	@Get
	ConfigDTO getConfig() {
		return this.config;
	}

	@Put
	storeConfig(final ConfigDTO config) {
	}

	@Delete
	deleteConfig(final ConfigDTO config) {
	}
}
