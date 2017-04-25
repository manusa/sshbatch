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


// https://mvnrepository.com/artifact/com.jcraft/jsch
@Grapes([
		@Grab(group = 'com.jcraft', module = 'jsch', version = '0.1.54'),
		@groovy.lang.GrabConfig(systemClassLoader = true)
])

import java.io.InputStream
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintStream
import java.util.Properties

import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import com.jcraft.jsch.Channel
import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.ChannelShell

/**
 *
 * Created by Marc Nuri <marc@marcnuri.com>
 */
class BatchShell {

    private final List<ShellAction> shellActions
    
    BatchShell(String host, String user, String sudoUser, String password,
        Object[] commands){
		this(new ShellAction(host, user, password, sudoUser, commands))
    }
	
	BatchShell(ShellAction shellAction){
		this(Arrays.asList(shellAction))
	}
	
	BatchShell(List<ShellAction> shellActions){
		this.shellActions = shellActions
	}
	
	
    
    void run(){

		println("\n======================================================================================")
		println(" ____  ____ \n|    \\|  _ \\\n| | | | | | |\n|_|_|_|_| |_|")
		println("\n======================================================================================\n")
        //Don't check hosts
        final Properties config = new Properties()
        config.put("StrictHostKeyChecking", "no")

        final JSch jsch = new JSch()
		//Perform each com.marcnuri.batch.ShellAction in a different session
		for(ShellAction shellAction : shellActions){
			final Session s = jsch.getSession(shellAction.getUser(), shellAction.getHost())
			s.setConfig(config)
			s.setPassword(shellAction.getPassword())
			s.connect(10000)
			println("Session connected [$shellAction.host]")
			//Perform each command for each shellAction in the same session
			for(Object c : shellAction.getCommands()){
				final String command
				final boolean isSudo
				if(c instanceof ShellActionCommand) {
					final ShellActionCommand sac = (ShellActionCommand)c
					isSudo = sac.getSudo()
					command = sac.getCommand()
				} else {
					isSudo = true
					command = c.toString()
				}
				
				final int status
				if(isSudo){
					status = sudo(s, shellAction.getPassword(), shellAction.getSudoUser(), command)
				} else {
					status = runCommand(s, command)
				}
				
				if(status != 0){
					println("Command $c exited with status code $status")
					break
				}
			}
			s.disconnect()
			println("Disconnected [$shellAction.host]")
		}
		
    }
    
    private static int sudo(Session s, String password, String user, String command){
        return runCommand(s, "sudo -S -p '' -u $user $command", { 
                ch, out ->
                out.write(("$password\n").getBytes())
                out.flush()
        })
    }
    
    /**
    */
    private static int runCommand(Session s,  String command, def cl = null){
        final ChannelExec ch = (ChannelExec)s.openChannel("exec")
        //ch.setEnv("password", password)
        ch.setCommand(command)
        ch.setInputStream(null)
        //ch.setOutputStream(System.out)
        ch.setErrStream(System.err)

        final InputStream is = ch.getInputStream()
        final OutputStream os =ch.getOutputStream()
        final BufferedReader br  = new BufferedReader(new InputStreamReader(is))
        String line = null

        ch.connect(10000)
		println("======================================================================================")
        println(">$command")
        //Run closure
        if(cl!= null){
            cl(ch, os)
        }
        while(true){
            while((line = br.readLine())!= null){
                println(line)
            }
            if(ch.isClosed()){
                println("Channel closed: ${ch.getExitStatus()}")
                break
            }
            try{Thread.sleep(1000)}catch(Exception ignore){}
        }
        final int ret = ch.getExitStatus()
        os.close()
        br.close()
        is.close()
        ch.disconnect()
        return ret
    }
}