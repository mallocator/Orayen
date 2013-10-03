package net.pyxzl.orayen.dao

import net.pyxzl.orayen.Config.Setting
import net.pyxzl.orayen.dto.GroupDTO
import net.pyxzl.orayen.service.EsService

@Singleton
class GroupDAO extends DAO {
	@Override
	protected Object getEsType() {
		'group'
	}

	GroupDTO get(String name) {
		def group = EsService.instance.client.get {
			index Setting.ES_INDEX.value
			type esType
			id name
		}
		return this.parseJson(group.response.sourceAsBytes)
	}

	GroupDTO put(GroupDTO group) {
		EsService.instance.client.index {
			index Setting.ES_INDEX.value
			type esType
			id group.id
			source {
				clientIds = group.clientIds
				configIds = group.configIds
				name = group.name
				created = group.created
				lastUpdate = group.lastUpdate
			}
		}
		user
	}
}
