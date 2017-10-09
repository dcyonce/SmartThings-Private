/**
 *
 */

// Preferences
preferences {
    input "ip", "text", title: "Arduino IP Address", description: "ip", required: true, displayDuringSetup: true
    input "port", "text", title: "Arduino Port", description: "port", required: true, displayDuringSetup: true
}
    
metadata {
	definition (name: "Door Controller", namespace: "dcyonce", author: "Don Yonce") {
		capability "Actuator"
		capability "Switch"
		capability "Sensor"
	}

	// Simulator metadata
	simulator {

	}
    

	// UI tile definitions
	tiles {
		standardTile("switch", "device.switch", width: 2, height: 2, canChangeIcon: true, canChangeBackground: true) {
			state "on", label: '${name}', action: "switch.off", icon: "st.switches.switch.on", backgroundColor: "#79b821"
			state "off", label: '${name}', action: "switch.on", icon: "st.switches.switch.off", backgroundColor: "#ffffff"
		}
		standardTile("configure", "device.configure", inactiveLabel: false, decoration: "flat") {
			state "configure", label:'', action:"configuration.configure", icon:"st.secondary.configure"
		}

		main (["switch"])
		details (["switch","configure"])
	}
}

// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"
	def msg = parseLanMessage(description)
	def headerString = msg.header
    
    def json = msg.xml
    def xml = msg.xml

	log.debug "JSON= ${json}"
	log.debug "XML= ${xml}"

	if (!headerString) {
		log.debug "headerstring was null for some reason :("
    }

	def bodyString = msg.body

	if (bodyString) {
        log.debug "BodyString: $bodyString"
        def value = bodyString
	    def name = value in ["on","off"] ? "switch" : null
	    def result = createEvent(name: name, value: value)
	    log.debug "Parse returned ${result?.descriptionText}"
	    return result
	}
}

private getHostAddress() {
    def ip = settings.ip
    def port = settings.port

	log.debug "Using ip: ${ip} and port: ${port} for device: ${device.id}"
    return ip + ":" + port
}

def sendEthernet(message) {
	def result
	log.debug "Executing 'sendEthernet' ${message}"
	//new physicalgraph.device.HubAction(
    //	method: "POST",
    //	path: "/${message}?",
    //	headers: [ HOST: "${getHostAddress()}" ]
	//)
	result = sendHubCommand(
    	new physicalgraph.device.HubAction(
        	"""GET ${uri} HTTP/1.1\r\nHOST: ${settings.ip}:${settings.port}\r\n\r\n""",
            physicalgraph.device.Protocol.LAN,
            "${deviceNetworkId}")
            )    
    log.debug "result: ${result}"
}

def getAction(url) 
{
  setDeviceNetworkId(settings.ip,settings.port)  
  //uri += "?Format=JSON"
  def hubAction = new physicalgraph.device.HubAction(
    method: "GET",
    path: url
 )
 //,delayAction(1000), refresh()]
  log.debug("Executing hubAction on " + getHostAddress())
  log.debug hubAction
  hubAction 
}

def unlock()
{
	def result
	log.debug "Executing 'UnLock Door #1'"
	// result = new physicalgraph.device.HubAction(
    //	method: "GET",
    //	path: "/api.asmx/Doors/1/UnLock",
    //    headers: [ HOST: "${getHostAddress()}", Data: "1234" ]
    //)
    //return result
    getAction("/api.asmx/Doors/1/UnLock?Format=JSON")
}
def lock()
{
	def result
	log.debug "Executing 'Lock Door #1'"
	result = new physicalgraph.device.HubAction(
    	method: "GET",
    	path: "/api.asmx/Doors/1/Lock",
		headers: [ HOST: "${getHostAddress()}", Data: "1234" ]    
    )
	return result
}



// Commands sent to the device
def on() {
	lock()
}

def off() {
	unlock()
}

def configure() {
	log.debug "Executing 'configure'"
    if(device.deviceNetworkId!=settings.mac) {
    	log.debug "setting device network id"
    	device.deviceNetworkId = settings.mac
    }
}

private setDeviceNetworkId(ip,port){
  	def iphex = convertIPtoHex(ip)
  	def porthex = convertPortToHex(port)
  	device.deviceNetworkId = "$iphex:$porthex"
  	log.debug "Device Network Id set to ${iphex}:${porthex}"
}

private String convertIPtoHex(ipAddress) { 
    String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
    return hex

}

private String convertPortToHex(port) {
	String hexport = port.toString().format( '%04x', port.toInteger() )
    return hexport
}