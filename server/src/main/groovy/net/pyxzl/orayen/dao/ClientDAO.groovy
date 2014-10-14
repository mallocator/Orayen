package net.pyxzl.orayen.dao

import net.pyxzl.orayen.Config.Setting
import net.pyxzl.orayen.dto.ClientDTO
import net.pyxzl.orayen.service.EsService

@Singleton
class ClientDAO extends DAO {
	@Override
	protected Object getEsType() {
		'clients'
	}

	ClientDTO get(String clientId) {
		def user = EsService.instance.client.get {
			index Setting.ES_INDEX.value
			type esType
			id clientId
		}
		this.parseJsonBytes(user.response.sourceAsBytes)
	}

	ClientDTO put(ClientDTO client) {
		EsService.instance.client.index {
			index Setting.ES_INDEX.value
			type esType
			id client.id
			source {
				address = client.address
				name = client.name
				lastUpdate = client.lastUpdate
				created = client.created
			}
		}
		user
	}
}
