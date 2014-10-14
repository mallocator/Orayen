package net.pyxzl.orayen.service

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

import groovy.util.logging.Slf4j


/**
 * @author Ravi Gairola (mallox@pyxzl.net)
 */
@Slf4j
@Singleton(strict = false)
class EsService {
	static final String	configLocation	= 'conf/elasticsearch.json'
	GClient				client
	Node				node

	EsService() {
		ESLoggerFactory.setDefaultFactory(new Slf4jESLoggerFactory())
		start()
	}

	EsService start() {
		log.info 'Starting ElasticSearch Database'
		if (node == null) {
			node = NodeBuilder.nodeBuilder().settings(this.settings).node()
			client = new GClient(node.client())
		}
		this.checkMapping()
		client.admin.cluster.prepareHealth(Setting.ES_INDEX.value).setWaitForYellowStatus().execute().actionGet()
		log.info 'ElasticSearch Database has been started'
		this
	}

	private Builder getSettings() {
		if (Setting.ES_CONFIG.value != null) {
			try {
				return ImmutableSettings.settingsBuilder().loadFromSource(new File(Setting.ES_CONFIG.value).text)
			} catch (IOException e) {
				log.info "Couldn't find elasticsearch config in conf directory -> using config 'elasticsearch-${Setting.ENV.value}.json' from classpath"
			}
		}
		return ImmutableSettings.settingsBuilder().loadFromClasspath("elasticsearch-${Setting.ENV.value}.json")
	}

	EsService stop() {
		log.info 'Stopping ElasticSearch Database'
		if (node != null) {
			node.close()
			node = null
		}
		log.info 'ElasticSearch Database has been stopped'
		this
	}

	private checkMapping() {
		[
			'client',
			'config',
			'user',
			'ssl',
			'group'
		].each { String type ->
			this.createMapping(type)
		}
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
			this.client.admin.indices
					.preparePutMapping(Setting.ES_INDEX.value)
					.setType(type)
					.setSource(mapping)
					.setIgnoreConflicts(true)
					.execute().actionGet()
		} catch (ElasticSearchException e) {
			log.debug "Mapping already exists for index ${Setting.ES_INDEX} and type ${type}"
		}
	}
}
