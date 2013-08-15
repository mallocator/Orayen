package net.pyxzl.orayen.dao

import net.pyxzl.orayen.Config.Setting
import net.pyxzl.orayen.dto.ConfigDTO
import net.pyxzl.orayen.service.EsService

@Singleton
class ConfigDAO extends DAO {
	protected String getEsType() {
		'config'
	}

	ConfigDTO get(final String configId) {
		def config = EsService.instance.client.get {
			index Setting.ES_INDEX.value
			type esType
			id configId
		}
		return this.parseJson(config.response.sourceAsBytes)
	}

	ConfigDTO put(final ConfigDTO config) {
		EsService.instance.client.index {
			index Setting.ES_INDEX.value
			type esType
			id config.id
			source {
				lastUpdate = config.lastUpdate
				payload = config.payload
				version = config.version
			}
		}
		config
	}
}
