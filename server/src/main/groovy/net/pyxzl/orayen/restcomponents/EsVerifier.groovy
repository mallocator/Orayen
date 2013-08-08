package net.pyxzl.orayen.restcomponents

import groovy.util.logging.Slf4j
import net.pyxzl.orayen.Config.Setting
import net.pyxzl.orayen.service.EsService

import org.restlet.security.LocalVerifier

/**
 * @author Ravi Gairola (mallox@pyxzl.net)
 */
@Slf4j
class EsVerifier extends LocalVerifier {
	private static final String ES_TYPE = 'users'

	EsVerifier() {
		def admin = EsService.instance.client.get {
			index Setting.ES_INDEX.value
			type ES_TYPE
			id 'admin'
		}
		if (!admin.response.exists) {
			EsService.instance.client.index {
				index Setting.ES_INDEX.value
				type ES_TYPE
				id 'admin'
				source { password = Setting.ADMIN_PASSWORD.value }
			}
			log.debug 'Created admin user with configured default password'
		}
	}

	@Override
	char[] getLocalSecret(final String identifier) {
		def user = EsService.instance.client.get {
			index Setting.ES_INDEX.value
			type ES_TYPE
			id identifier
		}
		if (!user.response.exists) {
			log.info "User '${identifier}' could not be authenticated because he's not registered"
		}
		return user.response.source?.password
	}
}
