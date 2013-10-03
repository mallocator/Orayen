package net.pyxzl.orayen.dto

import org.codehaus.jackson.annotate.JsonProperty
import org.elasticsearch.common.joda.time.DateTime

abstract class DTO {
	String id
	protected DateTime lastUpdate = DateTime.now()
	protected DateTime created = DateTime.now()

	@JsonProperty
	Long getLastUpdate() {
		this.lastUpdate.millis
	}

	@JsonProperty
	void setLastUpdate(long lastUpdate) {
		this.lastUpdate = new DateTime(lastUpdate)
	}

	@JsonProperty
	Long getCreated() {
		this.created.millis
	}

	@JsonProperty
	void setCreated(long created) {
		this.created = new DateTime(created)
	}
}
