/**
 *  Copyright 2015 SmartThings
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
 *  Notes - Edited and modifed from Z-Wave Secure Switch for a bare basic use of Aeotec Nano Shutter.
 */

metadata {
    definition(name: "Aeotec Inc (ZW141) Nano Shutter -2", namespace: "Aeotec", author: "Chris Cheng", ocfDeviceType: "oic.d.blind", mnmn: "SmartThings", vid: "generic-shade") {
        capability "Switch"
        capability "Configuration"
        capability "Health Check"
        //capability "Switch Level"
        capability "Refresh"
        capability "Window Shade"
        capability "Window Shade Preset"

        command "stop"

        fingerprint mfr: "0086", prod: "0103", model: "008D"
    }

    tiles {
        multiAttributeTile(name: "windowShade", type: "lighting", width: 6, height: 4) {
            tileAttribute("device.windowShade", key: "PRIMARY_CONTROL") {
                attributeState "unknown", label: '${name}', action: "calibrate", icon: "st.shades.shade-closed", backgroundColor: "#ffffff"
                attributeState "closed", label: '${name}', action: "open", icon: "st.shades.shade-closed", backgroundColor: "#ffffff", nextState: "opening"
                attributeState "open", label: '${name}', action: "close", icon: "st.shades.shade-open", backgroundColor: "#00a0dc", nextState: "closing"
                attributeState "opening", label: '${name}', action: "stop", icon: "st.shades.shade-opening", backgroundColor: "#00a0dc", nextState: "partially open"
                attributeState "closing", label: '${name}', action: "stop", icon: "st.shades.shade-closing", backgroundColor: "#00a0dc", nextState: "partially open"
                attributeState "partially open", label: '${name}', action: "close", icon: "st.shades.shade-open", backgroundColor: "#00a0dc", nextState: "closing"
            }
        }
        valueTile("open", "device.open", decoration: "flat", width: 2, height: 2) {
            state "open", label: 'Open', action: "open", icon: "st.shades.shade-opening"
        }
        valueTile("close", "device.close", decoration: "flat", width: 2, height: 2) {
            state "close", label: 'Close', action: "close", icon: "st.shades.shade-closing"
        }
        valueTile("stop", "device.stop", decoration: "flat", width: 2, height: 2) {
            state "stop", label: 'Stop', action: "stop", icon: "st.Home.home30"
        }
        standardTile("refresh", "device.refresh", decoration: "flat", width: 2, height: 2) {
            state "refresh", label: 'Refresh', action: "refresh", icon: "st.secondary.refresh-icon"
        }
        standardTile("configure", "device.needUpdate", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "NO", label: '', action: "configuration.configure", icon: "st.secondary.configure"
            state "YES", label: '', action: "configuration.configure", icon: "https://github.com/erocm123/SmartThingsPublic/raw/master/devicetypes/erocm123/qubino-flush-1d-relay.src/configure@2x.png"
        }

        main "windowShade"
        details(["windowShade", "open", "close", "stop", "configure", "refresh"])
    }

    preferences {
        input description: "Once you change values on this page, the corner of the \"configuration\" icon will change orange until all configuration parameters are updated.", title: "Settings", displayDuringSetup: false, type: "paragraph", element: "paragraph"
        generate_preferences(configuration_model())
    }
}

def installed() {
    log.debug("installed()")
    // Device-Watch simply pings if no device events received for checkInterval duration of 32min = 2 * 15min + 2min lag time
    sendEvent(name: "checkInterval", value: 1920, displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID, offlinePingable: "1"])
}

def updated() {
    log.debug("updated()")
    def cmds = update_needed_settings()
    sendEvent(name: "checkInterval", value: 2 * 15 * 60 + 2 * 60, displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID])
    sendEvent(name: "needUpdate", value: device.currentValue("needUpdate"), displayed: false, isStateChange: true)
    if (cmds != []) response(commands(cmds))
//    response(refresh())
}

def parse(description) {
    def result = null
    if (description.startsWith("Err 106")) {
        result = createEvent(descriptionText: description, isStateChange: true)
    } else if (description != "updated") {
        def cmd = zwave.parse(description, cmdVersions())
        if (cmd) {
            result = zwaveEvent(cmd)
            log.debug("'$description' parsed to $result")
        } else {
            log.debug("Couldn't zwave.parse '$description'")
        }
    }
    result
}

