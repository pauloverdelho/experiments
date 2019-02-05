/**
 * 	Fibaro Roller Shutter 3
 */
metadata {
    definition(name: "Fibaro Roller Shutter 3", namespace: "smartthings", author: "Paulo Verdelho", ocfDeviceType: "oic.d.blind") {
        capability "Window Shade"
        capability "Energy Meter"
        capability "Power Meter"
        capability "Configuration"
        capability "Health Check"
        capability "Refresh"

        command "reset"
        command "calibrate"
        command "stop"
        command "closeNow"
        command "openNow"

        capability "Switch Level"   // until we get a Window Shade Level capability

        // RAW information on device
        //zw:Ls type:1106 mfr:010F prod:0303 model:1000 ver:5.00 zwv:6.02 lib:03 cc:5E,55,98,9F,56,6C,22 sec:26,85,8E,59,86,72,5A,73,32,70,71,75,60,5B,7A role:05 ff:9900 ui:9900 ep:['1106 5E,98,9F,6C,22', '1106 5E,98,9F,6C,22']
        fingerprint mfr: "010F", prod: "0303", model: "1000"
    }

    tiles(scale: 2) {
        multiAttributeTile(name: "windowShade", type: "lighting", width: 6, height: 4) {
            tileAttribute("device.windowShade", key: "PRIMARY_CONTROL") {
                attributeState "unknown", label: '${name}', action: "calibrate", icon: "st.shades.shade-closed", backgroundColor: "#ffffff"
                attributeState "closed", label: '${name}', action: "open", icon: "st.shades.shade-closed", backgroundColor: "#ffffff", nextState: "opening"
                attributeState "open", label: '${name}', action: "close", icon: "st.shades.shade-open", backgroundColor: "#00a0dc", nextState: "closing"
                attributeState "opening", label: '${name}', action: "stop", icon: "st.shades.shade-opening", backgroundColor: "#00a0dc", nextState: "partially open"
                attributeState "closing", label: '${name}', action: "stop", icon: "st.shades.shade-closing", backgroundColor: "#00a0dc", nextState: "partially open"
                attributeState "partially open", label: '${name}', action: "close", icon: "st.shades.shade-open", backgroundColor: "#00a0dc", nextState: "closing"
            }
            tileAttribute("device.multiStatus", key: "SECONDARY_CONTROL") {
                attributeState("multiStatus", label: '${currentValue}')
            }
            tileAttribute("device.level", key: "SLIDER_CONTROL") {
                attributeState "level", action: "setLevel"
            }
        }
        valueTile("open", "device.open", decoration: "flat", width: 2, height: 2) {
            state "open", label: 'Open', action: "openNow", icon: "st.shades.shade-opening"
        }
        valueTile("close", "device.close", decoration: "flat", width: 2, height: 2) {
            state "close", label: 'Close', action: "closeNow", icon: "st.shades.shade-closing"
        }
        valueTile("stop", "device.stop", decoration: "flat", width: 2, height: 2) {
            state "stop", label: 'Stop', action: "stop", icon: "st.Home.home30"
        }
        valueTile("power", "device.power", decoration: "flat", width: 2, height: 1) {
            state "power", label: '${currentValue}\nW'
        }
        valueTile("energy", "device.energy", decoration: "flat", width: 2, height: 1) {
            state "energy", label: '${currentValue}\nkWh'
        }
        standardTile("refresh", "device.refresh", decoration: "flat", width: 2, height: 2) {
            state "refresh", label: 'Refresh', action: "refresh", icon: "st.secondary.refresh-icon"
        }
        valueTile("reset", "device.energy", decoration: "flat", width: 2, height: 1) {
            state "reset", label: 'reset kWh', action: "reset", icon: "st.secondary.tools"
        }
        valueTile("calibrate", "device.calibrate", decoration: "flat", width: 2, height: 1) {
            state "calibrate", label: 'Calibrate', action: "calibrate", icon: "st.contact.contact.closed"
        }

        main "windowShade"
        details(["windowShade", "open", "close", "stop", "power", "energy", "refresh", "reset", "calibrate"])

    }

    preferences {
        input(
                title: "Fibaro Roller Shutter 3 manual",
                description: "Tap to view the manual.",
                image: "http://manuals.fibaro.com/wp-content/uploads/2017/02/d2_icon.png",
                url: "https://manuals.fibaro.com/content/manuals/en/FGR-223/FGR-223-EN-T-v1.0.pdf",
                type: "href",
                element: "href"
        )

        parameterMap().each {
            input(
                    title: "${it.num}. ${it.title}",
                    description: it.descr,
                    type: "paragraph",
                    element: "paragraph"
            )

            input(
                    name: it.key,
                    title: null,
                    type: it.type,
                    options: it.options,
                    range: (it.min != null && it.max != null) ? "${it.min}..${it.max}" : null,
                    defaultValue: it.def,
                    required: false
            )
        }

        input(name: "logging", title: "Logging", type: "boolean", required: false)
    }
}

