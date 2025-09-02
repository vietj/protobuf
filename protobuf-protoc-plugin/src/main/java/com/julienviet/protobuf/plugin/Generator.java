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
package com.julienviet.protobuf.plugin;

import com.salesforce.jprotoc.ProtocPlugin;
import com.julienviet.protobuf.extension.ExtensionProto;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.util.*;
import java.util.concurrent.Callable;

@Command(
  name = "protobuf-generator",
  mixinStandardHelpOptions = true,
  version = "protobuf-generator 1.0",
  description = "Generates Protobuf code from proto files."
)
public class Generator implements Callable<Integer> {
  @Override
  public Integer call() {
    GeneratorImpl generator = new GeneratorImpl();
    ProtocPlugin.generate(List.of(generator), List.of(ExtensionProto.typeInterop));
    return 0;
  }

  public static void main(String[] args) {
    int exitCode = new CommandLine(new Generator()).execute(args);
    System.exit(exitCode);
  }
}
