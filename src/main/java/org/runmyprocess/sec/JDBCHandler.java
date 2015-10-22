package org.runmyprocess.sec;

import org.runmyprocess.sec.Config;
import org.runmyprocess.sec.GenericHandler;
import org.runmyprocess.sec.SECErrorManager;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

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
public class JDBCHandler {
    /**
     * reads the configuration files and calls the run method of the generic handler
     * @param args
     * @throws IOException
     */
    public static void main(String [] args)throws IOException {
        // Logging instance
        final SECLogManager LOG = new SECLogManager(JDBCHandler.class.getName());
        try{
            GenericHandler genericHandler = new GenericHandler();//Creates a new instance of generic handler

            LOG.log("Searching for config file...", Level.INFO);
             Config conf = new Config("configFiles"+File.separator+"handler.config",true);//sets the congif info
            LOG.log("Handler config file found for manager ping port " + conf.getProperty("managerPort"), Level.INFO);
            genericHandler.run( conf);//Runs the handler
        }catch( Exception e ){
            SECErrorManager errorManager = new SECErrorManager();//creates a new instance of the SDK error manager
            errorManager.logError(e.getMessage(), Level.SEVERE);//logs the error
            e.printStackTrace();//prints the error stack trace
        }
    }

}
