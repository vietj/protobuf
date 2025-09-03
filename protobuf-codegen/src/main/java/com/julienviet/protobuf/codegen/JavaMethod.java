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
package com.julienviet.protobuf.codegen;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeMirror;

public class JavaMethod {

  static JavaMethod createGet(String propertyName, String readerMethod, String writerMethod, TypeMirror type, ExecutableElement element) {
    return new JavaMethod(propertyName, readerMethod, writerMethod, type, element);
  }

  static JavaMethod createSet(String name, String readerMethod, String writerMethod, TypeMirror type, ExecutableElement element) {
    return new JavaMethod(name, readerMethod, writerMethod, type, element);
  }

  static JavaMethod createIs(String name, String readerMethod, String writerMethod, TypeMirror type, ExecutableElement element) {
    return new JavaMethod(name, readerMethod, writerMethod, type, element);
  }

  final String propertyName;
  final String readerMethod;
  final String writerMethod;
  final TypeMirror type;
  final ExecutableElement element;

  public JavaMethod(String propertyName, String readerMethod, String writerMethod, TypeMirror type, ExecutableElement element) {
    this.propertyName = propertyName;
    this.readerMethod = readerMethod;
    this.writerMethod = writerMethod;
    this.type = type;
    this.element = element;
  }
}
