package org.runmyprocess.sec;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.Properties;

import java.util.logging.Level;


import org.runmyprocess.json.JSONArray;
import org.runmyprocess.json.JSONObject;


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

    // Logging instance

    private static final  SECLogManager LOG = new SECLogManager(JDBC.class.getName());

    private Response response = new Response();


    public JDBC() {
        // TODO Auto-generated constructor stub
    }

    /**
     * Generates the error that will be sent back
     * @param e error
     * @return jsonObject error
     */
    private JSONObject DBAgentError(Exception e){

        response.setStatus(400);//sets the return status to internal server error
        JSONObject errorObject = new JSONObject();
        errorObject.put("error", e.toString());
        System.out.println(e.toString());
        response.setData(errorObject);
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
        return conn;
    }

    /**
     * Executes the sql statement
     * @param con the connection
     * @param sqlStatement the statement
     * @return  a jsonObject with the result of the call
     * @throws SQLException
     */
    private static JSONObject ExecuteStatement(Connection con, String sqlStatement)throws Exception {

    // Get a statement from the connection
    Statement stmt = con.createStatement() ;
    JSONObject retObj = new JSONObject();
    // Execute the SQL
    try{
        LOG.log("Executing query ...", Level.INFO);
        if( stmt.execute(sqlStatement) == false )
        {
            // Get the update count
            String rep = "Query OK, "+ stmt.getUpdateCount() + " rows affected" ;
            retObj.put("Message",rep);
            LOG.log(rep, Level.INFO);
        }
        else
        {
            // Get the result set and the metadata
            ResultSet rs = stmt.getResultSet() ;
            retObj = resultSet2JSONObject(rs);
            LOG.log("Query result: " + retObj.getString("result"), Level.INFO);

        }
    } catch (Exception e){
        LOG.log("Query execution failed", Level.INFO);
        throw new Exception(e);
    }   finally {
        stmt.close() ;
        con.close() ;
    }
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
        try{
            Config conf = new Config("configFiles"+File.separator+ "JDBC.config",true);//sets the config info
            JSONObject prop = JSONObject.fromString(conf.getProperty(jsonObject.getString("DBType")));

            JSONObject DBData = Execute(prop.getString("sqlDriverPath"),prop.getString("sqlDriver"),
                    prop.getString("sqlSource"),jsonObject.getString("sqlUsername"),jsonObject.getString("sqlPassword"),
                    jsonObject.getString("sqlStatement")) ;

            response.setStatus(200);//sets the return status to 200
            JSONObject resp = new JSONObject();
            resp.put("DBData", DBData);//sends the info inside an object
            response.setData(resp);

        } catch (Exception e) {
            DBAgentError(e);
            //LOG.log(e.getLocalizedMessage(), e, Level.SEVERE);
        }
    }

    @Override
    public Response getResponse() {
        return response;
    }

}
