package net.pyxzl.orayen

import groovy.util.logging.Slf4j
import net.pyxzl.orayen.Config.Setting
import net.pyxzl.orayen.restcomponents.ClientResource
import net.pyxzl.orayen.restcomponents.ConfigResource
import net.pyxzl.orayen.restcomponents.EsVerifier
import net.pyxzl.orayen.restcomponents.KeystoreResource
import net.pyxzl.orayen.restcomponents.LocalhostFilter
import net.pyxzl.orayen.restcomponents.RegisterResource
import net.pyxzl.orayen.restcomponents.UserResource
import net.pyxzl.orayen.service.CertGenerator

import org.restlet.Component
import org.restlet.Restlet
import org.restlet.Server
import org.restlet.data.ChallengeScheme
import org.restlet.data.Parameter
import org.restlet.data.Protocol
import org.restlet.resource.Directory
import org.restlet.routing.Filter
import org.restlet.routing.Router
import org.restlet.security.ChallengeAuthenticator
import org.restlet.util.Series

/**
 * @author Ravi Gairola (mallox@pyxzl.net)
 */
@Slf4j
class Main {
	static main(args) {
		System.props.setProperty('org.restlet.engine.loggerFacadeClass', 'org.restlet.ext.slf4j.Slf4jLoggerFacade')
		Config.instance
		CertGenerator.instance
		new Main()
	}

	Main() {
		setWebEndpoints(setRestEndpoints())
	}

	private Router setRestEndpoints() {
		final Router router = new Router()
		router.setName('REST Router')

		router.attach('/config', ConfigResource)
		router.attach('/config/{clientid}', ConfigResource)
		router.attach('/config/{clientid}/{version}', ConfigResource)
		router.attach('/register', RegisterResource)
		router.attach('/register/{clientid}', RegisterResource)
		router.attach('/client', ConfigResource)
		router.attach('/client/{clientid}', ClientResource)
		router.attach('/keystore', KeystoreResource)
		router.attach('/keystore/{format}', KeystoreResource)
		router.attach('/keystore/{clientid}/{format}', KeystoreResource)
		router.attach('/user', UserResource)
		router.attach('/user/{userid}', UserResource)

		this.createComponent(router, Setting.LOCAL_PORT, Setting.PORT, 'REST')
		router
	}

	private setWebEndpoints(final Router router) {
		final Directory dir = new Directory(null, Setting.ADMIN_ROOT.value)
		dir.name = 'Web Router'
		dir.listingAllowed = Setting.ENV.value == 'dev'

		final ChallengeAuthenticator guard = new ChallengeAuthenticator(null, ChallengeScheme.HTTP_BASIC, 'Orayen Configuration Server')
		guard.setVerifier(new EsVerifier())
		guard.setNext(dir)

		final Router proxy = new Router()
		proxy.setName('API Proxy')
		proxy.attach('/api', router)
		proxy.attach('/', guard)

		final Component component = this.createComponent(proxy, Setting.LOCAL_ADMIN_PORT, Setting.ADMIN_PORT, 'Web')

		dir.context = component.context.createChildContext()
	}

	private Component createComponent(final Restlet restlet, final Setting localPort, final Setting globalPort, final String type) {
		final Component webComponent
		if (Setting.LOCAL_PORT.value as int) {
			webComponent = new Component()
			if (type == 'Web') {
				webComponent.clients.add(Protocol.FILE)
			}
			webComponent.servers.add(Protocol.HTTP, localPort.value as int)
			final Filter filter = new LocalhostFilter()
			filter.context = webComponent.context.createChildContext()
			filter.next = restlet
			webComponent.defaultHost.attach('', filter)
			try {
				webComponent.start()
			} catch (BindException e) {
				log.error "Unable to bind port ${localPort} for the local ${type} interface, it's in use by another process"
				log.debug('', e)
			}

			log.info "Started local ${type} server on port ${localPort}"
		} else {
			log.info "Local ${type} endpoint has been disabled (${localPort})"
		}

		if (Setting.PORT.value as int) {
			if (!new File(Setting.KEYSTORE.value).exists()) {
				log.warn "No Keystore at ${Setting.KEYSTORE} -> SSL encryption for ${type} interface has been disabled"
				return
			}
			if (!new File(Setting.TRUSTSTORE.value).exists()) {
				log.warn "No Truststore at ${Setting.TRUSTSTORE} -> SSL encryption for ${type} interface has been disabled"
				return
			}

			final Component component = new Component()
			component.clients.add(Protocol.FILE)
			final Server server = component.servers.add(Protocol.HTTPS, globalPort.value as int)

			final Series<Parameter> parameters = server.context.parameters
			parameters.add('sslContextFactory', 'org.restlet.ext.ssl.PkixSslContextFactory')
			parameters.add('keystorePath', Setting.KEYSTORE.value)
			parameters.add('keystoreType', 'JKS')
			parameters.add('keystorePassword', Setting.CERTPASS.value)
			parameters.add('keyPassword', Setting.CERTPASS.value)
			parameters.add('truststorePath', Setting.TRUSTSTORE.value)
			parameters.add('truststoreType', 'JKS')
			parameters.add('truststorePassword', Setting.CERTPASS.value)
			parameters.add('disableCrl', 'true')
			parameters.add('allowRenegotiate', 'true')
			if (type == 'REST') {
				parameters.add('needClientAuthentication', 'true')
			} else {
				parameters.add('wantClientAuthentication', 'false')
			}
			component.defaultHost.attach('', restlet)
			try {
				component.start()
			} catch (BindException e) {
				log.error "Unable to bind port ${globalPort} for the ${type} interface, it's in use by another process"
				log.debug('', e)
				System.exit 1
			}

			log.info "Started ${type} server on port ${globalPort}"
		} else {
			log.info "${type} endpoint has been disabled (${globalPort})"
		}
		webComponent
	}
}
