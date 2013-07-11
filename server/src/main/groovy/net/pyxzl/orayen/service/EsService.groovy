package net.pyxzl.orayen.service

import groovy.util.logging.Slf4j
import net.pyxzl.orayen.Config
import net.pyxzl.orayen.Config.Setting

import org.elasticsearch.client.Client
import org.elasticsearch.common.logging.ESLoggerFactory
import org.elasticsearch.common.logging.slf4j.Slf4jESLoggerFactory
import org.elasticsearch.common.settings.ImmutableSettings
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.settings.Settings.Builder
import org.elasticsearch.node.Node
import org.elasticsearch.node.NodeBuilder


@Slf4j
@Singleton
class EsService {
	static final String	configLocation	= "conf/elasticsearch.json";
	Client				client;
	Node				node;

	EsService() {
		ESLoggerFactory.setDefaultFactory(new Slf4jESLoggerFactory())
		start();
	}

	EsService start() {
		log.info "Starting ElasticSearch Database"
		if (node == null) {
			node = NodeBuilder.nodeBuilder().settings(this.getSettings()).node()
		}
		if (client == null) {
			client = node.client()
		}
		client.admin().cluster().prepareHealth().setWaitForYellowStatus().execute().actionGet()
		log.info "ElasticSearch Database has been started"
		return this
	}

	private Builder getSettings() {
		try {
			return ImmutableSettings.settingsBuilder().loadFromSource(new File(Setting.CONFIG.value).text)
		} catch (IOException e) {
			log.info "Couldn't find elasticsearch config in conf directory -> using config 'elasticsearch-${Setting.ENV.value}.json' from classpath"
			return ImmutableSettings.settingsBuilder().loadFromClasspath("elasticsearch-${Setting.ENV.value}.json")
		}
	}

	EsService stop() {
		log.info "Stopping ElasticSearch Database"
		if (node != null) {
			node.close();
			node = null
		}
		if (client != null) {
			client.close()
			client = null
		}
		log.info "ElasticSearch Database has been stopped"
		return this
	}
}
