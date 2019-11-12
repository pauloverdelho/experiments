import groovy.json.JsonOutput

metadata {

    definition(name: "Aeotec Nano Shutter", namespace: "Aeotec", author: "Paulo Verdelho", ocfDeviceType: "oic.d.blind", mnmn: "SmartThings") {

        capability "Window Shade"
        capability "Window Shade Preset"
        capability "Switch"         // so it can be shared with google assistant
        capability "Configuration"
        capability "Health Check"
        capability "Actuator"
        capability "Sensor"
        capability "Refresh"
        //capability "Power Meter"
        capability "Battery"

        command "stop"

        capability "Switch Level"   // until we get a Window Shade Level capability

        fingerprint mfr: "0086", prod: "0103", model: "008D"
    }

    tiles(scale: 2) {
        multiAttributeTile(name: "windowShade", type: "generic", width: 6, height: 4) {
            tileAttribute("device.windowShade", key: "PRIMARY_CONTROL") {
                attributeState "unknown", label: '${name}', action: "calibrate", icon: "st.shades.shade-closed", backgroundColor: "#ffffff"
                attributeState "closed", label: '${name}', action: "open", icon: "st.shades.shade-closed", backgroundColor: "#ffffff", nextState: "opening"
                attributeState "open", label: '${name}', action: "close", icon: "st.shades.shade-open", backgroundColor: "#00a0dc", nextState: "closing"
                attributeState "opening", label: '${name}', action: "stop", icon: "st.shades.shade-opening", backgroundColor: "#00a0dc", nextState: "partially open"
                attributeState "closing", label: '${name}', action: "stop", icon: "st.shades.shade-closing", backgroundColor: "#00a0dc", nextState: "partially open"
                attributeState "partially open", label: '${name}', action: "close", icon: "st.shades.shade-open", backgroundColor: "#00a0dc", nextState: "closing"
            }
        }
        standardTile("open", "device.open", decoration: "flat", width: 2, height: 2) {
            state "open", label: 'Open', action: "open", icon: "st.shades.shade-opening"
        }
        standardTile("close", "device.close", decoration: "flat", width: 2, height: 2) {
            state "close", label: 'Close', action: "close", icon: "st.shades.shade-closing"
        }
        standardTile("stop", "device.stop", decoration: "flat", width: 2, height: 2) {
            state "stop", label: 'Stop', action: "stop", icon: "st.Home.home30"
        }
        standardTile("home", "device.level", decoration: "flat", width: 2, height: 2) {
            state "default", label: "home", action:"presetPosition", icon:"st.Home.home2"
        }
        standardTile("refresh", "device.refresh", decoration: "flat", width: 2, height: 2) {
            state "refresh", label: 'Refresh', action: "refresh", icon: "st.secondary.refresh-icon"
        }
        standardTile("configure", "device.configure", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "NO", label: '', action: "configuration.configure", icon: "st.secondary.configure"
            state "YES", label: '', action: "configuration.configure", icon: "https://github.com/erocm123/SmartThingsPublic/raw/master/devicetypes/erocm123/qubino-flush-1d-relay.src/configure@2x.png"
        }

        main "windowShade"
        details(["windowShade", "open", "close", "stop", "home", "configure", "refresh"])
    }

    preferences {
        input "preset", "number", title: "Preset position", defaultValue: 50, range: "1..100", required: false, displayDuringSetup: false
        input description: "Once you change values on this page, the corner of the \"configuration\" icon will change orange until all configuration parameters are updated.", title: "Settings", displayDuringSetup: false, type: "paragraph", element: "paragraph"
        generate_preferences(configuration_model())
    }
}

def open() {
    log.debug("open")
    commands([
            zwave.basicV1.basicSet(value: 0xFF),
            zwave.basicV1.basicGet()
    ])
}

def close() {
    log.debug("close")
    commands([
            zwave.basicV1.basicSet(value: 0x00),
            zwave.basicV1.basicGet()
    ])
}

def stop() {
    commands([
            zwave.switchMultilevelV3.switchMultilevelStopLevelChange(),
            zwave.switchMultilevelV3.switchMultilevelGet()
    ])
}

def pause() {
    log.debug "pause()"
    stop()
}

def on() {
    open()
}

def off() {
    close()
}

def presetPosition() {
    close()
}

