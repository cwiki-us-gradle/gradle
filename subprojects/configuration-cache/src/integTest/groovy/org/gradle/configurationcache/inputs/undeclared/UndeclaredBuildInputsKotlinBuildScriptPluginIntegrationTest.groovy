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

package org.gradle.configurationcache.inputs.undeclared

import spock.lang.Ignore

class UndeclaredBuildInputsKotlinBuildScriptPluginIntegrationTest extends AbstractUndeclaredBuildInputsIntegrationTest implements KotlinPluginImplementation {
    @Override
    String getLocation() {
        return "Plugin class 'Build_gradle\$SneakyPlugin'"
    }

    @Override
    void buildLogicApplication(SystemPropertyRead read) {
        kotlinPlugin(buildKotlinFile, read)
        buildKotlinFile << """
            plugins.apply(SneakyPlugin::class.java)
        """
    }

    @Ignore
    def "can reference methods from kotlin function"() {
        expect: false
    }
}
