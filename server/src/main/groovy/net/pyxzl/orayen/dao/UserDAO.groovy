package net.pyxzl.orayen.dao

import net.pyxzl.orayen.Config.Setting
import net.pyxzl.orayen.dto.UserDTO
import net.pyxzl.orayen.service.EsService

@Singleton
class UserDAO extends DAO {
	@Override
	protected Object getEsType() {
		'users'
	}

	UserDTO get(String name) {
		def user = EsService.instance.client.get {
			index Setting.ES_INDEX.value
			type esType
			id name
		}
		if (user.response.exists) {
			return new UserDTO(user.response.id, user.response.source.password)
		}
		null
	}

	UserDTO put(UserDTO user) {
		EsService.instance.client.index {
			index Setting.ES_INDEX.value
			type esType
			id user.id
			source { password = user.password }
		}
		user
	}
}
