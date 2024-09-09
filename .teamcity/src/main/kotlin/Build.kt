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

import jetbrains.buildServer.configs.kotlin.buildSteps.gradle

class Build : BaseBuildType({
    name = "Build File Events"

    artifactRules = """
        build/reports/** => file-events/reports
        build-logic/build/reports/** => build-logic/reports
        build/libs => repo
    """.trimIndent()

    params {
        param("env.PGP_SIGNING_KEY", "%pgpSigningKey%")
        param("env.PGP_SIGNING_KEY_PASSPHRASE", "%pgpSigningPassphrase%")

        param("env.GRADLE_INTERNAL_REPO_URL", "%gradle.internal.repository.url%")
    }

    steps {
        gradle {
            name = "Gradle assemble"
            tasks = "clean assemble"
        }
    }

    runOn(Agent.MacOsAarch64, 17)
})
