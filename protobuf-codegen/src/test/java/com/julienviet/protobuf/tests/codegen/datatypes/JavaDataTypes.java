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
package com.julienviet.protobuf.tests.codegen.datatypes;

import com.julienviet.protobuf.lang.ProtoField;
import com.julienviet.protobuf.lang.ProtoMessage;

@ProtoMessage
public class JavaDataTypes {

  private int intField;
  private long longField;
  private float floatField;
  private double doubleField;
  private boolean booleanField;
  private String stringField;
  private byte[] binaryField;
  private TestEnum enumField;

  @ProtoField(number = 1, protoName = "int_field")
  public int getIntField() {
    return intField;
  }

  public void setIntField(int i) {
    this.intField = i;
  }

  @ProtoField(number = 2, protoName = "long_field")
  public long getLongField() {
    return longField;
  }

  public void setLongField(long l) {
    this.longField = l;
  }

  @ProtoField(number = 3, protoName = "float_field")
  public float getFloatField() {
    return floatField;
  }

  public void setFloatField(float f) {
    this.floatField = f;
  }

  @ProtoField(number = 4, protoName = "double_field")
  public double getDoubleField() {
    return doubleField;
  }

  public void setDoubleField(double d) {
    this.doubleField = d;
  }

  @ProtoField(number = 5, protoName = "boolean_field")
  public boolean getBooleanField() {
    return booleanField;
  }

  public void setBooleanField(boolean b) {
    this.booleanField = b;
  }

  @ProtoField(number = 6, protoName = "enum_field")
  public TestEnum getEnumField() {
    return enumField;
  }

  public void setEnumField(TestEnum e) {
    this.enumField = e;
  }

  @ProtoField(number = 7, protoName = "string_field")
  public String getStringField() {
    return stringField;
  }

  public void setStringField(String s) {
    this.stringField = s;
  }

  @ProtoField(number = 8, protoName = "binary_field")
  public byte[] getBinaryField() {
    return binaryField;
  }

  public void setBinaryField(byte[] bytes) {
    this.binaryField = bytes;
  }

}
