#!/usr/bin/env groovy
package com.marcnuri.batch

class ShellActionCommand {

	private final boolean sudo
	private final String command
	
	ShellActionCommand(boolean sudo, String command){
		this.sudo = sudo
		this.command = command
	}
	
	final boolean getSudo(){
		return sudo
	}
	
	final String getCommand(){
		return command
	}
}