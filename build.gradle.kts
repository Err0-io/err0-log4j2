import org.gradle.api.tasks.testing.logging.TestLogEvent.*

plugins {
  `java-library`
  `maven-publish`
  signing
}

group = "io.err0"
version = "1.0.4"

repositories {
  mavenCentral()
}

val junitJupiterVersion = "5.7.0"

val launcherClassName = "io.err0.log4j2.Main"

dependencies {
  //implementation(platform("io.vertx:vertx-stack-depchain:$vertxVersion"))
  //implementation("io.vertx:vertx-web-client")
  //testImplementation("io.vertx:vertx-junit5")
  testImplementation("org.junit.jupiter:junit-jupiter:$junitJupiterVersion")

// https://mvnrepository.com/artifact/com.squareup.okhttp3/okhttp
  implementation("com.squareup.okhttp3:okhttp:4.10.0")
  implementation("com.google.code.gson:gson:2.8.7")

  // the below are uncommented to allow me to use autocomplete to edit the
  // java unit test cases, please comment these out as the tests don't actually
  // depend on these:
  // testImplementation("org.apache.logging.log4j:log4j-core:2.14.1")
  // testImplementation("org.slf4j:slf4j-api:1.7.31")

  // https://mvnrepository.com/artifact/org.apache.logging.log4j/log4j-core
  compileOnly("org.apache.logging.log4j:log4j-core:2.17.2")

  // to silence library included slf4j noises:
  //
  // https://mvnrepository.com/artifact/org.slf4j/slf4j-api
  compileOnly("org.slf4j:slf4j-api:1.7.36")
  // https://mvnrepository.com/artifact/org.slf4j/slf4j-simple
  compileOnly("org.slf4j:slf4j-simple:1.7.36")
}

java {
  sourceCompatibility = JavaVersion.VERSION_1_8
  targetCompatibility = JavaVersion.VERSION_1_8
  withJavadocJar()
  withSourcesJar()
}

tasks.withType<Test> {
  useJUnitPlatform()
  testLogging {
    events = setOf(PASSED, SKIPPED, FAILED)
  }
}

publishing {
  publications {
    create<MavenPublication>("mavenJava") {
      from(components["java"])
      pom {
        name.set("err0-log4j2")
        description.set("Connector for err0, log4j2")
        url.set("https://github.com/Err0-io/err0-log4j2")
        licenses {
          license {
            name.set("MIT license")
            url.set("https://github.com/Err0-io/err0-log4j2/blob/main/LICENSE.txt")
          }
        }
        developers {
          developer {
            id.set("err0package")
            name.set("Err0.io packages")
            email.set("package@err0.io")
          }
        }
        versionMapping {
          //usage("java-api") {
          //  fromResolutionOf("runtimeClasspath")
          //}
          usage("java-runtime") {
            fromResolutionResult()
          }
        }
        scm {
          connection.set("scm:git:https://github.com/Err0-io/err0-log4j2.git")
          developerConnection.set("scm:git:ssh://git@github.com:Err0-io/err0-log4j2.git")
          url.set("https://github.com/Err0-io/err0-log4j2")
        }
      }
    }
  }
  repositories {
    maven {
      name = "sonatype"
      setUrl("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
      credentials {
        username = project.property("sonatypeUsername") as String?
        password = project.property("sonatypePassword") as String?
      }
    }
  }
}

signing {
    sign(publishing.publications)
}

tasks.javadoc {
  if (JavaVersion.current().isJava9Compatible) {
    (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
  }
}