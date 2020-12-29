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

package org.gradle.api.plugins.jvm.internal

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.artifacts.ConfigurationPublications
import org.gradle.api.artifacts.PublishArtifact
import org.gradle.api.artifacts.PublishArtifactSet
import org.gradle.api.attributes.Bundling
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.DocsType
import org.gradle.api.attributes.HasConfigurableAttributes
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.attributes.Usage
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.internal.artifacts.ConfigurationVariantInternal
import org.gradle.api.internal.artifacts.configurations.ConfigurationInternal
import org.gradle.api.internal.tasks.DefaultSourceSetOutput
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskDependency
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.util.AttributeTestUtil

import static org.gradle.api.attributes.Bundling.*
import static org.gradle.api.attributes.Category.*
import static org.gradle.api.attributes.DocsType.DOCS_TYPE_ATTRIBUTE
import static org.gradle.api.attributes.LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE
import static org.gradle.api.attributes.Usage.*
import static org.gradle.api.attributes.java.TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE
import static org.gradle.util.AttributeTestUtil.named

class DefaultJvmPluginServicesTest extends AbstractJvmPluginServicesTest {

    def configuresCompileClasspath() {
        def mutable = AttributeTestUtil.attributesFactory().mutable()
        def attrs = Mock(HasConfigurableAttributes)
        Action[] action = new Action[1]
        def config
        config = Stub(ConfigurationInternal) {
            beforeLocking(_) >> { args -> action[0] = args[0] }
            getAttributes() >> mutable
        }
        def javaCompileProvider = Stub(TaskProvider) {
                get() >> Stub(JavaCompile) {
                    getTargetCompatibility() >> '8'
                }
            }
        when:
        services.configureAsCompileClasspath(attrs)

        then:
        1 * attrs.getAttributes() >> mutable
        0 * _
        mutable.asMap() == [
            (CATEGORY_ATTRIBUTE): named(Category, LIBRARY),
            (USAGE_ATTRIBUTE): named(Usage, JAVA_API),
            (BUNDLING_ATTRIBUTE): named(Bundling, EXTERNAL)
        ]

        when:
        services.useDefaultTargetPlatformInference(config, javaCompileProvider)
        action[0].execute(config)

        then:
        mutable.asMap() == [
            (CATEGORY_ATTRIBUTE): named(Category, LIBRARY),
            (USAGE_ATTRIBUTE): named(Usage, JAVA_API),
            (BUNDLING_ATTRIBUTE): named(Bundling, EXTERNAL),
            (TARGET_JVM_VERSION_ATTRIBUTE): 8
        ]
    }

    def configuresRuntimeClasspath() {
        def mutable = AttributeTestUtil.attributesFactory().mutable()
        def attrs = Mock(HasConfigurableAttributes)

        when:
        services.configureAsRuntimeClasspath(attrs)

        then:
        1 * attrs.getAttributes() >> mutable
        0 * _
        mutable.asMap() == [
            (CATEGORY_ATTRIBUTE): named(Category, LIBRARY),
            (USAGE_ATTRIBUTE): named(Usage, JAVA_RUNTIME),
            (BUNDLING_ATTRIBUTE): named(Bundling, EXTERNAL),
            (LIBRARY_ELEMENTS_ATTRIBUTE): named(LibraryElements, LibraryElements.JAR)
        ]
    }

    def "can replace the artifacts of an outgoing configuration"() {
        def outgoing = Mock(ConfigurationPublications)
        def artifacts = Mock(PublishArtifactSet)
        def artifacts2 = Mock(PublishArtifactSet)
        def config = Stub(ConfigurationInternal) {
            getOutgoing() >> outgoing
            getExtendsFrom() >> [Stub(ConfigurationInternal) {
                getOutgoing() >> Stub(ConfigurationPublications) {
                    getArtifacts() >> artifacts2
                }
            }]
        }

        def artifact1 = Stub(TaskProvider)
        def artifact2 = Stub(File)

        when:
        services.replaceArtifacts(config, artifact1, artifact2)

        then:
        1 * outgoing.getArtifacts() >> artifacts
        1 * artifacts.clear()
        1 * artifacts2.clear()
        1 * outgoing.artifact(artifact1)
        1 * outgoing.artifact(artifact2)
        0 * _
    }

    def "can setup a classes directory secondary variant"() {
        def sourceSet = Mock(SourceSet)
        def apiElements = Mock(ConfigurationInternal)
        def outgoing = Mock(ConfigurationPublications)
        def variants = Mock(NamedDomainObjectContainer)
        def variant = Mock(ConfigurationVariantInternal)
        def attrs = AttributeTestUtil.attributesFactory().mutable()
        def output = Mock(DefaultSourceSetOutput)
        def classes = Stub(ConfigurableFileCollection) {
            getFiles() >> [
                Stub(File) {
                    getName() >> 'toto'
                }
            ]
        }

        when:
        services.configureClassesDirectoryVariant("apiElements", sourceSet)

        then:
        1 * configurations.all(_) >> { args -> args[0].execute(apiElements) }
        1 * apiElements.getName() >> 'apiElements'
        1 * apiElements.getOutgoing() >> outgoing
        1 * outgoing.getVariants() >> variants
        1 * variants.maybeCreate('classes') >> variant
        1 * variant.getAttributes() >> attrs
        1 * variant.artifactsProvider(_) >> {
            def artifacts = it[0].create()
            assert artifacts.size() == 1
            PublishArtifact artifact = artifacts[0]
            assert artifact.name == 'toto'
        }
        _ * sourceSet.getOutput() >> output
        1 * output.getClassesDirs() >> classes
        1 * output.getClassesContributors() >> Stub(TaskDependency)
        0 * _
    }

