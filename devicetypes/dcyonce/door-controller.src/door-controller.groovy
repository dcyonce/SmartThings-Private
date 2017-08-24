/**
 *  Atlantis Development - Door Controller
 *
 *  Copyright 2017 Atlantis Development
 *	Don Yonce
 */
 
import groovy.json.JsonSlurper

preferences {
        input("ip", "string", title:"IP Address", description: "192.168.1.150", required: true, displayDuringSetup: true)
        input("port", "string", title:"Port", description: "80", defaultValue: 80 , required: true, displayDuringSetup: true)
}

metadata {
	definition (name: "Door Controller", namespace: "dcyonce", author: "Don Yonce") {
		capability "Polling"
		capability "Refresh"
        capability "Switch"
        capability "Sensor"
        capability "Actuator"
        capability "Door Control"
        
        attribute "systemName", "string"
        
		command "lock"
		command "unlock"
	}

	simulator {
		// TODO: define status and reply messages here
	}

	tiles {
		valueTile("systemName", "device.systemName", width: 2, height: 1) {
            state "systemName", label:'${currentValue}'
        }
        standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat") {
        	state "default", action:"refresh.refresh", icon: "st.secondary.refresh"
        }
        main "button"
        details(["button", "systemName"])
    }
}

// ------------------------------------------------------------------

// parse events into attributes
def parse(String description) {
    log.debug "description: ${description}"
    def map = [:]
    def descMap = parseDescriptionAsMap(description)
    def body = new String(descMap["body"].decodeBase64())
    def slurper = new JsonSlurper()
    def result = slurper.parseText(body)
    log.debug "result: ${result}"
	if (result){
    	log.debug "Controller is up"
   		sendEvent(name: "systemName", value: results.Computer.MachineName)
    }
    else
	{
   		sendEvent(name: "systemName", value: "unknown")
    }  
}

// handle commands
def poll() {
	log.debug "Executing 'poll'"
    getStatus()
}

def refresh() {
	log.debug "Executing 'refresh'"
    getStatus()
}

private getStatus() {
	def uri = "/WebServer/Stats?Format=JSON"
    getAction(uri)
}

// ------------------------------------------------------------------

private getAction(uri){
  //setDeviceNetworkId(ip,port)  
   
  //uri += "?Format=JSON"
  def hubAction = new physicalgraph.device.HubAction(
    method: "GET",
    path: uri
 )
  log.debug("Executing hubAction on " + getHostAddress())
  log.debug hubAction
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