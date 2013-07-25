package net.pyxzl.orayen.dto;

import net.pyxzl.orayen.service.ContentPayloadConverter

import org.codehaus.jackson.annotate.JsonProperty
import org.codehaus.jackson.map.annotate.JsonSerialize
import org.elasticsearch.common.joda.time.DateTime


public class ConfigDTO {
	final String	id
	final DateTime	lastUpdate
	final String version
	String	config

	public ConfigDTO(String id, DateTime lastUpdate, String config) {
		this.id = id;
		this.lastUpdate = lastUpdate;
		this.config = config;
		this.version = "1.0.0"
	}

	@JsonProperty
	Long getLastUpdate() {
		return this.lastUpdate.millis;
	}

	@JsonSerialize(using = ContentPayloadConverter.Serializer.class)
	@JsonProperty
	String getConfig() {
		return this.config;
	}
}
