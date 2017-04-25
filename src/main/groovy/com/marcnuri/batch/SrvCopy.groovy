#!/usr/bin/env groovy
/**
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.marcnuri.batch

import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.ChannelSftp

/**
 *
 * Created by Marc Nuri <marc@marcnuri.com>
 */
class SrvCopy {

	private final String sourceHost
	private final String destinationHost
	private final String user
	private final String password
	private final String[] patterns
	

	
    SrvCopy(String sourceHost, String destinationHost, String user,  String password, String... patterns){
		this.sourceHost = sourceHost
		this.destinationHost = destinationHost
		this.user = user
		this.password = password
		this.patterns = patterns
    }
	
    void run(){

		println("\n======================================================================================")
		println(" ____  ____ \n|    \\|  _ \\\n| | | | | | |\n|_|_|_|_| |_|")
		println("\n======================================================================================\n")
		if(patterns.length == 0){
			println("Nothing to copy")
			return
		}
		//Don't check hosts
		final Properties config = new Properties()
		config.put("StrictHostKeyChecking", "no")
		
		final JSch jsch = new JSch()
		final Session fromSession = jsch.getSession(user, sourceHost)
		fromSession.setConfig(config)
		fromSession.setPassword(password)
		fromSession.connect(10000)
		println("Source Session connected [$sourceHost]")
		final Session destinationSession = jsch.getSession(Variables.USER, destinationHost)
		destinationSession.setConfig(config)
		destinationSession.setPassword(password)
		destinationSession.connect(10000)
		println("Destination Session connected [$destinationHost]")
		////////////////////////////////////////////////////////////////////////////////
		
		for(String pattern : patterns){
			final String filePath = getFilePath(fromSession, pattern)
			if(filePath != null && !filePath.isEmpty()){
				copyFile(fromSession, destinationSession, filePath)
				//Change file permission
				BatchShell.runCommand(destinationSession, "chmod 666 $filePath")
				println("Copied file: $filePath")
			}
		}
		
		////////////////////////////////////////////////////////////////////////////////
		destinationSession.disconnect()
		fromSession.disconnect()
		println("Soruce Session disconnected [sourceHost]")
		println("Destination Session disconnected [destinationHost]")
	}
	
	private static void copyFile(Session fromSession, Session destinationSession, String fileName) throws Exception{
		final ChannelSftp sourceCh = (ChannelSftp) fromSession.openChannel("sftp")
		sourceCh.connect(1000)
		final ChannelSftp destinationCh = (ChannelSftp) destinationSession.openChannel("sftp")
		destinationCh.connect(1000)
		try{
			destinationCh.put(sourceCh.get(fileName), fileName, ChannelSftp.OVERWRITE)
		} catch(Exception ex){ println(ex.getMessage())}
		destinationCh.disconnect()
		sourceCh.disconnect()
	}
	
	private static String getFilePath(Session s, String filePattern){
		final StringBuilder ret = new StringBuilder()
        final ChannelExec ch = (ChannelExec)s.openChannel("exec")
        ch.setCommand("ls -rt $filePattern | tail -1")
        ch.setInputStream(null)
        //ch.setOutputStream(System.out)
        ch.setErrStream(System.err)
        final InputStream is = ch.getInputStream()
        final OutputStream os =ch.getOutputStream()
        final BufferedReader br  = new BufferedReader(new InputStreamReader(is))
        String line = null
        ch.connect(10000)
        while(true){
            while((line = br.readLine())!= null){
                ret.append(line)
            }
            if(ch.isClosed()){
                break
            }
            try{Thread.sleep(1000)}catch(Exception ignore){}
        }
        final int exitStatus = ch.getExitStatus()
        os.close()
        br.close()
        is.close()
        ch.disconnect()
		return exitStatus == 0 ? ret.toString() : null
	}
}