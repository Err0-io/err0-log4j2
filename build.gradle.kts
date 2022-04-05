import org.gradle.api.tasks.testing.logging.TestLogEvent.*

plugins {
  java
  application
}

group = "io.err0.log4j2"
version = "1.0.0-SNAPSHOT"

repositories {
  mavenCentral()
}

val junitJupiterVersion = "5.7.0"

val launcherClassName = "io.err0.log4j2.Main"

application {
  mainClass.set(launcherClassName)
}

dependencies {
  // https://mvnrepository.com/artifact/org.apache.ant/ant-launcher
  implementation("org.apache.ant:ant-launcher:1.10.11") //needed by shadow

  //implementation(platform("io.vertx:vertx-stack-depchain:$vertxVersion"))
  //implementation("io.vertx:vertx-web-client")
  //testImplementation("io.vertx:vertx-junit5")
  testImplementation("org.junit.jupiter:junit-jupiter:$junitJupiterVersion")

  // https://mvnrepository.com/artifact/org.apache.httpcomponents.client5/httpclient5
  implementation("org.apache.httpcomponents.client5:httpclient5:5.1")

  // the below are uncommented to allow me to use autocomplete to edit the
  // java unit test cases, please comment these out as the tests don't actually
  // depend on these:
  // testImplementation("org.apache.logging.log4j:log4j-core:2.14.1")
  // testImplementation("org.slf4j:slf4j-api:1.7.31")

  // https://mvnrepository.com/artifact/org.apache.logging.log4j/log4j-core
  implementation("org.apache.logging.log4j:log4j-core:2.17.2")
}

java {
  sourceCompatibility = JavaVersion.VERSION_1_8
  targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<Test> {
  useJUnitPlatform()
  testLogging {
    events = setOf(PASSED, SKIPPED, FAILED)
  }
}