def open() { encap(zwave.basicV1.basicSet(value: 99)) }

def close() { encap(zwave.basicV1.basicSet(value: 0)) }

def presetPosition() {
    setLevel(preset ?: state.preset ?: 99)
}

def openNow() {
    setLevel(99)
}

def closeNow() {
    setLevel(0)
}

def stop() { encap(zwave.switchMultilevelV3.switchMultilevelStopLevelChange()) }

def calibrate() { encap(zwave.configurationV2.configurationSet(configurationValue: intToParam(2, 1), parameterNumber: 150, size: 1)) }

def setLevel(value, duration = null) {
    logging("${device.displayName} - Executing setLevel(${value.inspect()})")
    Integer level = value as Integer
    if (level < 0) level = 0
    if (level > 99) level = 99

    Integer currentLevel = device.currentValue("level")
    if (currentLevel < level) sendEvent(name: "windowShade", value: "opening")
    if (currentLevel > level) sendEvent(name: "windowShade", value: "closing")

    encap(zwave.basicV1.basicSet(value: level))
}

def reset() {
    logging("${device.displayName} - Executing reset()", "info")
    def cmds = []
    cmds << zwave.meterV3.meterReset()
    cmds << zwave.meterV3.meterGet(scale: 0)
    encapSequence(cmds, 1000)
}

def refresh() {
    logging("${device.displayName} - Executing refresh()", "info")
    def cmds = []
    cmds << zwave.basicV1.basicGet()
    cmds << zwave.meterV3.meterGet(scale: 0)
    cmds << zwave.meterV3.meterGet(scale: 2)
    cmds << zwave.switchMultilevelV1.switchMultilevelGet()
    cmds << zwave.sensorMultilevelV5.sensorMultilevelGet()
    encapSequence(cmds, 1000)
}

def ping(){
    refresh()
}

def configure() {
    sendEvent(name: "windowShade", value: "closed", displayed: "true") //set the initial state to closed.
}

def installed() {
    log.debug "installed()"
    sendEvent(name: "checkInterval", value: 1920, displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID, offlinePingable: "1"])
    response(refresh())
}

def updated() {
    if (state.lastUpdated && (now() - state.lastUpdated) < 500) return
    logging("${device.displayName} - Executing updated()", "info")
    runIn(3, "syncStart")
    state.lastUpdated = now()
}

def syncStart() {
    boolean syncNeeded = false
    parameterMap().each {
        if (settings."$it.key" != null) {
            if (state."$it.key" == null) {
                state."$it.key" = [value: null, state: "synced"]
            }
            if (state."$it.key".value != settings."$it.key" as Integer || state."$it.key".state in ["notSynced", "inProgress"]) {
                state."$it.key".value = settings."$it.key" as Integer
                state."$it.key".state = "notSynced"
                syncNeeded = true
            }
        }
    }
    if (syncNeeded) {
        logging("${device.displayName} - starting sync.", "info")
        multiStatusEvent("Sync in progress.", true, true)
        syncNext()
    }
}

