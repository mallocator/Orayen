package net.pyxzl.orayen

import groovy.util.logging.Slf4j
import net.pyxzl.orayen.Config.Setting

import org.testng.annotations.Test

@Slf4j
class ConfigTest {
	@Test
	void testConfigSetup() {
		System.props['orayen_env'] = "test_env"
		new Config()
		assert Setting.ENV.value == "test_env"
	}
}
