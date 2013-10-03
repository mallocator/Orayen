package net.pyxzl.orayen.dao

import net.pyxzl.orayen.Config.Setting
import net.pyxzl.orayen.service.EsService

import org.codehaus.jackson.map.ObjectMapper



abstract class DAO {
	private static final ObjectMapper mapper = new ObjectMapper()

	protected abstract getEsType()

	protected <T> T parseJson(final byte[] source) {
		if (source != null) {
			return this.mapper.readValue(source, T.class)
		}
		null
	}

	protected <T> T parseJson(final String source) {
		if (source != null) {
			return this.mapper.readValue(source, T.class)
		}
		null
	}

	DAO delete(String elementId) {
		EsService.instance.client.delete {
			index Setting.ES_INDEX.value
			type esType
			id elementId
		}
		this
	}
}