def setLevel(value, duration = null) {
    log.debug "setLevel(value:${value})"
    if (value == 0) {
        close()
    } else {
        open()
    }
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


private List handleStateChange(physicalgraph.zwave.Command cmd) {
    def result = []
    def shadeValue
    def switchValue
    def level = cmd.value as Integer
    if (level == 99) {
        level = 100
        shadeValue = "open"
        switchValue = "on"
    } else {
        level = 0  // unlike dimmer switches, the level isn't saved when closed
        shadeValue = "closed"
        switchValue = "off"
    }
    result << createEvent(name: "windowShade", value: shadeValue, isStateChange: true)
    result << createEvent(name: "switch", value: switchValue, isStateChange: true)
    result << createEvent(name: "level", value: level, unit: "%", isStateChange: true)
    log.debug "handleStateChange(windowShade:${shadeValue} :: level:${level} :: switch:${switchValue})"
    return result
}

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

def zwaveEvent(physicalgraph.zwave.commands.configurationv2.ConfigurationReport cmd) {
    update_current_properties(cmd)
    logging("${device.displayName} parameter '${cmd.parameterNumber}' with a byte size of '${cmd.size}' is set to '${cmd2Integer(cmd.configurationValue)}'", 2)
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

def installed() {
    log.debug("installed()")
    // Device-Watch simply pings if no device events received for checkInterval duration of 32min = 2 * 15min + 2min lag time
    sendEvent(name: "checkInterval", value: 1920, displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID, offlinePingable: "1"])
    sendEvent(name: "supportedWindowShadeCommands", value: JsonOutput.toJson(["open", "close", "pause"]), displayed: false)
}

def updated() {
    log.debug("updated()")
    def cmds = update_needed_settings()
    sendEvent(name: "checkInterval", value: 2 * 15 * 60 + 2 * 60, displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID])
    sendEvent(name: "needUpdate", value: device.currentValue("needUpdate"), displayed: false, isStateChange: true)
    if (cmds != []) response(commands(cmds))
//    response(refresh())
}

def configure() {
    log.debug("configure()")
    def cmds = []
    cmds = update_needed_settings()
    if (cmds != []) commands(cmds)
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

private def logging(message, level) {
    log.debug "$message"
}

def configuration_model() {
    '''
<configuration>
    <Value type="number" byteSize="1" index="35" label="113 time frame moving up or down." min="1" max="2147483647" value="30"
           setting_type="zwave" fw="">
        <Help>
            Sets the move time from up (left) to down (right) for connected motor.
            Default: 30
        </Help>
    </Value>
    <Value type="list" byteSize="1" index="80" label="80 Instant Notification" min="0" max="1" value="0" setting_type="zwave" fw="">
        <Help>
            Notification report of status change sent to Group Assocation #1 when state of output load changes. Used to instantly update status to
            your gateway typically.
            0 - Nothing
            1 - Basic Report CC
            Range: 0~1
            Default: 0
        </Help>
        <Item label="None" value="0"/>
        <Item label="Basic Report CC" value="1"/>
    </Value>
    <Value type="list" byteSize="1" index="85" label="85 Set operation mode" min="0" max="1" value="0" setting_type="zwave" fw="">
        <Help>
            Set the operation mode of external switch.
            0 = Operation Mode 1.
            1 = Operation Mode 2.
            Range: 0~1
            Default: 0
        </Help>
        <Item label="Operation Mode 1" value="0"/>
        <Item label="Operation Mode 2" value="1"/>
    </Value>
    <Value type="list" byteSize="1" index="120" label="120 External Switch S1 Setting" min="0" max="4" value="0" setting_type="zwave" fw="">
        <Help>
            Configure the external switch mode for S1 via Configuration Set.
            0 = Unidentified mode.
            1 = 2-state switch mode.
            2 = 3-way switch mode.
            3 = momentary switch button mode.
            4 = Enter automatic identification mode. //can enter this mode by tapping internal button 4x times within 2 seconds.
            Note: When the mode is determined, this mode value will not be reset after exclusion.
            Range: 0~4
            Default: 0 (Previous State)
        </Help>
        <Item label="Unidentified" value="0"/>
        <Item label="2-State Switch Mode" value="1"/>
        <Item label="3-way Switch Mode" value="2"/>
        <Item label="Momentary Push Button Mode" value="3"/>
        <Item label="Automatic Identification" value="4"/>
    </Value>
    <Value type="list" byteSize="1" index="121" label="121 External Switch S2 Setting" min="0" max="4" value="0" setting_type="zwave" fw="">
        <Help>
            Configure the external switch mode for S2 via Configuration Set.
            0 = Unidentified mode.
            1 = 2-state switch mode.
            2 = 3-way switch mode.
            3 = momentary switch button mode.
            4 = Enter automatic identification mode. //can enter this mode by tapping internal button 6x times within 2 seconds.
            Note: When the mode is determined, this mode value will not be reset after exclusion.
            Range: 0~4
            Default: 0 (Previous State)
        </Help>
        <Item label="Unidentified" value="0"/>
        <Item label="2-State Switch Mode" value="1"/>
        <Item label="3-way Switch Mode" value="2"/>
        <Item label="Momentary Push Button Mode" value="3"/>
        <Item label="Automatic Identification" value="4"/>
    </Value>
</configuration>
'''
}
