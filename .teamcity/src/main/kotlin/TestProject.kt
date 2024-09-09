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

import jetbrains.buildServer.configs.kotlin.FailureAction
import jetbrains.buildServer.configs.kotlin.Project
import jetbrains.buildServer.configs.kotlin.RelativeId
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.ui.id

class TestProject(build: Build) : Project({
    name = "Test File Events"
    id(RelativeId("Test"))

    Agent.entries.forEach { agent ->
        buildType(BaseBuildType {
            name = "Test on $agent"
            id = RelativeId("Test$agent")

            steps {
                gradle {
                    name = "Gradle externalTest"
                    tasks = "externalTest"
                }
            }

            dependencies {
                snapshot(build) {
                    onDependencyFailure = FailureAction.FAIL_TO_START
                    onDependencyCancel = FailureAction.FAIL_TO_START
                }
                dependency(build) {
                    artifacts {
                        cleanDestination = true
                        artifactRules = "repo => build/remote/"
                    }
                }
            }

            runOn(agent, 17)
        })
    }
}) {
    val trigger = TestTrigger(this.buildTypes)
}
