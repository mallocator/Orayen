#Orayen

A configuration management service with libraries for a number of languages to register for changes and query for updates.

## Overview

This is both a library and a stand alone server along with clients available to integrate your current software with. The service provided is a central location, where you can store and update your configuration and push it to all clients connected to the server. Features included are:

* Web interface for editing
* Push capability to send config changes to clients
* Poll capabilities, so that client can ask for configuration changes
* Built in clustering capabilities
* Service can be used as a library and integrated into any (java) application
* Notification Service
* JSON representation of all configuration
* Optional SSL encrypted communication
* JSON REST interface
* Client implementations for a number of languages

The server itself is relying on Elasticsearch for storing and replicating data. The configuration can be customized to support larger clusters and additional nodes, as well as features such as gateways and rivers. The service itself is written in Groovy. The license for the code is the Apache 2.0 License.