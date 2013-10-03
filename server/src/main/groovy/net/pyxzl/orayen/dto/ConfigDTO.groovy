package net.pyxzl.orayen.dto

import net.pyxzl.orayen.service.ContentPayloadConverter

import org.codehaus.jackson.annotate.JsonProperty
import org.codehaus.jackson.map.annotate.JsonDeserialize
import org.codehaus.jackson.map.annotate.JsonSerialize


/**
 * @author Ravi Gairola (mallox@pyxzl.net)
 */
class ConfigDTO extends DTO {
	String label
	String version
	String payload

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
