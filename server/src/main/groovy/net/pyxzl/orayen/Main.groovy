/**
 *
 */
package net.pyxzl.orayen

import groovy.util.logging.Slf4j
import net.pyxzl.orayen.restendpoints.ClientResource
import net.pyxzl.orayen.restendpoints.ConfigResource
import net.pyxzl.orayen.restendpoints.RegisterResource
import net.pyxzl.orayen.service.EsService

import org.restlet.Application
import org.restlet.Component
import org.restlet.data.Protocol
import org.restlet.resource.Directory
import org.restlet.routing.Router

/**
 * @author Ravi Gairola (mallox@pyxzl.net)
 *
 */
@Slf4j
class Main extends Application {
	static main(args) {
		System.props.setProperty("org.restlet.engine.loggerFacadeClass","org.restlet.ext.slf4j.Slf4jLoggerFacade")
		Config.instance
		EsService.instance
		new Main()
	}

	Main() {
		setRestEndpoints()
		setWebEndpoints()
	}

	private setRestEndpoints() {
		final Component component = new Component()
		final Router router = new Router()
		router.setName("REST Router")

		router.attach("/config", ConfigResource.class)
		router.attach("/config/{clientid}", ConfigResource.class)
		router.attach("/config/{clientid}/{version}", ConfigResource.class)
		router.attach("/register", RegisterResource.class)
		router.attach("/register/{clientid}", RegisterResource.class)
		router.attach("/client", ConfigResource.class)
		router.attach("/client/{clientid}", ClientResource.class)

		component.getServers().add(Protocol.HTTP, Config.Setting.PORT.value as int)
		component.getDefaultHost().attach("", router)
		component.start()

		log.info "Started REST server on port ${Config.Setting.PORT}"
	}

	private setWebEndpoints() {
		final Component component = new Component()
		final Directory dir = new Directory(getContext(), Config.Setting.ADMIN_ROOT.value)
		dir.setListingAllowed(Config.Setting.ENV.value.equals("dev"))

		component.getClients().add(Config.Setting.ADMIN_ROOT.value.startsWith("clap") ? Protocol.CLAP : Protocol.FILE)
		component.getServers().add(Protocol.HTTP, Config.Setting.ADMIN_PORT.value as int)
		component.getDefaultHost().attach("", dir)
		component.start()

		log.info "Started Web server on port ${Config.Setting.ADMIN_PORT}"
	}
}