private syncNext() {
    logging("${device.displayName} - Executing syncNext()", "info")
    def cmds = []
    for (param in parameterMap()) {
        if (state."$param.key"?.value != null && state."$param.key"?.state in ["notSynced", "inProgress"]) {
            multiStatusEvent("Sync in progress. (param: ${param.num})", true)
            state."$param.key"?.state = "inProgress"
            cmds << response(encap(zwave.configurationV2.configurationSet(configurationValue: intToParam(state."$param.key".value, param.size), parameterNumber: param.num, size: param.size)))
            cmds << response(encap(zwave.configurationV2.configurationGet(parameterNumber: param.num)))
            break
        }
    }
    if (cmds) {
        runIn(10, "syncCheck")
        sendHubCommand(cmds, 1000)
    } else {
        runIn(1, "syncCheck")
    }
}

private syncCheck() {
    logging("${device.displayName} - Executing syncCheck()", "info")
    def failed = []
    def incorrect = []
    def notSynced = []
    parameterMap().each {
        if (state."$it.key"?.state == "incorrect") {
            incorrect << it
        } else if (state."$it.key"?.state == "failed") {
            failed << it
        } else if (state."$it.key"?.state in ["inProgress", "notSynced"]) {
            notSynced << it
        }
    }
    if (failed) {
        logging("${device.displayName} - Sync failed! Check parameter: ${failed[0].num}", "info")
        sendEvent(name: "syncStatus", value: "failed")
        multiStatusEvent("Sync failed! Check parameter: ${failed[0].num}", true, true)
    } else if (incorrect) {
        logging("${device.displayName} - Sync mismatch! Check parameter: ${incorrect[0].num}", "info")
        sendEvent(name: "syncStatus", value: "incomplete")
        multiStatusEvent("Sync mismatch! Check parameter: ${incorrect[0].num}", true, true)
    } else if (notSynced) {
        logging("${device.displayName} - Sync incomplete!", "info")
        sendEvent(name: "syncStatus", value: "incomplete")
        multiStatusEvent("Sync incomplete! Open settings and tap Done to try again.", true, true)
    } else {
        logging("${device.displayName} - Sync Complete", "info")
        sendEvent(name: "syncStatus", value: "synced")
        multiStatusEvent("Sync OK.", true, true)
    }
}

private multiStatusEvent(String statusValue, boolean force = false, boolean display = false) {
    if (!device.currentValue("multiStatus")?.contains("Sync") || device.currentValue("multiStatus") == "Sync OK." || force) {
        sendEvent(name: "multiStatus", value: statusValue, descriptionText: statusValue, displayed: display)
    }
}

def zwaveEvent(physicalgraph.zwave.commands.configurationv2.ConfigurationReport cmd) {
    def paramKey = parameterMap().find({ it.num == cmd.parameterNumber })?.key
    logging("${device.displayName} - Parameter ${paramKey} value is ${cmd.scaledConfigurationValue} expected " + state?."$paramKey"?.value, "info")
    state."$paramKey"?.state = (state."$paramKey"?.value == cmd.scaledConfigurationValue) ? "synced" : "incorrect"
    syncNext()
}

def zwaveEvent(physicalgraph.zwave.commands.applicationstatusv1.ApplicationRejectedRequest cmd) {
    logging("${device.displayName} - rejected request!", "warn")
    for (param in parameterMap()) {
        if (state."$param.key"?.state == "inProgress") {
            state."$param.key"?.state = "failed"
            break
        }
    }
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd) {
    handleLevelReport(cmd)
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicSet cmd) {
    handleLevelReport(cmd)
}

def zwaveEvent(physicalgraph.zwave.commands.switchmultilevelv3.SwitchMultilevelReport cmd) {
    handleLevelReport(cmd)
}

