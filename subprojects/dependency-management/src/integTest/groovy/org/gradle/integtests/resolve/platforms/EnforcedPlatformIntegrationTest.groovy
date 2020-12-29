/*
 * Copyright 2020 the original author or authors.
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

package org.gradle.integtests.resolve.platforms

import org.gradle.integtests.fixtures.AbstractHttpDependencyResolutionTest
import org.gradle.integtests.fixtures.ToBeFixedForConfigurationCache
import org.gradle.integtests.fixtures.resolve.ResolveTestFixture

class EnforcedPlatformIntegrationTest extends AbstractHttpDependencyResolutionTest {

    def setup() {
        settingsFile << """
            rootProject.name = 'test'
        """
    }

    @ToBeFixedForConfigurationCache(because = "Resolve test fixture doesn't support configuration cache")
    def "dependency on unsatisfiable range shouldn't trigger null pointer exception"() {
        settingsFile << """
            include 'platform'
            include 'lib'
        """

        buildFile << """
            allprojects {
                ${mavenCentralRepository()}
            }

            apply plugin: 'java-library'

            dependencies {
                implementation enforcedPlatform(project(":platform"))
                implementation project(':lib')
            }
        """

        file("lib/build.gradle") << """
            plugins {
                id 'java-library'
            }
            dependencies {
                api 'org.apache.commons:commons-lang3:3.10'
            }
        """

        file('platform/build.gradle') << """
            plugins {
                id 'java-platform'
            }

            dependencies {
                constraints {
                    // Deliberately unsatisfiable constraint with version range
                    api 'org.apache.commons:commons-lang3:[99,)'
                }
            }
        """
        def resolve = new ResolveTestFixture(buildFile, 'compileClasspath')
        resolve.expectDefaultConfiguration("compile")
        resolve.prepare()

        when:
        fails ':checkDeps'

        then:
        failure.assertHasCause "Could not find any version that matches org.apache.commons:commons-lang3:[99,)"
    }
}
