package net.pyxzl.orayen.dto;

import org.elasticsearch.common.joda.time.DateTime

public class ConfigDTO {
	final String	id;
	final DateTime	lastUpdate;
	final String	config;

	public ConfigDTO(String id, DateTime lastUpdate, String config) {
		this.id = id;
		this.lastUpdate = lastUpdate;
		this.config = config;
	}
}
