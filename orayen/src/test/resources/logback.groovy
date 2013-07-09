import static ch.qos.logback.classic.Level.ALL
import static ch.qos.logback.classic.Level.DEBUG
import static ch.qos.logback.classic.Level.INFO
import static ch.qos.logback.classic.Level.WARN
import static ch.qos.logback.classic.Level.ERROR
import static ch.qos.logback.classic.Level.OFF

def mainPackage = 'net.pyxzl.orayen'

appender("STDOUT", ConsoleAppender) {
	encoder(PatternLayoutEncoder) { pattern = "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n" }
}

logger(mainPackage, INFO, ["STDOUT"], false)
root(INFO, ["STDOUT"])