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
package com.julienviet.protobuf.tests.conformance;

import com.julienviet.protobuf.conformance.Runner;
import junit.framework.AssertionFailedError;
import org.junit.Ignore;
import org.junit.Test;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Attempt to run conformance test using docker + socat but does not really work
 */
public class ConformanceTest {

  private ImageFromDockerfile testBuildDockerFile() {
    String baseDirProp = System.getProperty("maven.project.basedir");
    assertNotNull("Missing project basedir", baseDirProp);
    File baseDir = new File(baseDirProp);
    assertTrue(baseDir.exists());
    assertTrue(baseDir.isDirectory());
    File dockerFile = new File(baseDir, "src" + File.separator + "test" + File.separator + "docker" + File.separator + "conformance" + File.separator + "DockerFile");
    assertTrue(dockerFile.exists());
    assertTrue(dockerFile.isFile());
    File conformanceFile = new File(baseDir, "src" + File.separator + "test" + File.separator + "docker" + File.separator + "conformance" + File.separator +  "conformance.sh");
    assertTrue(conformanceFile.exists());
    assertTrue(conformanceFile.isFile());
    File knownFailuresFile = new File(baseDir, "src" + File.separator + "test" + File.separator + "docker" + File.separator + "conformance" + File.separator +  "known_failures.txt");
    assertTrue(conformanceFile.exists());
    assertTrue(conformanceFile.isFile());
    return new ImageFromDockerfile("protobuf/conformance", false)
      .withDockerfile(dockerFile.toPath())
      .withFileFromFile("conformance.sh", conformanceFile)
      .withFileFromFile("known_failures.txt", knownFailuresFile);
  }

  private void startServerSocket(CountDownLatch bind, CountDownLatch done) throws Exception {
    try (ServerSocket server = new ServerSocket()) {
      server.bind(new InetSocketAddress("localhost", 4000));
      bind.countDown();
      Socket socket = server.accept();
      Runner.run(socket.getInputStream(), socket.getOutputStream());
      done.countDown();
    }
  }

  @Ignore
  @Test
  public void testWithServerSocket() throws Exception {
    // Start TCP server
    CountDownLatch latch1 = new CountDownLatch(1);
    CountDownLatch latch2 = new CountDownLatch(1);
    Thread server = new Thread(() -> {
      try {
        startServerSocket(latch1, latch2);
      } catch (Exception e) {
        e.printStackTrace(System.out);
      }
    });
    server.start();
    latch1.await(1, TimeUnit.MINUTES);
    System.out.println("Server listen");

    // ImageFromDockerfile img = testBuildDockerFile();
/*
    Testcontainers.exposeHostPorts(4000);
    try (GenericContainer<?> container = new GenericContainer<>("protobuf/conformance")) {
      StringBuilder sb = new StringBuilder();
      container.withLogConsumer(frame -> {
//        sb.append(frame.getUtf8String());
        System.out.print(frame.getUtf8String());
      });
      System.out.println("Starting test container");
      container.start();
      System.out.println("Test container running");
    } finally {
      Testcontainers.exposeHostPorts();
    }
*/

    latch2.await(1, TimeUnit.MINUTES);
    System.out.println("Done");
    server.join();
  }

  @Ignore
  @Test
  public void testConformance() throws Exception {
    ImageFromDockerfile img = testBuildDockerFile();
    try (GenericContainer<?> container = new GenericContainer<>(img)) {
      StringBuilder sb = new StringBuilder();
      container.withLogConsumer(frame -> {
        sb.append(frame.getUtf8String());
      });
      container.addExposedPort(4000);
      container.start();

      int actualPort = container.getMappedPort(4000);
      for (int i = 0;i < 10;i++) {
        Thread.sleep(100);
        try (Socket so = new Socket()) {
          so.connect(new InetSocketAddress("localhost", actualPort));
          InputStream in = so.getInputStream();
          OutputStream out = so.getOutputStream();
          Runner.run(in, out);
          // Check if we have a result
          if (checkResult(sb.toString())) {
            break;
          }
        } catch (ConnectException ex) {
          break;
        }
       }
    }
  }

  private boolean checkResult(String s) {
    Pattern pattern = Pattern.compile("CONFORMANCE SUITE (FAILED|PASSED): ([0-9]+) successes, ([0-9]+) skipped, ([0-9]+) expected failures, ([0-9]+) unexpected failures.");
    Matcher matcher = pattern.matcher(s);
    if (matcher.find()) {
      String result = matcher.group(1);
      int successes = Integer.parseInt(matcher.group(2));
      int skipped = Integer.parseInt(matcher.group(3));
      int expectedFailures = Integer.parseInt(matcher.group(4));
      int unexpectedFailures = Integer.parseInt(matcher.group(5));
      if (result.equals("FAILED")) {
        throw new AssertionFailedError("Conformance failure: " + matcher.group(0));
      }
      return true;
    }
    return false;
  }
}
