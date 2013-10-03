#Orayen

A configuration management service with libraries for a number of languages to register for changes and query for updates.

## Overview

This is both a library and a stand alone server along with clients available to integrate your current software with. The service provided is a central location, where you can store and update your configuration and push it to all clients connected to the server. Features included are:

* Web interface for editing
* Push capability to send configuration changes to clients
* Poll capabilities, so that client can ask for configuration changes
* Built in clustering of service
* Service can be used as a library and integrated into any (java) application
* Notification Service
* JSON representation of all configuration
* SSL encrypted communication/authentication
* JSON REST interface
* Client implementations for a number of languages

The server itself is relying on Elasticsearch for storing and replicating data. The configuration can be customized to support larger clusters and additional nodes, as well as features such as gateways and rivers. The service itself is written in Groovy. The license for the code is the Apache 2.0 License.

## Clients

There are a number of clients in various languages maintained by the core project, which aim to showcase an implementation of the Orayen protocol. Here is the list of available clients:

* [C#](clients/c%23) - implementation pending
* [Go](clients/go) - implementation pending
* [Java Client](clients/java) - Alpha Version
* [Node.js](clients/nodejs) - implementation pending
* [PHP](clients/php) - implementation pending
* [Python](clients/python) - implementation pending
* [Ruby](clients/ruby) - implementation pending

## The Configuration Service
There are two cases for which this service might be used:

### A million clients and more
Clients register with a number of user defined attributes at the server, which in turn will be able to create groups of clients based on these attributes.
Using the service for example for mobile devices, different configurations can be returned depending on model type, country, language, or whatever attribute you want to supply.

The service will allow you to target very specifically which clients should receive which updates. You can, for example, target a small group to test your configuration changes and then roll them out to the rest of your client population.

Additionally the service will allow to do rolling updates, so that not all clients will get the update at the same time, but subgroups at a time. A number of options will be available, such as updating based on attribute differences, number of changes per minute, or regions.

### Managing a cluster or two
A cluster where each node needs a configuration can use this service to update configurations on all nodes at once.
The server will allow to separate different instances into tiers, groups, regions, or however you plan to manage your set up.

The configuration will also allow to use local variable as configuration place holders, so that preconfigured values such as host names, IP addressed, etc. can be injected from the node receiving the configuration change.  

## Configuration of the Configuration Service
(yes this just got Meta)

### Options

There are 2 ways to set options for the server: Command line arguments and a configuration file. Any configuration that is supposed to be read from the command line has to be prefixed with "orayen_".
Here is an example for passing a custom config location to the server:

    java -jar Orayen.jar -Dorayen_config=/etc/orayen.conf
    
Configuration in the config file is stored in a json format. The default location is in a relative path stored at config/orayen.json.
A sample configuration would look like this:

    {
    	"env": "prod",
    	"port": 443,
    	"local_port": 0,
    	"admin_port": 0,
    	"local_admin_port": 80
    }
    
Any option that is neither set via command line, nor in a config file will fall back to its default.
All options have default setting.

A future feature will allow to change configuration options while running the service, using the admin interface. (After all that's the use case we're building this for, might as well use it)

Options are read in with the following descending priority:

* Runtime Configuration Changes (not yet available)
* Command Line Flags
* Configuration File
* Default Settings

TODO write proper description for each config

_config (Default = config/orayen.json)_  
Location of the configuration file, that will override all default and command line options

_env (Default = local)_  
Possible Environments are: dev, embedded and prod

_port (Default = 7443)_  
Server port on which REST calls can be made via https authentication

_local\_port (Default = 7000)_  
Server port on which REST calls can be made without https, but only from localhost

_admin\_port (Default = 8443)_  
Server port on which the admin interface can be accessed via https

_local\_admin\_port (Default = 8000)_  
Server port on which the admin interface can be accssed without https, but only from localhost

_admin\_root (Default = file:///var/www/orayen/)_
Directory in which to look for the web root that holds the admin interface

_admin\_password (Default = password)_  
The default password for the admin user to the admin interface

_keystore (Default = config/keystore.jks)_  
Keystore location that holds the certificate information for the server https connector

_truststore (Default = config/truststore.jks)_  
Truststore location that holds the certificate information for clients trying to access the server

_certstore (Default = config/certs/)_  
Directory in which client certificates will be stored

_certpass (Default = Orayen)_  
The password used to lock the client, key and trust store

_es\_index (Default = orayen)_  
ElasticSearch index name

_ES\_CONFIG (Default = built in config)_  
ElasticSearch configuration file

_no\_color (Default = false)_  
Disables coloured command line output when set to "true"

### Web Interface Access
The admin interface is your go to place for all configuration changes and management of clients. Here you will be able to change/create configurations, manage your clients in groups, and roll out updates to specified targets.

The interface can be accessed from the same host as the server without any authentication (if the port has been enabled in the options).
It can also be accessed from anywhere via https and a user/password combination. The web service uses DIGEST for it's authentication mechanism.

The default administration password is set via the _admin\_password_ configuration option and will only be set if no admin user has yet been created.
All users are stored in ElasticSearch and can be viewed and manipulated if the ElasticSearch configuration allows it.

### REST Access
Clients communicate with the service via a REST interface that runs on a separate port. Clients on the same host have access to the local admin port and don't need any further authentication.
Clients connecting from other hosts need to authenticate via client certificate provided by the server.

The server currently only supports a polling architecture, but future updates will include the ability to push updates to clients as well, if their environment allows it.
Clients in general have to register with the configuration server to receive updates, as it is dependent on their attributes, what client groups they will join and what updates they will receive.

TODO outline protocol

### Trust and Key Store Configuration

The server will generate a truststore and a keystore when it start automatically. From this information a client keystore will be generated that can be distributed with your clients to communicate with the server.

The certificates in these stores are of course self signed and will not be verified by third parties. If you want to supply your own certificate chains, then you need to make sure the truststore and keystore are at the configured locations.
Additionally the certificates are being stored within elasticsearch so that a clustered setup will share the same certificate. So if you replace the existing trust and key store, you will also need to delete the certificates stored in elasticsearch.
(In future updates this will be simplified through a command line option)

TODO show how to set up a client with the right certificate
TODO show how to create a keystore with your own certificate

### ElasticSearch Configuration

The server uses elasticsearch for storing data and as a clustering infrastructure. If you want to customize the elasticsearch configuration beyond the index that is used, you can do so by specifying an external configuration file.
Other then that, the default elasticsearch configuration actually depends on the environment that the server was set up with.

For details on how to set up an elasticsearch configuration refer to http://www.elasticsearch.org/guide/. 

An example for a configuration would look like this:

	{
	    "cluster": {
	        "name": "orayen"
	    },
	    "path": {
	        "data": "data/elasticsearch"
	    },
	    "http": {
	    	"enabled": false
	    },
	    "index": {
	        "analysis": {
	            "filter": {
	                "ngram_filter": {
	                    "type": "nGram",
	                    "min_gram": 2,
	                    "max_gram": 50
	                }
	            },
	            "analyzer": {
	                "ngram_analyzer": {
	                    "type": "custom",
	                    "tokenizer": "standard",
	                    "filter": "lowercase,ngram_filter"
	                }
	            }
	        }
	    }
	}
	
Note that the Orayen server depends on using the ngram_analyzer, so make sure to include it in your custom configuration.

## Roadmap

* Allow to make changes to server configuration at runtime (not all options will be applied without a restart)

## Changelog

### v0.0.1
T.B.D.