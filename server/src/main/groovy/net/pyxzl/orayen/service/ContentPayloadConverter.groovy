package net.pyxzl.orayen.service

import org.codehaus.jackson.JsonGenerator
import org.codehaus.jackson.JsonParser
import org.codehaus.jackson.JsonProcessingException
import org.codehaus.jackson.JsonToken
import org.codehaus.jackson.map.DeserializationContext
import org.codehaus.jackson.map.JsonDeserializer
import org.codehaus.jackson.map.JsonSerializer
import org.codehaus.jackson.map.SerializerProvider

/**
 * Can be used to (de-) serialize a property and all sub-elements of this property into a string and back.
 *
 * @author Ravi Gairola (mallox@pyxzl.net)
 */
class ContentPayloadConverter {
	/**
	 * Serializes the property, with valid JSON into the existing object and keeps all structure of the sub-object in tact.
	 *
	 * @author Ravi.Gairola
	 */
	static class Serializer extends JsonSerializer<Object> {
		@Override
		void serialize(final Object value, final JsonGenerator jgen, final SerializerProvider provider) throws IOException,
		JsonProcessingException {
			jgen.writeRaw(':' + String.valueOf(value))
		}
	}

	/**
	 * Deserializes everything that is stored under this property to a string, which will not be further interpreted by
	 * Jackson.
	 *
	 * @author Ravi.Gairola
	 */
	static class Deserializer extends JsonDeserializer<Object> {
		@Override
		Object deserialize(final JsonParser jp, final DeserializationContext ctxt) throws IOException, JsonProcessingException {
			final StringBuilder result = new StringBuilder()
			int objectCounter = 0
			int arrayCounter = 0
			JsonToken currentToken = jp.currentToken
			while (true) {
				switch (currentToken) {
					case JsonToken.START_OBJECT:
						result.append(jp.text)
						objectCounter++
						break
					case JsonToken.START_ARRAY:
						result.append(jp.text)
						arrayCounter++
						break
					case JsonToken.FIELD_NAME:
						result.append('"').append(jp.text).append('":')
						break
					case JsonToken.VALUE_STRING:
						result.append('"').append(jp.text.replace('\\', '\\\\').replace('"', '\\"')).append('",')
						break
					case JsonToken.END_ARRAY:
						if (result.charAt(result.length() - 1) == ',') {
							result.deleteCharAt(result.length() - 1)
						}
						result.append(jp.text).append(',')
						arrayCounter--
						break
					case JsonToken.END_OBJECT:
						if (result.charAt(result.length() - 1) == ',') {
							result.deleteCharAt(result.length() - 1)
						}
						result.append(jp.text).append(',')
						objectCounter--
						break
					default:
						result.append(jp.text).append(',')
				}

				if (objectCounter == 0 && arrayCounter == 0) {
					if (result.charAt(result.length() - 1) == ',') {
						result.deleteCharAt(result.length() - 1)
					}
					break
				}
				currentToken = jp.nextToken()
			}
			result.toString()
		}
	}
}