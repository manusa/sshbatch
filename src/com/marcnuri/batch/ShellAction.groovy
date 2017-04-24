#!/usr/bin/env groovy
package com.marcnuri.batch

class ShellAction {

    private final String host
    private final String user
    private final String password
    private final String sudoUser
	private final Object[] commands
	
	ShellAction(String host, String user, String password, String sudoUser, Object... commands){
		this.host = host
		this.user = user
		this.password = password
		this.sudoUser = sudoUser
		this.commands = commands
	}
	
	String getHost(){
		return host
	}
	
	String getUser(){
		return user
	}
	
	String getPassword(){
		return password
	}
	
	String getSudoUser(){
		return sudoUser
	}
	
	Object[] getCommands(){
		return commands
	}
	
}