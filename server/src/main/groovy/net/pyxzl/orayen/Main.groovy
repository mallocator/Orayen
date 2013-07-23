/**
 *
 */
package net.pyxzl.orayen

import groovy.util.logging.Slf4j
import net.pyxzl.orayen.restendpoints.ConfigResource

import org.restlet.Application
import org.restlet.Component
import org.restlet.data.Protocol
import org.restlet.routing.Router

/**
 * @author Ravi Gairola (mallox@pyxzl.net)
 *
 */
@Slf4j
class Main extends Application {
	static main(args) {
		new Main()
	}

	Main() {
		final Router router = new Router(getContext())
		router.attach("/config", ConfigResource.class)
		router.attach("/config/{id}", ConfigResource.class)

		final Component component = new Component()
		component.getServers().add(Protocol.HTTP, Config.Setting.PORT.value as int)
		component.getDefaultHost().attach("", router)
		component.start()

		log.info 'REST Interface has started'
	}
}
