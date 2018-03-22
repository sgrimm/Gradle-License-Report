package com.github.jk1.license.filter

import com.github.jk1.license.ProjectBuilder
import com.github.jk1.license.ProjectData
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import static com.github.jk1.license.ProjectBuilder.json
import static com.github.jk1.license.ProjectDataFixture.*

class LicenseBundleNormalizerSpec extends Specification {
    @Rule
    TemporaryFolder testProjectDir = new TemporaryFolder()
    File normalizerFile

    ProjectBuilder builder = new ProjectBuilder()

    def setup() {
        testProjectDir.create()

        normalizerFile = testProjectDir.newFile('test-normalizer-config.json')
        normalizerFile << """
            {
              "bundles" : [
                { "bundleName" : "apache2", "licenseName" : "Apache License, Version 2.0", "licenseUrl" : "https://www.apache.org/licenses/LICENSE-2.0" }
              ]"""

        // copy apache2 license file
        def apache2LicenseFile = new File(getClass().getResource('/apache2-license.txt').toURI())
        new File(testProjectDir.root, "apache2-license.txt") << apache2LicenseFile.text
    }

    def "normalize license of manifest (when stored as name)"() {
        normalizerFile << """,
            "transformationRules" : [
                { "bundleName" : "apache2", "licenseNamePattern" : "Apache 2.*" }
              ]
            }"""

        ProjectData projectData = builder.project {
            configuration("runtime") {
                module("mod1") {
                    manifest("mani1") {
                        license("Apache 2")
                    }
                }
            }
        }
        ProjectData expected = builder.project {
            configuration("runtime") {
                module("mod1") {
                    manifest("mani1") {
                        license("Apache License, Version 2.0")
                    }
                }
            }
        }

        when:
        def result = newNormalizer().filter(projectData)

        then:
        json(result) == json(expected)
    }

    def "normalize the manifests license or to the appropriate bundle-license-name"() {
        normalizerFile << """,
            "transformationRules" : [
                { "bundleName" : "apache2", "licenseUrlPattern" : "http://www.apache.org/licenses/LICENSE-2.0.*" }
              ]
            }"""

        ProjectData projectData = builder.project {
            configuration("runtime") {
                module("mod1") {
                    manifest("mani1") {
                        license("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
            }
        }
        ProjectData expected = builder.project {
            configuration("runtime") {
                module("mod1") {
                    manifest("mani1") {
                        license("Apache License, Version 2.0")
                    }
                }
            }
        }

        when:
        def result = newNormalizer().filter(projectData)

        then:
        json(result) == json(expected)
    }

    def "after normalisation, all poms of all configurations are normalized"() {
        normalizerFile << """,
            "transformationRules" : [
                { "bundleName" : "apache2", "licenseNamePattern" : "Apache 2.*" }
              ]
            }"""

        ProjectData projectData = builder.project {
            configuration("runtime") {
                module("mod1") {
                    pom("pom1") {
                        license(APACHE2_LICENSE(), name: "Apache 2")
                    }
                }
            }
            configuration("test") {
                module("mod1") {
                    pom("pom1") {
                        license(APACHE2_LICENSE(), name: "Apache 2")
                    }
                }
            }
        }
        ProjectData expected = builder.project {
            configuration("runtime") {
                module("mod1") {
                    pom("pom1") {
                        license(APACHE2_LICENSE())
                    }
                }
            }
            configuration("test") {
                module("mod1") {
                    pom("pom1") {
                        license(APACHE2_LICENSE())
                    }
                }
            }
        }

        when:
        def result = newNormalizer().filter(projectData)

        then:
        json(result) == json(expected)
    }

    def "after normalisation, all manifests of all configurations are normalized"() {
        normalizerFile << """,
            "transformationRules" : [
                { "bundleName" : "apache2", "licenseNamePattern" : "Apache 2.*" }
              ]
            }"""

        ProjectData projectData = builder.project {
            configuration("runtime") {
                module("mod1") {
                    manifest("mani1") {
                        license("Apache 2")
                    }
                }
            }
            configuration("test") {
                module("mod1") {
                    manifest("mani1") {
                        license("Apache 2")
                    }
                }
            }
        }
        ProjectData expected = builder.project {
            configuration("runtime") {
                module("mod1") {
                    manifest("mani1") {
                        license("Apache License, Version 2.0")
                    }
                }
            }
            configuration("test") {
                module("mod1") {
                    manifest("mani1") {
                        license("Apache License, Version 2.0")
                    }
                }
            }
        }

        when:
        def result = newNormalizer().filter(projectData)

        then:
        json(result) == json(expected)
    }

    private LicenseBundleNormalizer newNormalizer() {
        new LicenseBundleNormalizer(normalizerFile.absolutePath)
    }
}
