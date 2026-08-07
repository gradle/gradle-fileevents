package org.gradle.fileevents

import spock.lang.Specification

class WindowsVersionSupportTest extends Specification {
    def "file events are #description on #osName #osVersion"() {
        expect:
        FileEvents.isWindowsVersionSupported(osName, osVersion) == supported

        where:
        osName                   | osVersion | supported
        "Windows Vista"          | "6.0"     | false
        "Windows 7"              | "6.1"     | false
        "Windows 8"              | "6.2"     | false
        "Windows 8.1"            | "6.3"     | false
        "Windows Server 2008 R2" | "6.1"     | false
        "Windows Server 2012 R2" | "6.3"     | false
        "Windows Server 2016"    | "10.0"    | false
        "Windows Server 2019"    | "10.0"    | true
        "Windows Server 2022"    | "10.0"    | true
        "Windows 10"             | "10.0"    | true
        "Windows 11"             | "10.0"    | true
        "Windows 11"             | "11"      | true
        // Unrecognized version formats should not disable file events
        "Windows NT (unknown)"   | ""        | true

        description = supported ? "supported" : "not supported"
    }
}
