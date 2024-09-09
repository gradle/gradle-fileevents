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
import jetbrains.buildServer.configs.kotlin.ParameterDisplay
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle

class Publish(build: Build, tests: TestTrigger) : BaseBuildType({
    name = "Publish File Events"
    type = Type.DEPLOYMENT

    artifactRules = """
        build/reports/** => file-events/reports
        build-logic/build/reports/** => build-logic/reports
    """.trimIndent()

    params {
        param("env.GRADLE_INTERNAL_REPO_URL", "%gradle.internal.repository.url%")
        param("ARTIFACTORY_USERNAME", "%gradle.internal.repository.build-tool.publish.username%")
        password("ARTIFACTORY_PASSWORD", "gradle.internal.repository.build-tool.publish.password%", display = ParameterDisplay.HIDDEN)
        param("env.ORG_GRADLE_PROJECT_publishApiKey", "%ARTIFACTORY_PASSWORD%")
        param("env.ORG_GRADLE_PROJECT_publishUserName", "%ARTIFACTORY_USERNAME%")
    }

    steps {
        gradle {
            name = "Gradle publish"
            tasks = "publish"
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
        snapshot(tests) {
            onDependencyFailure = FailureAction.FAIL_TO_START
            onDependencyCancel = FailureAction.FAIL_TO_START
        }
    }

    runOn(Agent.MacOsAarch64, 17)
})
