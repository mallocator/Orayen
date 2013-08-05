package net.pyxzl.orayen

import groovy.util.logging.Slf4j
import net.pyxzl.orayen.Config.Setting
import net.pyxzl.orayen.restcomponents.ClientResource
import net.pyxzl.orayen.restcomponents.ConfigResource
import net.pyxzl.orayen.restcomponents.KeystoreResource
import net.pyxzl.orayen.restcomponents.LocalhostFilter
import net.pyxzl.orayen.restcomponents.RegisterResource
import net.pyxzl.orayen.restcomponents.UserResource
import net.pyxzl.orayen.service.CertGenerator

import org.restlet.Application
import org.restlet.Component
import org.restlet.Restlet
import org.restlet.Server
import org.restlet.data.Parameter
import org.restlet.data.Protocol
import org.restlet.resource.Directory
import org.restlet.routing.Filter
import org.restlet.routing.Router
import org.restlet.util.Series

/**
 * @author Ravi Gairola (mallox@pyxzl.net)
 *
 */
@Slf4j
class Main extends Application {
	static main(args) {
		System.props.setProperty("org.restlet.engine.loggerFacadeClass","org.restlet.ext.slf4j.Slf4jLoggerFacade")
		Config.instance
		CertGenerator.instance
		new Main()
	}

	Main() {
		setRestEndpoints()
		setWebEndpoints()
	}

	private setRestEndpoints() {
		final Router router = new Router()
		router.setName("REST Router")

		router.attach("/config", ConfigResource.class)
		router.attach("/config/{clientid}", ConfigResource.class)
		router.attach("/config/{clientid}/{version}", ConfigResource.class)
		router.attach("/register", RegisterResource.class)
		router.attach("/register/{clientid}", RegisterResource.class)
		router.attach("/client", ConfigResource.class)
		router.attach("/client/{clientid}", ClientResource.class)
		router.attach("/keystore", KeystoreResource.class)
		router.attach("/keystore/{format}", KeystoreResource.class)
		router.attach("/keystore/{clientid}/{format}", KeystoreResource.class)
		router.attach("/user", UserResource.class)
		router.attach("/user/{userid}", UserResource.class)

		this.createComponent(router, Setting.LOCAL_PORT, Setting.PORT, "REST")
	}

	private setWebEndpoints() {
		final Component component = new Component()
		final Directory dir = new Directory(getContext(), Setting.ADMIN_ROOT.value)
		dir.setName("Web Router")
		dir.setListingAllowed(Setting.ENV.value.equals("dev"))

		this.createComponent(dir, Setting.LOCAL_ADMIN_PORT, Setting.ADMIN_PORT, "Web")
	}

	private createComponent(Restlet restlet, Setting localPort, Setting globalPort, String type) {
		if (Setting.LOCAL_PORT.value as int) {
			final Component component = new Component()
			if (type.equals("Web")) {
				component.getClients().add(Protocol.FILE)
			}
			component.getServers().add(Protocol.HTTP, localPort.value as int)
			final Filter filter = new LocalhostFilter()
			filter.setContext(component.getContext().createChildContext())
			filter.setNext(restlet)
			component.getDefaultHost().attach("", filter)
			try {
				component.start()
			} catch (BindException e) {
				log.error "Unable to bind port ${localPort} for the local ${type} interface, it's in use by another process"
				log.debug("", e)
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
			component.getClients().add(Protocol.FILE)
			final Server server = component.getServers().add(Protocol.HTTPS, globalPort.value as int)
			final Series<Parameter> parameters = server.getContext().getParameters()
			parameters.add("sslContextFactory", "org.restlet.ext.ssl.PkixSslContextFactory")
			parameters.add("keystorePath", Setting.KEYSTORE.value)
			parameters.add("keystoreType", "JKS")
			parameters.add("keystorePassword", Setting.CERTPASS.value)
			parameters.add("keyPassword", Setting.CERTPASS.value)
			parameters.add("truststorePath", Setting.TRUSTSTORE.value)
			parameters.add("truststoreType", "JKS")
			parameters.add("truststorePassword", Setting.CERTPASS.value)
			parameters.add("disableCrl", "true")
			if (type.equals("REST") && false) {
				parameters.add("needClientAuthentication", "true")
			}
			component.getDefaultHost().attach("", restlet)
			try {
				component.start()
			} catch (BindException e) {
				log.error "Unable to bind port ${globalPort} for the ${type} interface, it's in use by another process"
				log.debug("", e)
				System.exit 1
			}

			log.info "Started ${type} server on port ${globalPort}"
		} else {
			log.info "${type} endpoint has been disabled (${globalPort})"
		}
	}
}
