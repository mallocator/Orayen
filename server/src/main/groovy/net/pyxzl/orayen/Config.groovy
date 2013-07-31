
package net.pyxzl.orayen

import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j

@Slf4j
@Singleton
class Config {
	static enum Setting {
		CONFIG('config/orayen.json'),				// Location of the configuration file, that will override all default and command line options
		ENV('local'),								// Possible Environments are: dev, local and prod
		PORT('7443'),								// Server port on which REST calls can be made via https authentication
		LOCAL_PORT('7000'),							// Server port on which REST calls can be made without https, but only from localhost
		ADMIN_PORT('8443'),							// Server port on which the admin interface can be accessed via https
		LOCAL_ADMIN_PORT('8000'),					// Server port on which the admin interface can be accssed without https, but only from localhost
		ADMIN_ROOT('file:///var/www/orayen/'),		// Directory in which to look for the web root that holds the admin interface
		KEYSTORE('config/keystore.jks'),			// Keystore location that holds the certificate information for the server https connector
		TRUSTSTORE('config/truststore.jks'),		// Truststore location that holds the certificate information for clients trying to access the server
		ES_INDEX('orayen'),							// ElasticSearch index name
		NO_COLOR(''),								// Disables coloured command line output when set to "true"

		def value
		final def defaultValue

		Setting(def value) {
			this.defaultValue = value
		}

		def getValue() {
			return value ?: defaultValue
		}

		@Override
		String toString() {
			return "[" + super.toString().toLowerCase() + ": " + getValue() + "]"
		}
	}

	private Config() {
		log.debug "Reading Configuration"
		System.props.each { String k, String v ->
			if (k.startsWith('orayen_')) {
				Setting.valueOf(k.substring(7).toUpperCase())?.value = v.trim()
			}
		}
		try {
			new JsonSlurper().parseText(new File(Setting.CONFIG.value)?.text)?.each { String k, String v ->
				Setting.valueOf(k.toUpperCase())?.value = v.trim()
			}
		} catch (FileNotFoundException e) {
			log.info "No configuration could be found at ${Setting.CONFIG}, falling back to defaults"
			log.trace('',e)
		}
	}
}