private List handleStateChange(physicalgraph.zwave.Command cmd) {
    def result = []
    def descriptionText = null
    def shadeValue
    def level = cmd.value as Integer
    if (level == 99) {
        level = 100
        shadeValue = "open"
    } else if (level == 0) {
        level = 0  // unlike dimmer switches, the level isn't saved when closed
        shadeValue = "closed"
    } else {
        shadeValue = "partially open"
        descriptionText = "${device.displayName} shade is ${level}% open"
    }
    result << createEvent(name: "windowShade", value: shadeValue, isStateChange: true)
    if (cmd.value > 99) {
        result << createEvent(name: "level", value: cmd.value, unit: "%", descriptionText: "${device.displayName} is uncalibrated! Please press calibrate!", isStateChange: true)
    } else {
        result << createEvent(name: "level", value: level, unit: "%", descriptionText: descriptionText, isStateChange: true)
    }
    log.debug "handleStateChange(windowShade:${shadeValue} :: level:${level})"
    return result
}
/*
private handleStateChange(physicalgraph.zwave.Command cmd) {
    log.debug "handleStateChange(value:${cmd.value})"
    def shadeValue = cmd.value > 0 ? "open" : "closed"
    return createEvent(name: "windowShade", value: shadeValue, isStateChange: true)
}
*/
def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd) {
    log.debug "BasicReport(value:${cmd.value})"
    return handleStateChange(cmd)
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicSet cmd) {
    log.debug "BasicSet(value:${cmd.value})"
    return handleStateChange(cmd)
}

def zwaveEvent(physicalgraph.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd) {
    log.debug "BinaryReport(value:${cmd.value})"
    return handleStateChange(cmd)
}

def zwaveEvent(physicalgraph.zwave.commands.switchmultilevelv3.SwitchMultilevelReport cmd) {
    log.debug "SwitchMultilevelReport(value:${cmd.value})"
    return handleStateChange(cmd)
}

def zwaveEvent(physicalgraph.zwave.commands.securityv1.SecurityMessageEncapsulation cmd) {
    def encapsulatedCommand = cmd.encapsulatedCommand(cmdVersions())
    if (encapsulatedCommand) {
        zwaveEvent(encapsulatedCommand)
    }
}

def zwaveEvent(physicalgraph.zwave.Command cmd) {
    log.debug "Unhandled: $cmd"
    null
}

def open() {
    setLevel(99)
}

def close() {
    setLevel(00)
}

def presetPosition() {
    setLevel(preset ?: state.preset ?: 99)
}

def setLevel(value, duration = null) {
    log.debug "${device.displayName} - Executing setLevel(${value.inspect()})"
    Integer level = value as Integer
    if (level == 0) {
        sendEvent(name: "windowShade", value: "closing")
    }
    if (level > 0) {
        sendEvent(name: "windowShade", value: "opening")
    }

    commands([
            zwave.basicV1.basicSet(value: level),
            zwave.basicV1.basicGet()
    ], 5000)
    return level
}

def on() {
    open()
}

def off() {
    close()
}

def stop() {
    commands([
            zwave.switchMultilevelV3.switchMultilevelStopLevelChange(),
            zwave.switchMultilevelV3.switchMultilevelGet()
    ])
}

def ping(){
    refresh()
}

def refresh() {
    log.debug "refresh"
    commands([
            zwave.basicV1.basicGet()
    ])
}

private Map cmdVersions() {
    [0x55: 1, 0x98: 1, 0x85: 2, 0x59: 1, 0x70: 2, 0x2C: 1, 0x2B: 1, 0x25: 1, 0x26: 3, 0x73: 1, 0x7A: 2, 0x86: 1, 0x72: 2, 0x5A: 1]
}

private command(physicalgraph.zwave.Command cmd) {
    if ((zwaveInfo.zw == null && state.sec != 0) || zwaveInfo?.zw?.contains("s")) {
        zwave.securityV1.securityMessageEncapsulation().encapsulate(cmd).format()
    } else {
        cmd.format()
    }
}

private commands(commands, delay = 200) {
    delayBetween(commands.collect { command(it) }, delay)
}

def zwaveEvent(physicalgraph.zwave.commands.configurationv2.ConfigurationReport cmd) {
    update_current_properties(cmd)
    logging("${device.displayName} parameter '${cmd.parameterNumber}' with a byte size of '${cmd.size}' is set to '${cmd2Integer(cmd.configurationValue)}'", 2)
}

def configure() {
    logging("configure()", 1)
    def cmds = []
    cmds = update_needed_settings()
    if (cmds != []) commands(cmds)
}

def update_current_properties(cmd) {
    def currentProperties = state.currentProperties ?: [:]

    currentProperties."${cmd.parameterNumber}" = cmd.configurationValue

    def parameterSettings = parseXml(configuration_model()).Value.find { it.@index == "${cmd.parameterNumber}" }

    if (settings."${cmd.parameterNumber}" != null || parameterSettings.@hidden == "true") {
        if (convertParam(cmd.parameterNumber, parameterSettings.@hidden != "true" ? settings."${cmd.parameterNumber}" : parameterSettings.@value) == cmd2Integer(cmd.configurationValue)) {
            sendEvent(name: "needUpdate", value: "NO", displayed: false, isStateChange: true)
        } else {
            sendEvent(name: "needUpdate", value: "YES", displayed: false, isStateChange: true)
        }
    }

    state.currentProperties = currentProperties
}

