/**
 *
 */
package net.pyxzl.orayen.restcomponents

import groovy.util.logging.Slf4j

import org.restlet.Request
import org.restlet.Response
import org.restlet.routing.Filter


/**
 * @author Ravi Gairola (mallox@pyxzl.net)
 */
@Slf4j
class LocalhostFilter extends Filter {
	private static final Set whitelist = new HashSet([
		'127.0.0.0',
		'0:0:0:0:0:0:0:1'
	])

	@Override
	protected int beforeHandle(final Request request, final Response response) {
		final String ip = request.clientInfo.address
		if (whitelist.contains(ip)) {
			return super.beforeHandle(request, response)
		}
		log.info "Blocking access from unauthorized IP (${ip})"
		STOP
	}
}
