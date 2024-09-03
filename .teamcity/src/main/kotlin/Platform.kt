/*
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import jetbrains.buildServer.configs.kotlin.Requirements

enum class Agent(val os: Os, val architecture: Architecture) {
    UbuntuAmd64(os = Os.Ubuntu16, architecture = Architecture.Amd64),
    UbuntuAarch64(os = Os.Ubuntu24, architecture = Architecture.Aarch64),
    AmazonLinuxAmd64(os = Os.AmazonLinux, architecture = Architecture.Amd64),
    AmazonLinuxAarch64(os = Os.AmazonLinux, architecture = Architecture.Aarch64),
    CentOsAmd64(os = Os.CentOs, architecture = Architecture.Amd64),
    WindowsAmd64(os = Os.Windows, architecture = Architecture.Amd64),
    MacOsAmd64(os = Os.MacOs, architecture = Architecture.Amd64),
    MacOsAarch64(os = Os.MacOs, architecture = Architecture.Aarch64),
}

interface Os {
    fun addAgentRequirements(requirements: Requirements)
    val osType: String

    object Ubuntu16 : Ubuntu(16)

    object Ubuntu24 : Ubuntu(24)

    object AmazonLinux : Linux() {
        override fun Requirements.additionalRequirements() {
            contains(osDistributionNameParameter, "amazon")
        }
    }

    object CentOs : Linux() {
        override fun Requirements.additionalRequirements() {
            contains(osDistributionNameParameter, "centos")
            contains(osDistributionVersionParameter, "9")
        }
    }

    object MacOs : OsWithNameRequirement("Mac OS X", "MacOs")

    object Windows : OsWithNameRequirement("Windows", "Windows")
}

private const val osDistributionNameParameter = "system.agent.os.distribution.name"
private const val osDistributionVersionParameter = "system.agent.os.distribution.version"

abstract class OsWithNameRequirement(private val osName: String, override val osType: String) : Os {
    override fun addAgentRequirements(requirements: Requirements) {
        requirements.contains("teamcity.agent.jvm.os.name", osName)
        requirements.additionalRequirements()
    }

    open fun Requirements.additionalRequirements() {}
}

abstract class Linux : OsWithNameRequirement("Linux", "Linux")

abstract class Ubuntu(val version: Int) : Linux() {
    override fun Requirements.additionalRequirements() {
        contains(osDistributionNameParameter, "ubuntu")
        contains(osDistributionVersionParameter, version.toString())
    }
}

enum class Architecture(val paramName: String) {
    Aarch64("aarch64") {
        override fun agentRequirementForOs(os: Os): String = "aarch64"
    },
    Amd64("64bit") {
        override fun agentRequirementForOs(os: Os): String = when (os) {
            Os.MacOs -> "x86_64"
            else -> "amd64"
        }
    };

    abstract fun agentRequirementForOs(os: Os): String
}
