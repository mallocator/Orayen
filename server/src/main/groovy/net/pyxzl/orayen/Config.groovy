package net.pyxzl.orayen

import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j

@Slf4j
@Singleton
class Config {
	static enum Setting {
		ENV('local'),
		PORT('7331'),
		ADMIN_PORT('8080'),
		CONFIG('conf/orayen.json'),
		ES_CONFIG('conf/elasticsearch.json')

		def value;
		final def defaultValue;
		Setting(def value) {
			this.defaultValue = value
		}

		def getValue() {
			return value ?: defaultValue;
		}
	}

	Config() {
		System.props.each { String k, v ->
			if (k.startsWith('orayen_')) {
				Setting.valueOf(k.substring(7).toUpperCase())?.value = v
			}
		}
		try {
			new JsonSlurper().parseText(new File(Setting.CONFIG.value)?.text)?.each { String k, v ->
				Setting.valueOf(k.toUpperCase())?.value = v
			};
		} catch (FileNotFoundException e) {
			log.info "No configuration could be found at ${Setting.CONFIG.value}, falling back to defaults"
			log.info('',e)
		}
	}
}
