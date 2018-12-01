package org.camunda.bpm.camel.itest;

import org.ops4j.pax.exam.ConfigurationFactory;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;

import java.io.File;

import static org.ops4j.pax.exam.CoreOptions.*;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.*;

/**
 * This class sets up the common environment for all integration tests.
 * <p/>
 * This class is also referenced as default configuration in<br/>
 * <code>
 * src/test/resources/META-INF/services/org.ops4j.pax.exam.ConfigurationFactory
 * </code>
 *
 * @author Ronny Bräunlich
 * @author Fedor Resnyanskiy
 */
public class OSGiTestEnvironment implements ConfigurationFactory {

  @Override
  public Option[] createConfiguration() {
    return
      options(
        karafDistributionConfiguration()
          .frameworkUrl(
            maven()
              .groupId("org.apache.karaf")
              .artifactId("apache-karaf")
              .type("zip")
              .versionAsInProject())
          .useDeployFolder(false)
          .unpackDirectory(new File("target/exam/")),
        features(
          maven()
            .groupId("org.camunda.bpm.extension.osgi")
            .artifactId("camunda-bpm-karaf-feature")
            .classifier("features")
            .type("xml")
            .versionAsInProject(),
          "camunda-bpm-karaf-feature-minimal"
        ), features(
          maven()
            .groupId("org.camunda.bpm.extension.osgi")
            .artifactId("camunda-bpm-karaf-feature")
            .classifier("features")
            .type("xml")
            .versionAsInProject(),
          "camunda-bpm-karaf-feature-minimal"
        ),
        features(
          maven()
            .groupId("org.apache.camel.karaf")
            .artifactId("apache-camel")
            .classifier("features")
            .type("xml")
            .version("2.23.0"),
          "camel"
        ),
        keepRuntimeFolder(),
        mavenBundle("org.camunda.bpm.extension.osgi", "camunda-bpm-blueprint-wrapper").versionAsInProject(),
        mavenBundle("org.camunda.bpm.extension.camel", "camunda-bpm-camel-common").versionAsInProject(),
        mavenBundle("org.camunda.bpm.extension.camel", "camunda-bpm-camel-blueprint").versionAsInProject(),

//      logLevel(LogLevelOption.LogLevel.TRACE),
//        debugConfiguration("5005", true),

        CoreOptions.junitBundles()
      );
  }


}
