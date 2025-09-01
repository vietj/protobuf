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

import com.google.protobuf.TypeRegistry;
import com.google.protobuf.util.JsonFormat;
import com.google.protobuf_test_messages.proto3.MessageLiteral;
import com.google.protobuf_test_messages.proto3.ProtoReader;
import com.google.protobuf_test_messages.proto3.ProtoWriter;
import com.google.protobuf_test_messages.proto3.TestAllTypesProto3;
import com.google.protobuf_test_messages.proto3.TestMessagesProto3;
import com.julienviet.protobuf.core.ProtobufReader;
import com.julienviet.protobuf.core.ProtobufWriter;
import com.julienviet.protobuf.core.json.ProtoJsonReader;
import com.julienviet.protobuf.core.json.ProtoJsonWriter;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.StringWriter;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ConformanceTest {

  private TypeRegistry typeRegistry;

  public ConformanceTest() {
    typeRegistry =
      TypeRegistry.newBuilder()
        .add(TestMessagesProto3.TestAllTypesProto3.getDescriptor())
        .add(com.google.protobuf_test_messages.proto3.TestMessagesProto3.TestAllTypesProto3.getDescriptor())
        .build();
  }

  @Ignore
  @Test
  public void testJsonOutput() throws Exception {
    byte[] bytes = { -110, 19, 9, 17, 0, 0, 0, 0, 0, 0, -16, 127 };
    ProtoReader reader = new ProtoReader();
    TestMessagesProto3.TestAllTypesProto3 d = TestMessagesProto3.TestAllTypesProto3.parseFrom(bytes);

    JsonFormat.Printer printer = JsonFormat.printer();
    String expected = printer.print(d);

    System.out.println(expected);

    ProtobufReader.parse(MessageLiteral.TestAllTypesProto3, reader, bytes);
    TestAllTypesProto3 testMessage = (TestAllTypesProto3) reader.stack.pop();

    StringWriter out = new StringWriter();
    ProtoJsonWriter streamingProtoJsonWriter = new ProtoJsonWriter(out);
    streamingProtoJsonWriter.write(visitor -> {
      ProtoWriter.emit(testMessage, visitor);
    });
//    String output = JsonWriter.encode(v -> ProtoWriter.emit(testMessage, v)).encode();

//    System.out.println(output);

  }

  @Test
  public void testJsonInput() throws Exception {

    String json = "{\"oneofNullValue\": null}";

/*
    json = "{\n" +
      "        \"optionalAny\": {\n" +
      "          \"@type\": \"type.googleapis.com/protobuf_test_messages.proto3.TestAllTypesProto3\",\n" +
      "          \"optionalInt32\": 12345\n" +
      "  }\n" +
      "      }";
*/

    TestMessagesProto3.TestAllTypesProto3.Builder builder = TestMessagesProto3.TestAllTypesProto3.newBuilder();
    JsonFormat.parser().usingTypeRegistry(typeRegistry).merge(json, builder);
    TestMessagesProto3.TestAllTypesProto3 d = builder.build();

    String print = JsonFormat.printer().print(d);
    System.out.println(print);

    ProtoReader reader = new ProtoReader();
    ProtoJsonReader.parse(json, MessageLiteral.TestAllTypesProto3, reader);
    TestAllTypesProto3 testMessage = (TestAllTypesProto3) reader.stack.pop();

    StringWriter out = new StringWriter();
    ProtoJsonWriter streamingProtoJsonWriter = new ProtoJsonWriter(out);
    streamingProtoJsonWriter.write(visitor -> {
      ProtoWriter.emit(testMessage, visitor);
    });
    System.out.println(out);

  }

  @Test
  public void testConformance() throws Exception {

    // Recommended.Proto3.ProtobufInput.ValidDataRepeated.ENUM.PackedInput.UnpackedOutput.ProtobufOutput
    byte[] bytes = { -102, 3, 32, 0, 1, 2, -1, -1, -1, -1, -1, -1, -1, -1, -1, 1, -1, -1, -1, -1, -1, -1, -1, -1, 127, -127, -128, -128, -128, -128, -128, -128, -128, -128, 1 };

    // Expected
    // [-48, 41, 123,
    // -48, 41, -56, 3,
    // -46, 41, 3, 97, 98, 99,
    // -46, 41, 3, 100, 101, 102
    // ]

    // Actual
    // [-46, 41, 3, 97, 98, 99,
    // -46, 41, 3, 100, 101, 102,
    // -48, 41, 123,
    // -48, 41, -56, 3]

    // 0
    // 1
    // 2
    // -1
    // -1
    // 1


    ProtoReader reader = new ProtoReader();
    TestMessagesProto3.TestAllTypesProto3 d = TestMessagesProto3.TestAllTypesProto3.parseFrom(bytes);

    byte[] expected = d.toByteArray();

    // repeatedUint64

    //    System.out.println("d = " + d);
    ProtobufReader.parse(MessageLiteral.TestAllTypesProto3, reader, bytes);
    TestAllTypesProto3 testMessage = (TestAllTypesProto3) reader.stack.pop();
    List<TestAllTypesProto3.NestedEnum> a = testMessage.getRepeatedNestedEnum();

    byte[] result = ProtobufWriter.encodeToByteArray(visitor -> {
      ProtoWriter.emit(testMessage, visitor);
    });

    Assert.assertEquals(result.length, expected.length);
  }
}
