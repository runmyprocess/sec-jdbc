package org.runmyprocess.sec;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Base64;
import java.util.Properties;
import java.util.logging.Level;
import org.runmyprocess.json.JSONArray;
import org.runmyprocess.json.JSONObject;


import static javax.xml.bind.DatatypeConverter.parseBase64Binary;

/**
 *
 * @author Malcolm Haslam <mhaslam@runmyprocess.com>
 *
 * Copyright (C) 2013 Fujitsu RunMyProcess
 *
 * This file is part of RunMyProcess SEC.
 *
 * RunMyProcess SEC is free software: you can redistribute it and/or modify
 * it under the terms of the Apache License Version 2.0 (the "License");
 *
 *   You may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
public class JDBC implements ProtocolInterface {

    final SECLogManager LOG = new SECLogManager(JDBC.class.getName());
    private Response response = new Response();


    public JDBC() {
        // TODO Auto-generated constructor stub
    }

    /**
     *
     * @param error error message
     * @return jsonObject error
     */
    private JSONObject DBAgentError(String error){
        LOG.log(error, Level.SEVERE);
        response.setStatus(400);//sets the return status to internal server error
        JSONObject errorObject = new JSONObject();
        errorObject.put("error", error);
        return errorObject;
    }

    /**
     *  Transforms a result set to a jsonObject
     * @param rs result set to transform
     * @return  jsonObject of the result set
     */
    private static final JSONObject resultSet2JSONObject(ResultSet rs) {
        JSONObject element = null;
        JSONArray joa = new JSONArray();
        JSONObject jo = new JSONObject();
        int totalLength = 0;
        ResultSetMetaData rsmd = null;
        String columnName = null;
        String columnValue = null;
        try {
            rsmd = rs.getMetaData();
            while (rs.next()) {
                element = new JSONObject();
                for (int i = 0; i < rsmd.getColumnCount(); i++) {
                    columnName = rsmd.getColumnName(i+1);
                    columnValue = rs.getString(columnName);
                    element.accumulate(columnName, columnValue);
                }
                joa.add(element);
                totalLength ++;
            }
            jo.accumulate("result", "success");
            jo.accumulate("rows", totalLength);
            jo.accumulate("data", joa);
        } catch (SQLException e) {
            jo.accumulate("result", "failure");
            jo.accumulate("error", e.getMessage());
        }
        return jo;
    }

    /**
     * establishes a connection to the database
     * @param userName
     * @param password
     * @param sqlSource
     * @param sqlDriver
     * @param driverPath
     * @return   the connection object
     * @throws Exception
     */
    private Connection getConnection(String userName, String password, String sqlSource, String sqlDriver,
                                     String driverPath) throws Exception {

        LOG.log("Connecting to "+sqlSource,Level.INFO);
        Connection conn = null;
        Properties connectionProps = new Properties();
        connectionProps.put("user", userName);
        connectionProps.put("password", password);
        URL[] urls = new URL[1];
        File f = new File(driverPath);
        urls[0] = f.toURL();
        URLClassLoader ucl = URLClassLoader.newInstance(urls);
        Driver driver = (Driver)Class.forName(sqlDriver, true, ucl).newInstance();
        DriverManager.registerDriver(new DriverShim(driver));
        conn = DriverManager.getConnection(sqlSource, userName, password);

        LOG.log("Connected to database", Level.INFO);
        return conn;
    }

    /**
     * Executes the sql statement
     * @param con the connection
     * @param sqlStatement the statement
     * @return  a jsonObject with the result of the call
     * @throws SQLException
     */
    private static JSONObject ExecuteStatement(Connection con, String sqlStatement)throws SQLException {

    // Get a statement from the connection
    Statement stmt = con.createStatement() ;
    JSONObject retObj = new JSONObject();
    // Execute the SQL
    if( stmt.execute(sqlStatement) == false )
    {
        // Get the update count
        String rep = "Query OK, "+ stmt.getUpdateCount() + " rows affected" ;
        retObj.put("Message",rep);
    }
    else
    {
        // Get the result set and the metadata
        ResultSet         rs = stmt.getResultSet() ;
        retObj = resultSet2JSONObject(rs);

    }
    stmt.close() ;
    con.close() ;

    return retObj;

    }

    /**
     * Calls to establish the connection and execute the statement
     * @param driverPath
     * @param sqlDriver
     * @param sqlSource
     * @param sqlUsername
     * @param sqlPassword
     * @param sqlStatement
     * @return  the result (jsonObject) of the execution
     * @throws Exception
     */
    private JSONObject Execute(String driverPath, String sqlDriver, String sqlSource, String sqlUsername,
                               String sqlPassword,String sqlStatement) throws Exception{

        Connection con = getConnection(sqlUsername, sqlPassword,sqlSource,sqlDriver,driverPath);
        return ExecuteStatement(con, sqlStatement);
    }

    /**
     * Recieves the information, reads the configuration information and calls the appropriate functions to set the
     * response value
     * @param jsonObject
     * @param configPath
     */
    @Override
    public void accept(JSONObject jsonObject,String configPath) {

        try {

            LOG.log("Searching for config file...", Level.INFO);
            Config conf = new Config("configFiles"+File.separator+ "JDBC.config",true);//sets the config info
            LOG.log( "Config file found  ", Level.INFO);

            String sqlUsername = jsonObject.getString("sqlUsername");
            String sqlPassword = jsonObject.getString("sqlPassword");
            String sqlStatement = jsonObject.getString("sqlStatement");
            String DBType = jsonObject.getString("DBType");
            //decode base 64 if necesary
            if (jsonObject.containsKey("base64Input") && jsonObject.getString("base64Input").equals("true")){
                sqlUsername = new String(Base64.getDecoder().decode(sqlUsername));
                sqlPassword = new String(Base64.getDecoder().decode(sqlPassword));
                sqlStatement = new String(Base64.getDecoder().decode(sqlStatement));
                DBType = new String(Base64.getDecoder().decode(DBType));
            }

            JSONObject prop = JSONObject.fromString(conf.getProperty(DBType));

            JSONObject DBData = Execute(prop.getString("sqlDriverPath"), prop.getString("sqlDriver"),
                    prop.getString("sqlSource"), sqlUsername, sqlPassword,
                    sqlStatement) ;

            response.setStatus(200);//sets the return status to 200
            JSONObject resp = new JSONObject();
            if (jsonObject.containsKey("base64Output") && jsonObject.getString("base64Output").equals("true")){

                resp.put("DBData", new String (Base64.getEncoder().encode(DBData.toString().getBytes())));//sends the info inside an object
            }else{
                resp.put("DBData", DBData);//sends the info inside an object
            }
            response.setData(resp);

        } catch (Exception e) {
            e.printStackTrace();
            response.setData(this.DBAgentError(e.getMessage()));
            SECErrorManager errorManager = new SECErrorManager();
            errorManager.logError(e.getMessage(), Level.SEVERE);

        }
    }

    @Override
    public Response getResponse() {
        return response;
    }

}
