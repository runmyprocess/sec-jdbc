RunMyProcess SEC JDBC Adapter
=============================

The "JDBC Adapter" allows you to access data stored in your databases from the [SEC Manager](https://github.com/runmyprocess/sec-manager). When the Secure Enterprise Connector is used in conjunction with the JDBC Adapter, you can securely access a database from outside a firewall.


##Install and Configure the Adapter
1. Make sure you have [Java](http://www.oracle.com/technetwork/java/index.html) and [Maven](http://maven.apache.org/) installed on your machine.
2. Download the jdbc project and  run mvn clean install on the project's folder.

Run mvn clean install :

	mvn clean install

3. Copy the generated jar file (usually created in a generated "target" folder in the JDBC project's folder) to a folder of your choice.
4. Create a "configFiles" folder in the jar file's path.
5. Inside the "configFiles" folder you must create 2 config files : handler.config and the JDBC.config

The handler.config file should look like this :
    
	#Generic Protocol Configuration
	protocol = DBAgent
	protocolClass = com.runmyprocess.sec.DBAgent
	handlerHost = 127.0.0.1
	connectionPort = 5832
	managerHost = 127.0.0.1
	managerPort = 4444
	pingFrequency = 300
	    
Where :

* **protocol** is the name to identify our Adapter.
* **protocolClass** is the class of the Adapter.
* **handlerHost** is where the Adapter is running.
* **connectionPort** is the port of the adapter where data will be received and returned.
* **managerHost** is where the SEC is running. 
* **managerPort** is the port where the SEC is listening for ping registrations.
* **pingFrequency** is the frequency in which the manager will be pinged (at least three times shorter than what's configured in the manager).
 

The **JDBC.config** file should look like this :

	#DBAgent Configuration
	MYSQL = {"sqlDriver" : "com.mysql.jdbc.Driver", "sqlSource" = "jdbc:mysql://localhost:3306/mydatabase?characterEncoding=UTF-8","sqlDriverPath" = "/pathToDriver/mysql-connector-java-5.0.8-bin.jar" }

Each line of the config file must be populated with the information of the DB we are trying to connect to.
Note that you must provide the path to where the Sql driver is located and configure the information. 

##Running and Testing the Adapter
You can now run the Adapter by executing the generated jar in the chosen path :

    java -jar sec-JDBCAdapter-1.0.jar
    
If everything is configured correctly and the sec-Manager is running you can now Post the manager to retrieve information from the database.
The POST body should look something like this :
    
	{
	"protocol":"DBAgent",
	"data":{
		"DBType" = "MYSQL",
		"sqlUsername"="mysqlUser",
		"sqlPassword"="mysqlPassword",
		"sqlStatement"="SELECT * FROM pet Limit 10"

		}
	}

Note that the DBType should coincide with the type in the configuration file. This value **IS** case sensitive.
The expected return is a JSON object that should look like this :

	{
	"SECStatus":200,
	"DBData":{
		"result":"success",
		"data":[
			{"birth":"1996-02-05","death":"2010-06-06","name":"Pancho","owner":"Malcolm","specie":"dog"},		
			{"birth":"1999-04-09","death":"2012-08-09","name":"Skeff","owner":"Malcolm","specie":"dog"},
			{"birth":"2012-03-01","death":null,"name":"Tuffy","owner":"Axel","specie":"hamster"}
			],
		"rows":3
		}
	}
