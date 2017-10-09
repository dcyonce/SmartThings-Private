/**
 *  Raspberry Pi
 *
 *  Copyright 2014 Nicholas Wilde
 *
 *  Monitor your Raspberry Pi using SmartThings and WebIOPi <https://code.google.com/p/webiopi/>
 *
 *  Companion WebIOPi python script can be found here:
 *  <https://github.com/nicholaswilde/smartthings/blob/master/device-types/raspberry-pi/raspberrypi.py>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
 
import groovy.json.JsonSlurper

preferences {
        input("ip", "string", title:"IP Address", description: "192.168.1.150", required: true, displayDuringSetup: true)
        input("port", "string", title:"Port", description: "8000", defaultValue: 8000 , required: true, displayDuringSetup: true)
}

metadata {
	definition (name: "Raspberry Pi", namespace: "dcyonce", author: "Don Yonce") {
		capability "Polling"
		capability "Refresh"
		capability "Temperature Measurement"
        capability "Switch"
        capability "Sensor"
        capability "Actuator"
        
        attribute "cpuPercentage", "string"
        attribute "memory", "string"
        attribute "diskUsage", "string"
        attribute "systemState", "string"
        
        command "restart"
		command "zoneOn"
		command "stop"
		command "unlock"        
	}

	simulator {
		// TODO: define status and reply messages here
	}

	tiles {
		valueTile("systemState", "device.systemState", width: 1, height: 1) {
            state "systemState", label:'${currentValue}'
        }
        standardTile("button", "device.switch", width: 1, height: 1, canChangeIcon: true) {
			state "down", label: 'DOWN', icon: "st.Electronics.electronics18", backgroundColor: "#FF0000", nextState: "active"
			state "active", label: 'Active', icon: "st.Electronics.electronics18", backgroundColor: "#79b821", nextState: "down"
		}
		standardTile("water", "device.switch2") {
			state "off2", label: '${name}', icon: "st.Electronics.electronics18", backgroundColor: "#ffffff",  action: "zoneOn"
			state "on2",  label: '${name}', icon: "st.Electronics.electronics18", backgroundColor: "#79b821", action: "stop"  
        }                   
        valueTile("cpuPercentage", "device.cpuPercentage", inactiveLabel: false) {
        	state "default", label:'${currentValue}', unit:"Percentage",
            backgroundColors:[
                [value: 31, color: "#153591"],
                [value: 44, color: "#1e9cbb"],
                [value: 59, color: "#90d2a7"],
                [value: 74, color: "#44b621"],
                [value: 84, color: "#f1d801"],
                [value: 95, color: "#d04e00"],
                [value: 96, color: "#bc2323"]
            ]
        }
        valueTile("memory", "device.memory", width: 1, height: 1) {
        	state "default", label:'${currentValue} MB', unit:"MB",
            backgroundColors:[
                [value: 353, color: "#153591"],
                [value: 287, color: "#1e9cbb"],
                [value: 210, color: "#90d2a7"],
                [value: 133, color: "#44b621"],
                [value: 82, color: "#f1d801"],
                [value: 26, color: "#d04e00"],
                [value: 20, color: "#bc2323"]
            ]
        }
        valueTile("diskUsage", "device.diskUsage", width: 1, height: 1) {
        	state "default", label:'${currentValue}% Disk', unit:"Percent",
            backgroundColors:[
                [value: 31, color: "#153591"],
                [value: 44, color: "#1e9cbb"],
                [value: 59, color: "#90d2a7"],
                [value: 74, color: "#44b621"],
                [value: 84, color: "#f1d801"],
                [value: 95, color: "#d04e00"],
                [value: 96, color: "#bc2323"]
            ]
        }
        standardTile("restart", "device.restart", inactiveLabel: false, decoration: "flat") {
        	state "default", action:"restart", label: "Restart", displayName: "Restart"
        }
        standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat") {
        	state "default", action:"refresh.refresh", icon: "st.secondary.refresh"
        }
        main "button"
        details(["button", "water", "systemState", "cpuPercentage", "memory" , "diskUsage", "restart", "refresh"])
    }
}

// ------------------------------------------------------------------

// parse events into attributes
def parse(String description) {
    def map = [:]
    def descMap = parseDescriptionAsMap(description)
    //log.debug descMap
    def body = new String(descMap["body"].decodeBase64())
    //log.debug "body: ${body}"
    def slurper = new JsonSlurper()
    def result = slurper.parseText(body)
    log.debug "result: ${result}"
	if (result){
    	log.debug "Computer is up"
   		sendEvent(name: "switch", value: "active")
    }
    else
	{
   		sendEvent(name: "switch", value: "down")
    }
    
    if (result.containsKey("Status")) {
    	log.debug( "Status: ${result.Status.SystemState}")
    	sendEvent(name: "systemState", value: result.Status.SystemState)
        if (result.Status.SystemState=="Idle")
        {
        	sendEvent(name: "switch2", value: "off2")
        }
        else
        {
        	sendEvent(name: "switch2", value: "on2")
        }

    	if (result.Status.ActiveSchedule) {
    	    sendEvent(name: "cpuPercentage", value: result.Status.ActiveSchedule.ActiveZone.ZoneID)
	    }
        else
        {
    	    sendEvent(name: "cpuPercentage", value: "")
        }
    }
    
    
    if (result.containsKey("mem_avail")) {
    	log.debug "mem_avail: ${result.mem_avail}"
        sendEvent(name: "memory", value: result.mem_avail)
    }
    if (result.containsKey("disk_usage")) {
    	log.debug "disk_usage: ${result.disk_usage}"
        sendEvent(name: "diskUsage", value: result.disk_usage)
    }
  
}

// handle commands
def poll() {
	log.debug "Executing 'poll'"
    sendEvent(name: "switch", value: "off")
    getRPiData()
}

def refresh() {
	log.debug "Executing 'refresh'"
    getRPiData()
}

def unlock()
{
    def uri = "/api.asmx/Doors/1/unlock?Format=JSON"
    getAction(uri)
}

def zoneOn()
{
    def uri = "/api/Zone/B2/start?Format=JSON"
    getAction(uri)
}

def stop()
{
    def uri = "/api/stop?Format=JSON"
    getAction(uri)
}

def restart(){
	log.debug "Restart was pressed"
    sendEvent(name: "switch", value: "off")
    def uri = "/macros/reboot"
    postAction(uri)
}

// Get CPU percentage reading
private getRPiData() {
	def uri = "/api/status?Format=JSON"
    getAction(uri)
}

// ------------------------------------------------------------------

private getAction(uri){
  setDeviceNetworkId(ip,port)  
   
  //uri += "?Format=JSON"
  def hubAction = new physicalgraph.device.HubAction(
    method: "GET",
    path: uri,
	headers: [HOST: "10.10.0.103:80"]    
 )
 //,delayAction(1000), refresh()]
  log.debug("Executing hubAction on " + getHostAddress())
  log.debug hubAction
  hubAction    
}

private postAction(uri){
  setDeviceNetworkId(ip,port)  
  
  def userpass = encodeCredentials(username, password)
  
  def headers = getHeader(userpass)
  
  def hubAction = new physicalgraph.device.HubAction(
    method: "POST",
    path: uri,
    headers: headers
  )//,delayAction(1000), refresh()]
  log.debug("Executing hubAction on " + getHostAddress())
  //log.debug hubAction
  hubAction    
}

// ------------------------------------------------------------------
// Helper methods
// ------------------------------------------------------------------

def parseDescriptionAsMap(description) {
	description.split(",").inject([:]) { map, param ->
		def nameAndValue = param.split(":")
		map += [(nameAndValue[0].trim()):nameAndValue[1].trim()]
	}
}

private encodeCredentials(username, password){
	log.debug "Encoding credentials"
	def userpassascii = "${username}:${password}"
    def userpass = "Basic " + userpassascii.encodeAsBase64().toString()
    //log.debug "ASCII credentials are ${userpassascii}"
    //log.debug "Credentials are ${userpass}"
    return userpass
}

private getHeader(userpass){
	log.debug "Getting headers"
    def headers = [:]
    headers.put("HOST", getHostAddress())
    headers.put("Authorization", userpass)
    //log.debug "Headers are ${headers}"
    return headers
}

private delayAction(long time) {
	new physicalgraph.device.HubAction("delay $time")
}

private setDeviceNetworkId(ip,port){
  	def iphex = convertIPtoHex(ip)
  	def porthex = convertPortToHex(port)
  	device.deviceNetworkId = "$iphex:$porthex"
  	log.debug "Device Network Id set to ${iphex}:${porthex}"
}

private getHostAddress() {
	return "${ip}:${port}"
}

private String convertIPtoHex(ipAddress) { 
    String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
    return hex

}

private String convertPortToHex(port) {
	String hexport = port.toString().format( '%04x', port.toInteger() )
    return hexport
}