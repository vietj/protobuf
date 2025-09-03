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
package com.julienviet.protobuf.tests.codegen.validation;

import com.julienviet.protobuf.lang.ProtoField;
import com.julienviet.protobuf.lang.ProtoMessage;

@ProtoMessage
public class DuplicateFieldNumber {

  @ProtoField(number = 1)
  public String getFoo() {
    throw new UnsupportedOperationException();
  }

  public void setFoo(String s) {
  }

  @ProtoField(number = 1)
  public String getBar() {
    throw new UnsupportedOperationException();
  }

  public void setBar(String s) {
  }
}
