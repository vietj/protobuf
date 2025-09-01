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

import com.google.common.io.LineReader;
import junit.framework.AssertionFailedError;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(Parameterized.class)
public class RunConformanceTest {

  @Parameterized.Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() throws Exception {
    List<Object[]> list = new ArrayList<>();
    try (InputStream is = ConformanceTest.class.getResourceAsStream("/all.txt")) {
      LineReader reader = new LineReader(new InputStreamReader(is));
      while (true) {
        String line = reader.readLine();
        if (line == null) {
          break;
        }
        if (line.contains("Proto2")) {
          continue;
        }
        line = line.replace("[", "\\[");
        line = line.replace("]", "\\]");
        list.add(new Object[] { line });
      }
    }
    return list;
  }

  private final String test;

  public RunConformanceTest(String test) {
    this.test = test;
  }

  @Ignore("Requires the project to be build with the conformance profile")
  @Test
  public void testConformance() throws Exception {

    String sprop = System.getProperty("maven.project.basedir");
    if (sprop == null) {
      throw new AssertionFailedError("Was expecting maven.project.basedir system property to be set");
    }
    File baseDir = new File(sprop);
    if (!baseDir.exists() || !baseDir.isDirectory()) {
      throw new AssertionFailedError();
    }

    File conformanceTestRunner = new File(baseDir, "conformance_test_runner");
    if (!conformanceTestRunner.exists() || !conformanceTestRunner.isFile()) {
      throw new AssertionFailedError();
    }


    File conformance = new File(baseDir, "conformance.sh");
    if (!conformance.exists() || !conformance.isFile()) {
      throw new AssertionFailedError();
    }

    ProcessBuilder processBuilder = new ProcessBuilder(
      conformanceTestRunner.getAbsolutePath(),
      "--maximum_edition", "PROTO3",
      "--output_dir", ".",
      "--test", test,
      conformance.getAbsolutePath());
    processBuilder.redirectOutput();
    Process process = processBuilder.start();
    InputStream out = process.getErrorStream();
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    Thread th = new Thread(() -> {
      byte[] buffer = new byte[256];
      try {
        while (true) {
          int amount = out.read(buffer, 0, 256);
          if (amount == -1) {
            break;
          }
          baos.write(buffer, 0, amount);
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });
    th.start();
    int i = process.waitFor();
    th.join();

    String s = baos.toString();
    Pattern pattern = Pattern.compile("CONFORMANCE SUITE PASSED: ([0-9]+) successes, ([0-9]+) skipped, ([0-9]+) expected failures, ([0-9]+) unexpected failures.");

    Matcher matcher = pattern.matcher(s);
    Assert.assertTrue(matcher.find());
    int successes = Integer.parseInt(matcher.group(1));
    int skipped = Integer.parseInt(matcher.group(2));
    int expectedFailures = Integer.parseInt(matcher.group(3));
    int unexpectedFailures = Integer.parseInt(matcher.group(4));

    if (expectedFailures > 0) {
      Assert.fail();
    }

    if (successes == 0) {
      Pattern p = Pattern.compile("WARNING, test=([^:]+):(.*)");
      Matcher m = p.matcher(s);
      if (m.find()) {
        Assert.fail("Unexpected warning " + m.group(1) + ": " + m.group(2));
      }
    }
  }
}
