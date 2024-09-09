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
import jetbrains.buildServer.configs.kotlin.FailureAction
import jetbrains.buildServer.configs.kotlin.buildFeatures.PullRequests
import jetbrains.buildServer.configs.kotlin.buildFeatures.pullRequests
import jetbrains.buildServer.configs.kotlin.triggers.VcsTrigger
import jetbrains.buildServer.configs.kotlin.triggers.vcs

class TestTrigger(dependencies: List<BuildType>) : BaseBuildType({
    name = "Test File Events (Trigger)"
    type = Type.COMPOSITE

    triggers {
        vcs {
            quietPeriodMode = VcsTrigger.QuietPeriodMode.USE_CUSTOM
            quietPeriod = 0

            branchFilter = """
                +:(*)
                +:gh-readonly-(queue/*/pr-*)-*
            """.trimIndent()

            triggerRules = """
                +:.
            """.trimIndent()

            enabled = true
        }
    }

    features {
        pullRequests {
            provider = github {
                authType = vcsRoot()
                filterTargetBranch = "main"
                filterAuthorRole = PullRequests.GitHubRoleFilter.MEMBER_OR_COLLABORATOR
                ignoreDrafts = true
            }
        }
    }

    dependencies {
        dependencies.forEach {
            snapshot(it) {
                onDependencyFailure = FailureAction.ADD_PROBLEM
                onDependencyCancel = FailureAction.FAIL_TO_START
            }
        }
    }
})