def update_needed_settings() {
    def cmds = []
    def currentProperties = state.currentProperties ?: [:]

    def configuration = parseXml(configuration_model())
    def isUpdateNeeded = "NO"

    //cmds << zwave.multiChannelAssociationV2.multiChannelAssociationRemove(groupingIdentifier: 1, nodeId: [0,zwaveHubNodeId,1])
    //cmds << zwave.multiChannelAssociationV2.multiChannelAssociationGet(groupingIdentifier: 1)
    cmds << zwave.associationV2.associationSet(groupingIdentifier: 1, nodeId: zwaveHubNodeId)
    cmds << zwave.associationV2.associationGet(groupingIdentifier: 1)

    configuration.Value.each {
        if ("${it.@setting_type}" == "zwave" && it.@disabled != "true") {
            if (currentProperties."${it.@index}" == null) {
                if (it.@setonly == "true") {
                    logging("Parameter ${it.@index} will be updated to " + convertParam(it.@index.toInteger(), settings."${it.@index}" ? settings."${it.@index}" : "${it.@value}"), 2)
                    def convertedConfigurationValue = convertParam(it.@index.toInteger(), settings."${it.@index}" ? settings."${it.@index}" : "${it.@value}")
                    cmds << zwave.configurationV1.configurationSet(configurationValue: integer2Cmd(convertedConfigurationValue, it.@byteSize.toInteger()), parameterNumber: it.@index.toInteger(), size: it.@byteSize.toInteger())
                } else {
                    isUpdateNeeded = "YES"
                    logging("Current value of parameter ${it.@index} is unknown", 2)
                    cmds << zwave.configurationV1.configurationGet(parameterNumber: it.@index.toInteger())
                }
            } else if ((settings."${it.@index}" != null || "${it.@hidden}" == "true") && cmd2Integer(currentProperties."${it.@index}") != convertParam(it.@index.toInteger(), "${it.@hidden}" != "true" ? settings."${it.@index}" : "${it.@value}")) {
                isUpdateNeeded = "YES"
                logging("Parameter ${it.@index} will be updated to " + convertParam(it.@index.toInteger(), "${it.@hidden}" != "true" ? settings."${it.@index}" : "${it.@value}"), 2)
                def convertedConfigurationValue = convertParam(it.@index.toInteger(), "${it.@hidden}" != "true" ? settings."${it.@index}" : "${it.@value}")
                cmds << zwave.configurationV1.configurationSet(configurationValue: integer2Cmd(convertedConfigurationValue, it.@byteSize.toInteger()), parameterNumber: it.@index.toInteger(), size: it.@byteSize.toInteger())
                cmds << zwave.configurationV1.configurationGet(parameterNumber: it.@index.toInteger())
            }
        }
    }

    sendEvent(name: "needUpdate", value: isUpdateNeeded, displayed: false, isStateChange: true)
    return cmds
}

def convertParam(number, value) {
    def parValue
    switch (number) {
        case 110:
            if (value < 0)
                parValue = value * -1 + 1000
            else
                parValue = value
            break
        default:
            parValue = value
            break
    }
    return parValue.toInteger()
}

def generate_preferences(configuration_model) {
    def configuration = parseXml(configuration_model)

    configuration.Value.each {
        if (it.@hidden != "true" && it.@disabled != "true") {
            switch (it.@type) {
                case ["number"]:
                    input "${it.@index}", "number",
                            title: "${it.@label}\n" + "${it.Help}",
                            range: "${it.@min}..${it.@max}",
                            defaultValue: "${it.@value}",
                            displayDuringSetup: "${it.@displayDuringSetup}"
                    break
                case "list":
                    def items = []
                    it.Item.each { items << ["${it.@value}": "${it.@label}"] }
                    input "${it.@index}", "enum",
                            title: "${it.@label}\n" + "${it.Help}",
                            defaultValue: "${it.@value}",
                            displayDuringSetup: "${it.@displayDuringSetup}",
                            options: items
                    break
                case "decimal":
                    input "${it.@index}", "decimal",
                            title: "${it.@label}\n" + "${it.Help}",
                            range: "${it.@min}..${it.@max}",
                            defaultValue: "${it.@value}",
                            displayDuringSetup: "${it.@displayDuringSetup}"
                    break
                case "boolean":
                    input "${it.@index}", "boolean",
                            title: "${it.@label}\n" + "${it.Help}",
                            defaultValue: "${it.@value}",
                            displayDuringSetup: "${it.@displayDuringSetup}"
                    break
            }
        }
    }
}