private handleLevelReport(physicalgraph.zwave.Command cmd) {
    logging("${device.displayName} - LevelReport received, $cmd", "info")
    def descriptionText = null
    def shadeValue

    def level = cmd.value as Integer
    if (level >= 99) {
        level = 100
        shadeValue = "open"
    } else if (level <= 0) {
        level = 0  // unlike dimmer switches, the level isn't saved when closed
        shadeValue = "closed"
    } else {
        shadeValue = "partially open"
        descriptionText = "${device.displayName} shade is ${level}% open"
    }
    def levelEvent = createEvent(name: "level", value: level, unit: "%", displayed: false)
    def stateEvent = createEvent(name: "windowShade", value: shadeValue, descriptionText: descriptionText, isStateChange: levelEvent.isStateChange)

    logging("${device.displayName} - Setting level to ${level} and windowsShade to ${shadeValue}", "info")

    def result = [stateEvent, levelEvent]
    result
}

def zwaveEvent(physicalgraph.zwave.commands.sensormultilevelv5.SensorMultilevelReport cmd) {
    logging("${device.displayName} - SensorMultilevelReport received, $cmd", "info")
    if (cmd.sensorType == 4) {
        sendEvent(name: "power", value: cmd.scaledSensorValue, unit: "W")
        multiStatusEvent("${(device.currentValue("power") ?: "0.0")} W | ${(device.currentValue("energy") ?: "0.00")} kWh")
    }
}

def zwaveEvent(physicalgraph.zwave.commands.meterv3.MeterReport cmd) {
    logging("${device.displayName} - MeterReport received, value: ${cmd.scaledMeterValue} scale: ${cmd.scale}", "info")
    switch (cmd.scale) {
        case 0:
            sendEvent([name: "energy", value: cmd.scaledMeterValue, unit: "kWh"])
            break
        case 2:
            sendEvent([name: "power", value: cmd.scaledMeterValue, unit: "W"])
            break
    }
    multiStatusEvent("${(device.currentValue("power") ?: "0.0")} W | ${(device.currentValue("energy") ?: "0.00")} kWh")
}

def zwaveEvent(physicalgraph.zwave.commands.switchmultilevelv3.SwitchMultilevelStopLevelChange cmd) {
    [createEvent(name: "windowShade", value: "partially open", displayed: false, descriptionText: "$device.displayName shade stopped"),
     response(zwave.switchMultilevelV1.switchMultilevelGet().format())]
}

def zwaveEvent(physicalgraph.zwave.Command cmd) {
    log.debug "${device.displayName} - Unhandled $cmd"
    return []
}

def parse(String description) {
    def result = []
    logging("${device.displayName} - Parsing: ${description}")
    if (description.startsWith("Err 106")) {
        result = createEvent(
                descriptionText: "Failed to complete the network security key exchange. If you are unable to receive data from it, you must remove it from your network and add it again.",
                eventType: "ALERT",
                name: "secureInclusion",
                value: "failed",
                displayed: true,
        )
    } else if (description == "updated") {
        return null
    } else {
        def cmd = zwave.parse(description, cmdVersions())
        if (cmd) {
            logging("${device.displayName} - Parsed: ${cmd}")
            zwaveEvent(cmd)
        }
    }
}

def zwaveEvent(physicalgraph.zwave.commands.securityv1.SecurityMessageEncapsulation cmd) {
    def encapsulatedCommand = cmd.encapsulatedCommand(cmdVersions())
    if (encapsulatedCommand) {
        logging("${device.displayName} - Parsed SecurityMessageEncapsulation into: ${encapsulatedCommand}")
        zwaveEvent(encapsulatedCommand)
    } else {
        log.warn "Unable to extract Secure command from $cmd"
    }
}

def zwaveEvent(physicalgraph.zwave.commands.crc16encapv1.Crc16Encap cmd) {
    def version = cmdVersions()[cmd.commandClass as Integer]
    def ccObj = version ? zwave.commandClass(cmd.commandClass, version) : zwave.commandClass(cmd.commandClass)
    def encapsulatedCommand = ccObj?.command(cmd.command)?.parse(cmd.data)
    if (encapsulatedCommand) {
        logging("${device.displayName} - Parsed Crc16Encap into: ${encapsulatedCommand}")
        zwaveEvent(encapsulatedCommand)
    } else {
        log.warn "Unable to extract CRC16 command from $cmd"
    }
}

