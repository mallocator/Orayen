
package net.pyxzl.orayen

import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j

/**
 * @author Ravi Gairola (mallox@pyxzl.net)
 */
@Slf4j
@Singleton
class Config {
	/**
	 * @author Ravi Gairola (mallox@pyxzl.net)
	 */
	static enum Setting {
		CONFIG('config/orayen.json'),				// Location of the configuration file, that will override all default and command line options
		ENV('embedded'),							// Possible Environments are: dev, embedded and prod
		PORT('7443'),								// Server port on which REST calls can be made via https authentication
		LOCAL_PORT('7000'),							// Server port on which REST calls can be made without https, but only from localhost
		ADMIN_PORT('8443'),							// Server port on which the admin interface can be accessed via https
		LOCAL_ADMIN_PORT('8000'),					// Server port on which the admin interface can be accssed without https, but only from localhost
		ADMIN_ROOT('file:///var/www/orayen/'),		// Directory in which to look for the web root that holds the admin interface
		BCRYPT_SALT('a123c65da901f'),				// Salt used to encrypt user passwords
		ADMIN_PASSWORD('password'),					// The default password for the admin user to the admin interface
		KEYSTORE('config/keystore.jks'),			// Keystore location that holds the certificate information for the server https connector
		TRUSTSTORE('config/truststore.jks'),		// Truststore location that holds the certificate information for clients trying to access the server
		CERTSTORE('config/certs/'),					// Directory in which client certificates will be stored
		CERTPASS('Orayen'),							// The password used to lock the client, key and trust store
		ES_INDEX('orayen'),							// ElasticSearch index name
		ES_CONFIG(null),							// ElasticSearch configuration file
		NO_COLOR(''),								// Disables colored command line output when set to "true"

		def value
		final defaultValue

		Setting(def value) {
			this.defaultValue = value
		}

		def getValue() {
			value ?: defaultValue
		}

		@Override
		String toString() {
			'[' + super.toString().toLowerCase() + ': ' + getValue() + ']'
		}
	}

	private Config() {
		log.debug 'Reading Configuration'
		if (System.props['orayen_config']!=null) {
			Setting.CONFIG.value = System.props['orayen_config'].trim()
		}
		try {
			new JsonSlurper().parseText(new File(Setting.CONFIG.value)?.text)?.each { String k, String v ->
				Setting.valueOf(k.toUpperCase())?.value = v.trim()
			}
		} catch (FileNotFoundException e) {
			log.info "No configuration could be found at ${Setting.CONFIG}, falling back to defaults"
			log.trace('',e)
		}
		System.props.each { String k, String v ->
			if (k.startsWith('orayen_')) {
				Setting.valueOf(k.substring(7).toUpperCase())?.value = v.trim()
			}
		}
	}
}