private def logging(message, level) {
    log.debug "$message"
    /*  if (logLevel > 0) {
          switch (logLevel) {
              case "1":
                  if (level > 1)
                      log.debug "$message"
                  break
              case "99":
                  log.debug "$message"
                  break
          }
      }
    */
}

/**
 * Convert byte values to integer
 */
def cmd2Integer(array) {
    switch (array.size()) {
        case 1:
            array[0]
            break
        case 2:
            ((array[0] & 0xFF) << 8) | (array[1] & 0xFF)
            break
        case 3:
            ((array[0] & 0xFF) << 16) | ((array[1] & 0xFF) << 8) | (array[2] & 0xFF)
            break
        case 4:
            ((array[0] & 0xFF) << 24) | ((array[1] & 0xFF) << 16) | ((array[2] & 0xFF) << 8) | (array[3] & 0xFF)
            break
    }
}

def integer2Cmd(value, size) {
    switch (size) {
        case 1:
            [value]
            break
        case 2:
            def short value1 = value & 0xFF
            def short value2 = (value >> 8) & 0xFF
            [value2, value1]
            break
        case 3:
            def short value1 = value & 0xFF
            def short value2 = (value >> 8) & 0xFF
            def short value3 = (value >> 16) & 0xFF
            [value3, value2, value1]
            break
        case 4:
            def short value1 = value & 0xFF
            def short value2 = (value >> 8) & 0xFF
            def short value3 = (value >> 16) & 0xFF
            def short value4 = (value >> 24) & 0xFF
            [value4, value3, value2, value1]
            break
    }
}

def configuration_model()
{
    '''
    <configuration>
      <Value type="decimal" index="35" label="Set Up/Down motor time." min="1" max="255" value="30" byteSize="1" setting_type="zwave" displayDuringSetup="true">
            <Help>
            Set the maximum UP or DOWN motor time. 
            Range : 1 - 255 seconds.
            </Help>
      </Value>
      <Value type="list" index="80" label="Motor Status Update." min="0" max="1" value="1" byteSize="1" setting_type="zwave" displayDuringSetup="true">
            <Help>
            Allows Nano Switch to automatically update its status to instantly update its status between OPEN or CLOSED.
            </Help>
            <Item label="Disable" value="0" />
            <Item label="Enabled" value="1" />
      </Value>
      <Value type="list" index="85" label="External Switch Operating Modes" min="0" max="1" value="0" byteSize="1" setting_type="zwave" displayDuringSetup="true">
            <Help>
            Mode 1 : Both S1 and S2 operate the same for Open/Close/Stop. Ideal for single switch use.
            Mode 2 : S1 operates Open/Stop, S2 operates Close/Stop. Ideal for 2 external switches.
            </Help>
            <Item label="Mode 1" value="0" />
            <Item label="Mode 2" value="1" />
      </Value>
      <Value type="list" index="120" label="S1 External Switch Mode." min="0" max="4" value="4" byteSize="1" setting_type="zwave" displayDuringSetup="true">
            <Help>
            Change external switch detected type connected to S1 terminal.
            If set to Re-detect, once saved, toggle your switch once.
            </Help>
            <Item label="Unidentified Mode" value="0" />
            <Item label="2-State Toggle Switch" value="1" />
            <Item label="NC Mode push-button or 3-way switch" value="2" />
            <Item label="NO Mode push-button" value="3" />
            <Item label="Re-detect" value="4" />
      </Value>
      <Value type="list" index="121" label="S2 External Switch Mode." min="0" max="4" value="4" byteSize="1" setting_type="zwave" displayDuringSetup="true">
            <Help>
            Change external switch detected type connected to S2 terminal.
            If set to Re-detect, once saved, toggle your switch once.
            </Help>
            <Item label="Unidentified Mode" value="0" />
            <Item label="2-State Toggle Switch" value="1" />
            <Item label="NC Mode push-button or 3-way switch" value="2" />
            <Item label="NO Mode push-button" value="3" />
            <Item label="Re-detect" value="4" />
      </Value>
      <Value type="list" index="248" label="External Switch S1/2 Z-Wave Controls" min="0" max="0" value="0" byteSize="1" setting_type="zwave" displayDuringSetup="true">
            <Help>
            Functions allowed by Momentary Push Button.
            NIF : allows Nano Shutter to be unpaired by SmartThings using "Generic Exclude"
            RF Power : allows Nano Shutter to initiate health test
            Factory Reset : allows Nano Shutter to initiate manual factory reset
            </Help>
            <Item label="All Disabled" value="0" />
            <Item label="NIF" value="1" />
            <Item label="RF power" value="2" />
            <Item label="NIF, RF power" value="3" />
            <Item label="Factory Reset" value="4" />
            <Item label="Factory Reset, NIF" value="5" />
            <Item label="Factory Reset, RF power" value="6" />
            <Item label="All Enabled" value="7" />
      </Value>
    </configuration>
'''
}
