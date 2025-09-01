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
package com.julienviet.protobuf.conformance;

import com.google.protobuf.ByteString;
import com.google.protobuf.conformance.Conformance;
import com.julienviet.protobuf.core.DecodeException;
import com.julienviet.protobuf.core.ProtobufReader;
import com.julienviet.protobuf.core.ProtobufWriter;
import com.google.protobuf_test_messages.proto3.ProtoWriter;
import com.google.protobuf_test_messages.proto3.TestAllTypesProto3;
import com.google.protobuf_test_messages.proto3.ProtoReader;
import com.google.protobuf_test_messages.proto3.MessageLiteral;
import com.julienviet.protobuf.core.json.ProtoJsonReader;
import com.julienviet.protobuf.core.json.ProtoJsonWriter;

import java.io.StringWriter;

public class Runner {

  private static int testCount;

  private static void run() throws Exception {
    while (doTestIo()) {
      testCount++;
    }
  }

  private static boolean doTestIo() throws Exception {
    int bytes = readLittleEndianIntFromStdin();

    if (bytes == -1) {
      return false; // EOF
    }

    byte[] serializedInput = new byte[bytes];

    if (!readFromStdin(serializedInput, bytes)) {
      throw new RuntimeException("Unexpected EOF from test program.");
    }

    Conformance.ConformanceRequest request =
      Conformance.ConformanceRequest.parseFrom(serializedInput);
    Conformance.ConformanceResponse response = doTest(request);
    byte[] serializedOutput = response.toByteArray();

    writeLittleEndianIntToStdout(serializedOutput.length);
    writeToStdout(serializedOutput);


    return true;
  }

  private static Conformance.ConformanceResponse doTest(Conformance.ConformanceRequest request) {
    TestAllTypesProto3 testMessage;
    String messageType = request.getMessageType();

    if (messageType.equals("protobuf_test_messages.proto3.TestAllTypesProto3")
      && (request.getPayloadCase() == Conformance.ConformanceRequest.PayloadCase.PROTOBUF_PAYLOAD || request.getPayloadCase() == Conformance.ConformanceRequest.PayloadCase.JSON_PAYLOAD)
      && (request.getRequestedOutputFormat() == Conformance.WireFormat.PROTOBUF || request.getRequestedOutputFormat() == Conformance.WireFormat.JSON)) {
      ProtoReader reader = new ProtoReader();
      try {
        switch (request.getPayloadCase()) {
          case PROTOBUF_PAYLOAD:
            byte[] buffer = request.getProtobufPayload().toByteArray();
            ProtobufReader.parse(MessageLiteral.TestAllTypesProto3, reader, buffer);
            break;
          case JSON_PAYLOAD:
            boolean ignoreUnknownJsonParsing = request.getTestCategory() == Conformance.TestCategory.JSON_IGNORE_UNKNOWN_PARSING_TEST;
            String json = request.getJsonPayload();
            ProtoJsonReader r = new ProtoJsonReader(json, reader);
            r.ignoreUnknownFields(ignoreUnknownJsonParsing);
            r.read(MessageLiteral.TestAllTypesProto3);
            break;
        }
      } catch (DecodeException | IndexOutOfBoundsException e) {
        return Conformance.ConformanceResponse.newBuilder().setParseError(e.getMessage() != null ? e.getMessage() : e.getClass().getName()).build();
      }
      testMessage = (TestAllTypesProto3) reader.stack.pop();
    } else {
      return Conformance.ConformanceResponse.newBuilder()
        .setSkipped("Only proto3 tested")
        .build();
    }

    switch (request.getRequestedOutputFormat()) {

      case PROTOBUF: {
        byte[] result = ProtobufWriter.encodeToByteArray(visitor -> {
          ProtoWriter.emit(testMessage, visitor);
        });
        return Conformance.ConformanceResponse.newBuilder()
          .setProtobufPayload(ByteString.copyFrom(result))
          .build();
      }

      case JSON:
        String result;
        try {
          StringWriter out = new StringWriter();
          ProtoJsonWriter streamingProtoJsonWriter = new ProtoJsonWriter(out);
          streamingProtoJsonWriter.write(visitor -> {
            ProtoWriter.emit(testMessage, visitor);
          });
          result = out.toString();
        } catch (Exception e) {
          return Conformance.ConformanceResponse.newBuilder().setSerializeError(e.getMessage() != null ? e.getMessage() : e.getClass().getName()).build();
        }
        return Conformance.ConformanceResponse.newBuilder()
          .setJsonPayload(result)
          .build();

      default: {
        throw new IllegalArgumentException("Unexpected request output.");
      }
    }
  }

  private static void writeLittleEndianIntToStdout(int val) throws Exception {
    byte[] buf = new byte[4];
    buf[0] = (byte) val;
    buf[1] = (byte) (val >> 8);
    buf[2] = (byte) (val >> 16);
    buf[3] = (byte) (val >> 24);
    writeToStdout(buf);
  }

  private static void writeToStdout(byte[] buf) throws Exception {
    System.out.write(buf);
  }

  private static int readLittleEndianIntFromStdin() throws Exception {
    byte[] buf = new byte[4];
    if (!readFromStdin(buf, 4)) {
      return -1;
    }
    return (buf[0] & 0xff)
      | ((buf[1] & 0xff) << 8)
      | ((buf[2] & 0xff) << 16)
      | ((buf[3] & 0xff) << 24);
  }

  private static boolean readFromStdin(byte[] buf, int len) throws Exception {
    int ofs = 0;
    while (len > 0) {
      int read = System.in.read(buf, ofs, len);
      if (read == -1) {
        return false; // EOF
      }
      ofs += read;
      len -= read;
    }

    return true;
  }


  public static void main(String[] args) throws Exception {
//    Thread.sleep(100000000);
    run();
  }
}
