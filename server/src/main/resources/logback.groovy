import ch.qos.logback.classic.filter.LevelFilter;

import static ch.qos.logback.classic.Level.ALL
import static ch.qos.logback.classic.Level.DEBUG
import static ch.qos.logback.classic.Level.INFO
import static ch.qos.logback.classic.Level.WARN
import static ch.qos.logback.classic.Level.ERROR
import static ch.qos.logback.classic.Level.OFF
import static ch.qos.logback.core.spi.FilterReply.ACCEPT
import static ch.qos.logback.core.spi.FilterReply.DENY

def mainPackage = 'net.pyxzl.orayen'
def project = 'orayen'
def logpath = System.properties.'logging.dir' ?: 'target/logs/'
def logpattern = "%d{MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
def rollingPattern = "%d{yyyy-MM-dd}"

appender("STDOUT", ConsoleAppender) {
	encoder(PatternLayoutEncoder) { pattern = logpattern }
}

appender("FILE", RollingFileAppender) {
	file = logpath + project +".log"
	encoder(PatternLayoutEncoder) { pattern = logpattern }
	rollingPolicy(TimeBasedRollingPolicy) {
		fileNamePattern = logpath + project + "." + rollingPattern + ".log"
		maxHistory = 31
	}
}

appender("ERROR_FILE", RollingFileAppender) {
	file = logpath + project + ".error.log"
	encoder(PatternLayoutEncoder) { pattern = logpattern }
	rollingPolicy(TimeBasedRollingPolicy) {
		fileNamePattern = logpath + project + ".error." + rollingPattern + ".log"
		maxHistory = 31
	}
	filter(LevelFilter) {
		level = ERROR
		onMatch = ACCEPT
		onMismatch = DENY
	}
}

appender("ROOT", RollingFileAppender) {
	file = logpath + "root.log"
	encoder(PatternLayoutEncoder) { pattern = logpattern }
	rollingPolicy(TimeBasedRollingPolicy) {
		fileNamePattern = logpath + "root." + rollingPattern + ".log"
		maxHistory = 31
	}
}

logger(mainPackage, INFO, ["FILE", "ERROR_FILE", "STDOUT"], false)
logger("org.restlet", ALL, ["FILE", "ERROR_FILE", "STDOUT"], false)
root(INFO, ["ROOT"])