/*
 * Utility functions to establish a connection and initialize or shutdown
 * a database, using the database as defined by system properties.
 */

import java.sql.Connection
import java.sql.DriverManager
import java.sql.Statement
import java.util.Properties
import java.io.File
import java.io.FileInputStream

package utils {

  object JdbcConnection {

    val defaultDriver = "org.h2.Driver"
    val defaultUrl = "jdbc:h2:database/db"
    val defaultUrlPrefix = "jdbc:h2:"
    
    var driver = defaultDriver    
    var url = defaultUrl
    var urlPrefix = defaultUrlPrefix
    var user = ""
    var password = ""
    
    var statement: Statement = null
    var conn: Connection = null
    var readOnly = false
    
    
    // connect specifying no db name, the whole db url will be expected to
    // be in the config file.
    // the zero parameter method will establish a writable (non-read-only) 
    // connection.
    def getConnection(readOnly: Boolean = false): Connection = {
      this.readOnly = readOnly
      setParmsFromConfig
      establishConnection(driver,addReadOnlyParm(url),readOnly)
    }
    
    // connect specifying a db name
    def getConnection(db: String, readOnly: Boolean): Connection  = {
      this.readOnly = readOnly
      setParmsFromConfig
      establishConnection(driver,addReadOnlyParm(urlPrefix+db),readOnly)
    } // getConnection

        
    def shutdown(): Unit = {
      if(driver == "" ||  conn == null) {
        throw new RuntimeException("Cannot shutdown a DB, do not have a driver or connection");
      }
      if(!readOnly) {
        statement.execute("COMMIT")
      }
      if(driver == "org.hsqldb.jdbcDriver") {
        if(!readOnly) {
          statement.execute("CHECKPOINT")
        }
        statement.execute("SHUTDOWN")
      } else if(driver == "org.h2.Driver") {
        // for now we use the default shutdown which happens after the connection closes
      }
      conn.close()
    }

    def shutdownError(): Unit = {
      conn.close()
    }
    
    // this is used to establish the actual connection once we have all the 
    // driver and url information we need
    private def establishConnection(driver: String, url: String, readOnly: Boolean): Connection = {
      Class.forName(driver)          
      // appending MV_STORE=FALSE to the URL, to solve the performance degradation problem
      // in version 1.4 as per
      // https://groups.google.com/d/msg/h2-database/IriOPTP4XHM/PFIiW0fcAgAJ
      conn = DriverManager.getConnection(url+";MV_STORE=FALSE",user,password)
      statement = conn.createStatement()
      if(driver == "org.hsqldb.jdbcDriver") {
        if(!readOnly) {
          statement.execute("SET DATABASE TRANSACTION CONTROL MVCC")
          statement.execute("SET FILES LOG FALSE")
          statement.execute("SET FILES NIO SIZE 4096")  // depending on memory, try other values or remove
          statement.execute("CHECKPOINT")
          statement.execute("SET AUTOCOMMIT FALSE")
        }
      } else if(driver == "org.h2.Driver") {
        if(!readOnly) {
          statement.execute("SET AUTOCOMMIT FALSE")
          statement.execute("SET UNDO_LOG 0")
          statement.execute("SET LOCK_MODE 0") // 0 could be dangerous, 3 is default
          statement.execute("SET LOG 0")        
        }
        statement.execute("SET CACHE_SIZE 1048576")
      }     
      conn      
    }
    
    // get the config properties, if any
    // if we have something, use it to overwrite the defaults
    def setParmsFromConfig() = {
      val props = new Properties()      
      val configFileName = System.getProperty("config")
      if(configFileName != null) {
        val configFile = new File(configFileName)
        if(configFile.exists()) {
          props.load(new FileInputStream(configFile))
        } else {
          throw new RuntimeException("Database configuration properties file does not exist: "+configFile)
        }
      } 
      if(props.getProperty("lodie.prep.db.driver") != null) {
        driver = props.getProperty("lodie.prep.db.driver")
      }
      if(props.getProperty("lodie.prep.db.urlPrefix") != null) {
        urlPrefix = props.getProperty("lodie.prep.db.urlPrefix")
      }
      if(props.getProperty("lodie.prep.db.url") != null) {
        url = props.getProperty("lodie.prep.db.url")
      }
      if(props.getProperty("lodie.prep.db.user") != null) {
        user = props.getProperty("lodie.prep.db.user")
      }
      if(props.getProperty("lodie.prep.db.password") != null) {
        password = props.getProperty("lodie.prep.db.password")
      }      
    }
    
    def addReadOnlyParm(url: String): String = {
      if(readOnly) {
        if(driver == "org.hsqldb.jdbcDriver") {
          url+";readonly=true"
        } else if(driver == "org.h2.Driver") {
          url+";ACCESS_MODE_DATA=r;FILE_LOCK=NO"
        } else {
          url
        }
      } else {
        url
      }      
    }

  } // object
} // package