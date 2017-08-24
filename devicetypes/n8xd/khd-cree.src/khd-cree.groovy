/**
 *  Cree Bulb with graph % on/off display
 *
 *  Copyright 2014 SmartThings
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
 * 04/07/2016 Keith (n8xd) modified to include on/off % graph using polling
 */
 
metadata {
	definition (name: "KHD Cree", namespace: "n8xd", author: "n8xd") {

		capability "Actuator"
   		capability "Configuration"
		capability "Refresh"
		capability "Switch"
		capability "Switch Level"
    	capability "Polling"  /// KHD add polling to count time on/off
        
    // KHD added tile routines
        command "genGraph"
        command "clearStats"

		fingerprint profileId: "C05E", inClusters: "0000,0003,0004,0005,0006,0008,1000", outClusters: "0000,0019"
	}

	// simulator metadata
	simulator {
		// status messages
		status "on": "on/off: 1"
		status "off": "on/off: 0"

		// reply messages
		reply "zcl on-off on": "on/off: 1"
		reply "zcl on-off off": "on/off: 0"
	}

	// UI tile definitions
    tiles(scale: 2) {
    
        // KHD Insert pieChart, regenerate graph, and clear stats tiles
        carouselTile("pieChart", "device.image", width: 6, height: 4) { }
        standardTile("genGraph", "device.switch", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "default", label:"Fresh Chart", action:"genGraph", icon:"https://raw.githubusercontent.com/n8xd/icons/master/chartlinegraph.png"
        }
        standardTile("clearStats", "device.switch", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "default", label:"Clear Data", action:"clearStats", icon: "https://raw.githubusercontent.com/n8xd/icons/master/chartlineempty.png"
        }
        
        multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true){
            tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
                attributeState "on", label:'${name}', action:"switch.off", icon:"st.switches.light.on", backgroundColor:"#79b821", nextState:"turningOff"
                attributeState "off", label:'${name}', action:"switch.on", icon:"st.switches.light.off", backgroundColor:"#ffffff", nextState:"turningOn"
                attributeState "turningOn", label:'${name}', action:"switch.off", icon:"st.switches.light.on", backgroundColor:"#79b821", nextState:"turningOff"
                attributeState "turningOff", label:'${name}', action:"switch.on", icon:"st.switches.light.off", backgroundColor:"#ffffff", nextState:"turningOn"
            }
            tileAttribute ("device.level", key: "SLIDER_CONTROL") {
                attributeState "level", action:"switch level.setLevel"
            }
        }
        standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
        }

        main "switch"
        details(["switch", "pieChart", "refresh", "clearStats", "genGraph"   ])  //KHD added new tiles
    }
}

// Parse incoming device messages to generate events
def parse(String description) {
    log.debug "description is $description"

    def resultMap = zigbee.getKnownDescription(description)
    if (resultMap) {
        log.info resultMap
        if (resultMap.type == "update") {
            log.info "$device updates: ${resultMap.value}"
        }
        else {
            sendEvent(name: resultMap.type, value: resultMap.value)
        }
    }
    else {
        log.warn "DID NOT PARSE MESSAGE for description : $description"
        log.debug zigbee.parseDescriptionAsMap(description)
    }
}

def off() {
    zigbee.off()
}

def on() {
    zigbee.on()
}

def setLevel(value) {
    zigbee.setLevel(value)
}

def refresh() {
    zigbee.onOffRefresh() + zigbee.levelRefresh() + zigbee.onOffConfig() + zigbee.levelConfig()
}

def configure() {
    log.debug "Configuring Reporting and Bindings."
    zigbee.onOffConfig() + zigbee.levelConfig() + zigbee.onOffRefresh() + zigbee.levelRefresh()
}


// KHD Polling for tracking... using pollster get 1 tick on or off for the calculations as often as you poll
//   1 tick per minute gives 1440 datapoints a day.

def poll() {
   if (state.oncount == null) {  clearStats() }
   def abc = device.latestValue("switch")
   if (abc == "on") {state.oncount = state.oncount + 1 } else { state.offcount = state.offcount + 1 }
   log.debug "Polled on=${state.oncount} off=${state.offcount}"
   // gengraph()
}

def clearStats() {   // don't use zero (divide by zero errors)
     state.oncount = 1
     state.offcount = 1
     log.debug "Cleared stats"
     genGraph()    // create an empty graph if they clear data
}

// KHD Generate the graph using google's deprecated raster image graph creator.
def genGraph()
{
       def tottime = state.oncount + state.offcount
       def onp = ((state.oncount / tottime) * 100 + 0.5 ) as int
       def offp = ((state.offcount / tottime) * 100 + 0.5) as int
       def podParams = [
          uri: "https://chart.googleapis.com",
          path: "/chart",
          query: [cht: "p3", chd: "t:${onp},${offp}", chs: "400x150", chl: "On ${onp}%|Off ${offp}%", chof: "gif"],
          contentType: 'image/gif'
        ]
        httpGet(podParams) { resp ->
            log.debug resp.data
            saveImage(resp.data)
        }
        log.debug "Created new graph"

}

//  KHD Inserted image code snipped from somewhere

// Save the image to the S3 store to display
def saveImage(image) {
    log.trace "Saving image to S3"

    // Send the image to an App who wants to consume it via an event as a Base64 String
    def bytes = image.buf
    //log.debug "JPEG Data Size: ${bytes.size()}"
    String str = bytes.encodeBase64()
    sendEvent(name: "imageDataJpeg", value: str, displayed: false, isStateChange: true)

    // Now save it to the S3 cloud, do this in the end since it removes the data from the object leaving it empty
    storeImage(getPictureName(), image)

    return null
}

private getPictureName() {
    def pictureUuid = java.util.UUID.randomUUID().toString().replaceAll('-', '')
    "image" + "_$pictureUuid" + ".jpg"
}
// KHD end snipped code