package net.pyxzl.orayen.restcomponents

import groovy.util.logging.Slf4j
import net.pyxzl.orayen.Config.Setting
import net.pyxzl.orayen.dao.UserDAO
import net.pyxzl.orayen.dto.UserDTO

import org.restlet.security.SecretVerifier

/**
 * @author Ravi Gairola (mallox@pyxzl.net)
 */
@Slf4j
class EsVerifier extends SecretVerifier {
	private static final String ES_TYPE = 'users'

	EsVerifier() {
		def admin =  UserDAO.instance.get('admin')
		if (admin == null) {
			UserDAO.instance.put(new UserDTO('admin', Setting.ADMIN_PASSWORD.value, true))
			log.debug 'Created admin user with configured default password'
		}
	}

	char[] getLocalSecret(final String identifier) {
		def user = UserDAO.instance.get(identifier)
		if (user == null) {
			log.info "User '${identifier}' could not be authenticated because he's not registered"
		}
		return user?.password
	}

	@Override
	public int verify(String identifier, char[] secret) {
		def user = UserDAO.instance.get(identifier)
		if (user == null) {
			log.info "User '${identifier}' could not be authenticated because he's not registered"
			return RESULT_UNKNOWN
		}
		return compare(UserDTO.encrypt(secret), getLocalSecret(identifier)) ? RESULT_VALID
		: RESULT_INVALID
	}
}
