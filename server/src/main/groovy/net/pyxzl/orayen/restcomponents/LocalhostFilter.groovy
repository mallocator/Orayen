/**
 *
 */
package net.pyxzl.orayen.restcomponents

import groovy.util.logging.Slf4j

import org.restlet.Request
import org.restlet.Response
import org.restlet.routing.Filter

/**
 * @author Ravi Gairola (ravig@motorola.com)
 */
@Slf4j
public class LocalhostFilter extends Filter {
	private static final Set whitelist = new HashSet([
		"127.0.0.0",
		"0:0:0:0:0:0:0:1"
	])

	@Override
	protected int beforeHandle(final Request request, final Response response) {
		final String ip = request.getClientInfo().getAddress()
		if (whitelist.contains(ip)) {
			return super.beforeHandle(request, response)
		}
		log.info "Blocking access from unauthorized IP (${ip})"
		return STOP
	}
}
