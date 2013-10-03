package net.pyxzl.orayen.dao

import net.pyxzl.orayen.Config.Setting
import net.pyxzl.orayen.dto.ConfigDTO
import net.pyxzl.orayen.service.EsService

@Singleton
class ConfigDAO extends DAO {
	protected String getEsType() {
		'config'
	}

	ConfigDTO get(final String label, final String version) {
		// TODO test this!
		def config = EsService.instance.client.search {
			indices Setting.ES_INDEX.value
			types esType
			source {
				query {
					bool {
						must[term(configType: label)]
						should[term(version: version)]
					}
				}
				sort [version { order : 'desc' }]
			}
		}
		return this.parseJson(config.response.hits[1].sourceAsString)
	}

	ConfigDTO put(final ConfigDTO config) {
		EsService.instance.client.index {
			index Setting.ES_INDEX.value
			type esType
			source {
				configType = config.label
				lastUpdate = config.lastUpdate
				payload = config.payload
				version = config.version
			}
		}
		config
	}

	@Override
	public DAO delete(final String label, final String version) {
		EsService.instance.client.deleteByQuery {
			indices Setting.ES_INDEX.value
			types esType
			source {
				query {
					bool {
						must[term(configType: label)]
						should[term(version: version)]
					}
				}
			}
		}
	}
}