private logging(text, type = "debug") {
    if (settings.logging == "true") {
        log."$type" text
    }
}

private secEncap(physicalgraph.zwave.Command cmd) {
    logging("${device.displayName} - encapsulating command using Secure Encapsulation, command: $cmd", "info")
    zwave.securityV1.securityMessageEncapsulation().encapsulate(cmd).format()
}

private crcEncap(physicalgraph.zwave.Command cmd) {
    logging("${device.displayName} - encapsulating command using CRC16 Encapsulation, command: $cmd", "info")
    zwave.crc16EncapV1.crc16Encap().encapsulate(cmd).format()
}


private encap(physicalgraph.zwave.Command cmd, Integer ep) {
    encap(multiEncap(cmd, ep))
}

private encap(List encapList) {
    encap(encapList[0], encapList[1])
}

private encap(Map encapMap) {
    encap(encapMap.cmd, encapMap.ep)
}

private encap(physicalgraph.zwave.Command cmd) {
    if (zwaveInfo.zw.contains("s")) {
        secEncap(cmd)
    } else if (zwaveInfo.cc.contains("56")) {
        crcEncap(cmd)
    } else {
        logging("${device.displayName} - no encapsulation supported for command: $cmd", "info")
        cmd.format()
    }
}

private encapSequence(cmds, Integer delay = 250) {
    delayBetween(cmds.collect { encap(it) }, delay)
}

private encapSequence(cmds, Integer delay, Integer ep) {
    delayBetween(cmds.collect { encap(it, ep) }, delay)
}

private List intToParam(Long value, Integer size = 1) {
    def result = []
    size.times {
        result = result.plus(0, (value & 0xFF) as Short)
        value = (value >> 8)
    }
    return result
}

private Map cmdVersions() {
    [0x5E: 1, 0x86: 1, 0x72: 2, 0x59: 1, 0x73: 1, 0x22: 1, 0x31: 5, 0x32: 3, 0x71: 3, 0x56: 1, 0x98: 1, 0x7A: 2, 0x20: 1, 0x5A: 1, 0x85: 2, 0x26: 3, 0x8E: 2, 0x60: 3, 0x70: 2, 0x75: 2, 0x27: 1]
}

private parameterMap() {
    [
            [key  : "switchType", num: 20, size: 1, type: "enum", options: [
                    0: "Momentary switches",
                    1: "Toggle switches",
                    2: "Single, momentary switch. (The switch should be connected to S1 terminal)"
            ], def: "2", title: "Switch type",
             descr: "The parameter settings are relevant for Roller Blind Mode and Venetian Blind Mode (parameter 151 set to 0, 1, 2)."],
            [key  : "inputsOrientation", num: 24, size: 1, type: "enum", options: [
                    0: "default (S1 - 1st channel, S2 - 2nd channel)",
                    1: "reversed (S1 - 2nd channel, S2 - 1st channel)"
            ], def: "0", title: "Inputs Orientation"],
            [key  : "outputsOrientation", num: 25, size: 1, type: "enum", options: [
                    0: "default (Q1 - 1st channel, Q2 - 2nd channel)",
                    1: "reversed (Q1 - 2nd channel, Q2 - 1st channel)"
            ], def: "0", title: "Outputs Orientation"],
            [key  : "operatingMode", num: 151, size: 1, type: "enum", options: [
                    0: "0 - Roller Blind Mode, without positioning",
                    1: "1 - Roller Blind Mode, with positioning",
                    2: "2 - Venetian Blind Mode, with positioning",
                    3: "3 - Gate Mode, without positioning",
                    4: "4 - Gate Mode, with positioning"
            ], def: "1", title: "Roller Shutter operating modes",
             descr: ""],
            [key  : "motorOperationDetection", num: 155, size: 2, type: "number", def: 10, min: 0, max: 255, title: "Motor operation detection",
             descr: "Power threshold to be interpreted as reaching a limit switch. (1-255 W)"],
    ]
}
