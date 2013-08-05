package net.pyxzl.orayen.service

import groovy.util.logging.Slf4j
import net.pyxzl.orayen.Config
import net.pyxzl.orayen.Config.Setting

import org.elasticsearch.ElasticSearchException
import org.elasticsearch.ExceptionsHelper
import org.elasticsearch.common.logging.ESLoggerFactory
import org.elasticsearch.common.logging.slf4j.Slf4jESLoggerFactory
import org.elasticsearch.common.settings.ImmutableSettings
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.settings.Settings.Builder
import org.elasticsearch.groovy.client.GClient
import org.elasticsearch.indices.IndexAlreadyExistsException
import org.elasticsearch.node.Node
import org.elasticsearch.node.NodeBuilder


@Slf4j
@Singleton
class EsService {
	static final String	configLocation	= "conf/elasticsearch.json"
	GClient				client
	Node				node

	EsService() {
		ESLoggerFactory.setDefaultFactory(new Slf4jESLoggerFactory())
		start()
	}

	EsService start() {
		log.info "Starting ElasticSearch Database"
		if (node == null) {
			node = NodeBuilder.nodeBuilder().settings(this.getSettings()).node()
		}
		if (client == null) {
			client = new GClient(node.client())
		}
		this.checkMapping()
		client.admin.cluster.prepareHealth(Setting.ES_INDEX.value).setWaitForYellowStatus().execute().actionGet()
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
			node.close()
			node = null
		}
		if (client != null) {
			client.close()
			client = null
		}
		log.info "ElasticSearch Database has been stopped"
		return this
	}

	private checkMapping() {
		this.createMapping("client")
		this.createMapping("config")
		this.createMapping("user")
		this.createMapping("ssl")
	}

	private createMapping(String type) {
		final String mapping = this.class.getResource("/mapping/${type}.json").text
		try {
			this.client.admin.indices.prepareCreate(Setting.ES_INDEX.value).addMapping(type, mapping).execute().actionGet()
			log.info "Created Index ${Setting.ES_INDEX} with ${type}"
		} catch (Exception e) {
			if (ExceptionsHelper.unwrapCause(e) instanceof IndexAlreadyExistsException) {
				log.debug "Not creating Index ${Setting.ES_INDEX} as it already exists"
			}
			else if (ExceptionsHelper.unwrapCause(e) instanceof ElasticSearchException) {
				log.debug "Mapping ${Setting.ES_INDEX}.${type} already exists and will not be created"
			}
			else {
				log.warn("failed to create index [${Setting.ES_INDEX}], disabling river...", e)
				return
			}
		}

		try {
			this.client.admin.indices.preparePutMapping(Setting.ES_INDEX.value).setType(type).setSource(mapping).setIgnoreConflicts(true).execute().actionGet()
		} catch (ElasticSearchException e) {
			log.debug "Mapping already exists for index ${Setting.ES_INDEX} and type ${type}"
		}
	}
}