    def "configures attributes"() {
        def attrs = AttributeTestUtil.attributesFactory().mutable()

        when:
        services.configureAttributes(Stub(HasConfigurableAttributes) { getAttributes() >> attrs }) {
            it.library()
        }

        then:
        attrs.asMap() == [
            (CATEGORY_ATTRIBUTE): named(Category, LIBRARY)
        ]

        when:
        attrs = AttributeTestUtil.attributesFactory().mutable()
        services.configureAttributes(Stub(HasConfigurableAttributes) { getAttributes() >> attrs }) {
            it.platform()
        }

        then:
        attrs.asMap() == [
            (CATEGORY_ATTRIBUTE): named(Category, REGULAR_PLATFORM)
        ]

        when:
        attrs = AttributeTestUtil.attributesFactory().mutable()
        services.configureAttributes(Stub(HasConfigurableAttributes) { getAttributes() >> attrs }) {
            it.library('foo')
        }

        then:
        attrs.asMap() == [
            (CATEGORY_ATTRIBUTE): named(Category, LIBRARY),
            (LIBRARY_ELEMENTS_ATTRIBUTE): named(LibraryElements, 'foo')
        ]

        when:
        services.configureAttributes(Stub(HasConfigurableAttributes) { getAttributes() >> attrs }) {
            it.asJar()
        }

        then:
        attrs.asMap() == [
            (CATEGORY_ATTRIBUTE): named(Category, LIBRARY),
            (LIBRARY_ELEMENTS_ATTRIBUTE): named(LibraryElements, LibraryElements.JAR)
        ]

        when:
        attrs = AttributeTestUtil.attributesFactory().mutable()
        services.configureAttributes(Stub(HasConfigurableAttributes) { getAttributes() >> attrs }) {
            it.documentation("foo")
        }

        then:
        attrs.asMap() == [
            (CATEGORY_ATTRIBUTE): named(Category, DOCUMENTATION),
            (DOCS_TYPE_ATTRIBUTE): named(DocsType, 'foo')
        ]

        when:
        attrs = AttributeTestUtil.attributesFactory().mutable()
        services.configureAttributes(Stub(HasConfigurableAttributes) { getAttributes() >> attrs }) {
            it.apiUsage()
        }

        then:
        attrs.asMap() == [
            (USAGE_ATTRIBUTE): named(Usage, JAVA_API)
        ]

        when:
        attrs = AttributeTestUtil.attributesFactory().mutable()
        services.configureAttributes(Stub(HasConfigurableAttributes) { getAttributes() >> attrs }) {
            it.runtimeUsage()
        }

        then:
        attrs.asMap() == [
            (USAGE_ATTRIBUTE): named(Usage, JAVA_RUNTIME)
        ]

        when:
        services.configureAttributes(Stub(HasConfigurableAttributes) { getAttributes() >> attrs }) {
            it.withExternalDependencies()
        }

        then:
        attrs.asMap() == [
            (USAGE_ATTRIBUTE): named(Usage, JAVA_RUNTIME),
            (BUNDLING_ATTRIBUTE): named(Bundling, EXTERNAL)
        ]

        when:
        services.configureAttributes(Stub(HasConfigurableAttributes) { getAttributes() >> attrs }) {
            it.withEmbeddedDependencies()
        }

        then:
        attrs.asMap() == [
            (USAGE_ATTRIBUTE): named(Usage, JAVA_RUNTIME),
            (BUNDLING_ATTRIBUTE): named(Bundling, EMBEDDED)
        ]

        when:
        services.configureAttributes(Stub(HasConfigurableAttributes) { getAttributes() >> attrs }) {
            it.withShadowedDependencies()
        }

        then:
        attrs.asMap() == [
            (USAGE_ATTRIBUTE): named(Usage, JAVA_RUNTIME),
            (BUNDLING_ATTRIBUTE): named(Bundling, SHADOWED)
        ]

        when:
        services.configureAttributes(Stub(HasConfigurableAttributes) { getAttributes() >> attrs }) {
            it.withTargetJvmVersion(15)
        }

        then:
        attrs.asMap() == [
            (USAGE_ATTRIBUTE): named(Usage, JAVA_RUNTIME),
            (BUNDLING_ATTRIBUTE): named(Bundling, SHADOWED),
            (TARGET_JVM_VERSION_ATTRIBUTE): 15
        ]
    }

}
