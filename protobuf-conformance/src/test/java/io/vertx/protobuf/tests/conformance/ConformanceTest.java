/*
 * Copyright (C) 2025 Julien Viet
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package io.vertx.protobuf.tests.conformance;

import junit.framework.AssertionFailedError;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.ToStringConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.MountableFile;

import java.io.File;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Conformance test using Docker and testcontainers.
 * <p>
 * This test builds a Docker image with the protobuf conformance test runner,
 * then executes it against our implementation to verify protocol buffer
 * serialization/deserialization compliance.
 * <p>
 * Prerequisites:
 * <ul>
 *   <li>Docker must be running</li>
 *   <li>The project must be built (mvn package) to generate the conformance-runner.jar</li>
 * </ul>
 */
public class ConformanceTest {

  private static final Pattern CONFORMANCE_RESULT_PATTERN = Pattern.compile("CONFORMANCE SUITE (FAILED|PASSED): (\\d+) successes?, (\\d+) skipped, (\\d+) expected failures?, (\\d+) unexpected failures?\\.");

  private static File baseDir;
  private static File dockerFile;
  private static File conformanceShFile;
  private static File knownFailuresFile;
  private static File conformanceJarFile;

  @BeforeClass
  public static void setup() {
    String baseDirProp = System.getProperty("maven.project.basedir");
    assertNotNull("Missing project basedir system property", baseDirProp);
    baseDir = new File(baseDirProp);
    assertTrue("Project basedir does not exist: " + baseDir, baseDir.exists());
    assertTrue("Project basedir is not a directory: " + baseDir, baseDir.isDirectory());

    dockerFile = new File(baseDir, "src/test/docker/conformance/Dockerfile");
    conformanceShFile = new File(baseDir, "src/test/docker/conformance/conformance.sh");
    knownFailuresFile = new File(baseDir, "known_failures.txt");
    conformanceJarFile = new File(baseDir, "target/conformance-runner.jar");
  }

  private void checkFilesExist() {
    assertTrue("Dockerfile not found: " + dockerFile, dockerFile.exists());
    assertTrue("conformance.sh not found: " + conformanceShFile, conformanceShFile.exists());
    assertTrue("known_failures.txt not found: " + knownFailuresFile, knownFailuresFile.exists());
    assertTrue("conformance-runner.jar not found (run 'mvn package' first): " + conformanceJarFile, conformanceJarFile.exists());
  }

  private ImageFromDockerfile buildDockerImage() {
    return new ImageFromDockerfile("protobuf/conformance", false)
      .withDockerfile(dockerFile.toPath())
      .withFileFromFile("conformance.sh", conformanceShFile);
  }

  /**
   * Runs the full conformance test suite using Docker.
   * <p>
   * This test:
   * <ol>
   *   <li>Builds a Docker image containing the protobuf conformance test runner</li>
   *   <li>Copies our conformance-runner.jar and known_failures.txt into the container</li>
   *   <li>Executes the conformance suite</li>
   *   <li>Parses the output to verify all tests pass (excluding known failures)</li>
   * </ol>
   */
  @Test
  public void testConformance() throws Exception {
    Assume.assumeTrue("Docker is not available", isDockerAvailable());

    checkFilesExist();

    ImageFromDockerfile image = buildDockerImage();
    ToStringConsumer logConsumer = new ToStringConsumer();

    try (GenericContainer<?> container = new GenericContainer<>(image)) {
      container.withCopyToContainer(MountableFile.forHostPath(conformanceJarFile.toPath()), "/conformance-runner.jar");
      container.withCopyToContainer(MountableFile.forHostPath(knownFailuresFile.toPath()), "/known_failures.txt");
      container.withLogConsumer(logConsumer);
      container.waitingFor(Wait.forLogMessage(".*CONFORMANCE SUITE.*", 1)
        .withStartupTimeout(Duration.ofMinutes(10)));

      container.start();

      // Wait for container to finish
      while (container.isRunning()) {
        Thread.sleep(100);
      }
    }

    String output = logConsumer.toUtf8String();
    System.out.println(output);

    checkConformanceResult(output);
  }

  private boolean isDockerAvailable() {
    try {
      DockerClientFactory.instance().client();
      return true;
    } catch (Exception e) {
      System.err.println("Docker is not available: " + e.getMessage());
      return false;
    }
  }

  private void checkConformanceResult(String output) {
    Matcher matcher = CONFORMANCE_RESULT_PATTERN.matcher(output);

    if (!matcher.find()) {
      fail("Could not find conformance suite result in output:\n" + output);
    }

    String result = matcher.group(1);
    int successes = Integer.parseInt(matcher.group(2));
    int skipped = Integer.parseInt(matcher.group(3));
    int expectedFailures = Integer.parseInt(matcher.group(4));
    int unexpectedFailures = Integer.parseInt(matcher.group(5));

    System.out.printf("Conformance Results: %s%n", result);
    System.out.printf("  Successes: %d%n", successes);
    System.out.printf("  Skipped: %d%n", skipped);
    System.out.printf("  Expected Failures: %d%n", expectedFailures);
    System.out.printf("  Unexpected Failures: %d%n", unexpectedFailures);

    if ("FAILED".equals(result)) {
      throw new AssertionFailedError(String.format(
        "Conformance suite failed with %d unexpected failures. Output:\n%s",
        unexpectedFailures, output));
    }

    assertTrue("No successful tests were run", successes > 0);
  }
}
