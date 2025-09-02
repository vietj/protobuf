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
import com.julienviet.protobuf.schema.TypeID;

@ProtoMessage
public class ProtoDataTypes {

  private int int32Field;
  private int uint32Field;
  private int sint32Field;
  private int fixed32Field;
  private int sfixed32Field;
  private long int64Field;
  private long uint64Field;
  private long sint64Field;
  private long fixed64Field;
  private long sfixed64Field;

  @ProtoField(number = 1, protoName = "int32_field", type = TypeID.INT32)
  public int getInt32Field() {
    return int32Field;
  }

  public void setInt32Field(int i) {
    this.int32Field = i;
  }

  @ProtoField(number = 2, protoName = "uint32_field", type = TypeID.UINT32)
  public int getUint32Field() {
    return uint32Field;
  }

  public void setUint32Field(int i) {
    this.uint32Field = i;
  }

  @ProtoField(number = 3, protoName = "sint32_field", type = TypeID.SINT32)
  public int getSint32Field() {
    return sint32Field;
  }

  public void setSint32Field(int i) {
    this.sint32Field = i;
  }

  @ProtoField(number = 4, protoName = "fixed32_field", type = TypeID.FIXED32)
  public int getFixed32Field() {
    return fixed32Field;
  }

  public void setFixed32Field(int i) {
    this.fixed32Field = i;
  }

  @ProtoField(number = 5, protoName = "sfixed32_field", type = TypeID.SFIXED32)
  public int getSfixed32Field() {
    return sfixed32Field;
  }

  public void setSfixed32Field(int i) {
    this.sfixed32Field = i;
  }

  @ProtoField(number = 6, protoName = "int64_field", type = TypeID.INT64)
  public long getInt64Field() {
    return int64Field;
  }

  public void setInt64Field(long i) {
    this.int64Field = i;
  }

  @ProtoField(number = 7, protoName = "uint64_field", type = TypeID.UINT64)
  public long getUint64Field() {
    return uint64Field;
  }

  public void setUint64Field(long i) {
    this.uint64Field = i;
  }

  @ProtoField(number = 8, protoName = "sint64_field", type = TypeID.SINT64)
  public long getSint64Field() {
    return sint64Field;
  }

  public void setSint64Field(long i) {
    this.sint64Field = i;
  }

  @ProtoField(number = 9, protoName = "fixed64_field", type = TypeID.FIXED64)
  public long getFixed64Field() {
    return fixed64Field;
  }

  public void setFixed64Field(long i) {
    this.fixed64Field = i;
  }

  @ProtoField(number = 10, protoName = "sfixed64_field", type = TypeID.SFIXED64)
  public long getSfixed64Field() {
    return sfixed64Field;
  }

  public void setSfixed64Field(long i) {
    this.sfixed64Field = i;
  }
}
