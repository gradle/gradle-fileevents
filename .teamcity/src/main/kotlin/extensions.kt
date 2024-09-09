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

import jetbrains.buildServer.configs.kotlin.BuildType
import jetbrains.buildServer.configs.kotlin.Requirements
import jetbrains.buildServer.configs.kotlin.buildSteps.GradleBuildStep

fun Requirements.requireAgent(agent: Agent) {
    agent.os.addAgentRequirements(this)
    contains("teamcity.agent.jvm.os.arch", agent.architecture.agentRequirementForOs(agent.os))
}

fun BuildType.runOn(agent: Agent, javaVersion: Int) {
    if (agent.container.isNullOrBlank()) {
        params {
            param(
                "env.JAVA_HOME",
                "%${agent.os.osType.lowercase()}.java${javaVersion}.openjdk.${agent.architecture.paramName}%"
            )
        }
    }

    requirements {
        requireAgent(agent)
    }

    if (!agent.container.isNullOrBlank()) {
        steps.items.filterIsInstance<GradleBuildStep>().forEach {
            it.dockerImage = agent.container
            it.dockerPull = true
        }
    }
}
