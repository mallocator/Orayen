
package net.pyxzl.orayen

import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j

@Slf4j
@Singleton
class Config {
	static enum Setting {
		CONFIG('conf/orayen.json'),
		ENV('local'),
		PORT('7331'),
		ADMIN_PORT('8080'),
		ADMIN_ROOT('clap://system/web/'),

		def value;
		final def defaultValue;

		Setting(def value) {
			this.defaultValue = value
		}

		def getValue() {
			return value ?: defaultValue;
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
			};
		} catch (FileNotFoundException e) {
			log.info "No configuration could be found at ${Setting.CONFIG.value}, falling back to defaults"
			log.trace('',e)
		}
	}
}
