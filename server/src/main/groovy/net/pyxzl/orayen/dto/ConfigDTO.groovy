package net.pyxzl.orayen.dto

import net.pyxzl.orayen.service.ContentPayloadConverter

import org.codehaus.jackson.annotate.JsonProperty
import org.codehaus.jackson.map.annotate.JsonDeserialize
import org.codehaus.jackson.map.annotate.JsonSerialize
import org.elasticsearch.common.joda.time.DateTime


/**
 * @author Ravi Gairola (mallox@pyxzl.net)
 */
class ConfigDTO {
	String	id
	DateTime	lastUpdate
	String version
	String	payload

	@JsonProperty
	Long getLastUpdate() {
		this.lastUpdate.millis
	}

	@JsonProperty
	void setLastUpdate(long lastUpdate) {
		this.lastUpdate = new DateTime(lastUpdate)
	}

	@JsonSerialize(using = ContentPayloadConverter.Serializer.class)
	@JsonProperty
	String getPayload() {
		this.payload
	}

	@JsonDeserialize(using = ContentPayloadConverter.Deserializer.class)
	@JsonProperty
	void setPayload(final String payload) {
		this.payload = payload
	}
}
