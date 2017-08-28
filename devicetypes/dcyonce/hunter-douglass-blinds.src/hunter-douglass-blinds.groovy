/**
 *  Hunter Douglas Blinds
 *
 *  Copyright 2016 Paul Mullen
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




metadata {
	definition (name: "Hunter Douglass Blinds", namespace: "dcyonce", author: "Paul Mullen", oauth: true) {
		capability "Window Shade"
        
        attribute "ipAddress", "string"
		attribute "openSceneID", "string"
		attribute "closedSceneID", "string"
		attribute "tiltSceneID", "string"
        

	}
    
    preferences {
		input "internal_ip", "text", title: "Internal IP", required: true
		input "internal_port", "text", title: "Internal Port", required: true
		input "internal_open", "text", title: "Internal Open Scene #", required: true
		input "internal_closed", "text", title: "Internal Tilted Scene #", required: true
     	input "internal_tilted", "text", title: "Internal Closed Scene #", required: true
}


	simulator {
		// TODO: define status and reply messages here
	}

	tiles {
		standardTile("scene1Switch", "device.switch", width: 1, height: 1, canChangeIcon: false) {
			state "nc", label: 'Open', action: "open", backgroundColor: "#ffffff"
		}
		standardTile("scene2Switch", "device.switch", width: 1, height: 1, canChangeIcon: false) {
			state "nc", label: 'Close', action: "close", backgroundColor: "#ffffff"
		}
		standardTile("scene3Switch", "device.switch", width: 1, height: 1, canChangeIcon: false) {
			state "nc", label: 'Tilt', action: "presetPosition", backgroundColor: "#ffffff"
		}
        valueTile("Status", "device.status", width: 3, height: 1) {
            state "status", label: '${currentValue}'
        }

		details(["scene1Switch","scene2Switch","scene3Switch","Status"])
        main ("Status")

	}
}



// handle commands
def open() {

	log.debug "Executing 'open'"
    

    try {

		def result = new physicalgraph.device.HubAction(
				method: "GET",
				path: "/api/scenes?sceneid=${internal_open}",
                headers: [
                HOST: "${internal_ip}:${internal_port}"
				]
				)
			sendHubCommand(result)
	 }   catch (e) {
        log.debug e
     }
     sendEvent(name: "status", value: "Open")
     

}

def close() {

	log.debug "Executing 'close'"

    try {

		def result = new physicalgraph.device.HubAction(
				method: "GET",
				path: "/api/scenes?sceneid=${internal_closed}",
                headers: [
                HOST: "${internal_ip}:${internal_port}"
				]
				)
			sendHubCommand(result)
	 }   catch (e) {
        log.debug e
     }
     sendEvent(name: "status", value: "Closed")


    
}

def presetPosition() {

	log.debug "Executing 'presetPosition'"
    
    try {

		def result = new physicalgraph.device.HubAction(
				method: "GET",
				path: "/api/scenes?sceneid=${internal_tilted}",
                headers: [
                HOST: "${internal_ip}:${internal_port}"
				]
				)
			sendHubCommand(result)
	 }   catch (e) {
        log.debug e
     }
     
     sendEvent(name: "status", value: "Tilted")

     
}