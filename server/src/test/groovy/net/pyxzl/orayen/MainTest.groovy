package net.pyxzl.orayen

import groovy.util.logging.Slf4j

import org.testng.Assert
import org.testng.annotations.Test

@Slf4j
class MainTest {
	def variable = 'blah blubb'

	@Test
	void testName() {
		Assert.assertTrue(true, "Stub test to see if compilation works")
		log.info "Everything at ${variable} seems to be fine."
	}
}
