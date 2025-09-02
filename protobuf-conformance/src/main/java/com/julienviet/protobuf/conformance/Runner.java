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

import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

public class Runner {

  private static int testCount;

  public static void run(InputStream in, OutputStream out) throws Exception {
    while (doTestIo(in, out)) {
      testCount++;
    }
  }

  private static boolean doTestIo(InputStream in, OutputStream out) throws Exception {
    int bytes = readLittleEndianIntFromStdin(in);
    if (bytes == -1) {
      return false; // EOF
    }
    byte[] serializedInput = new byte[bytes];
    if (!readFromStdin(in, serializedInput, bytes)) {
      throw new RuntimeException("Unexpected EOF from test program.");
    }
    Conformance.ConformanceRequest request = Conformance.ConformanceRequest.parseFrom(serializedInput);

    Conformance.ConformanceResponse response = doTest(request);
    byte[] serializedOutput;
    try {
      serializedOutput = response.toByteArray();
    } catch (Exception e) {
      // Can fail due to surrogate exception
      serializedOutput = errorResponse(e).toByteArray();
    }
    writeLittleEndianIntToStdout(out, serializedOutput.length);
    writeToStdout(out, serializedOutput);
    return true;
  }

  private static Conformance.ConformanceResponse errorResponse(Exception e) {
    return Conformance.ConformanceResponse.newBuilder().setParseError(e.getMessage() != null ? e.getMessage() : e.getClass().getName()).build();
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
        return errorResponse(e);
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

  private static void writeLittleEndianIntToStdout(OutputStream out, int val) throws Exception {
    byte[] buf = new byte[4];
    buf[0] = (byte) val;
    buf[1] = (byte) (val >> 8);
    buf[2] = (byte) (val >> 16);
    buf[3] = (byte) (val >> 24);
    writeToStdout(out, buf);
  }

  private static void writeToStdout(OutputStream out, byte[] buf) throws Exception {
    out.write(buf);
  }

  private static int readLittleEndianIntFromStdin(InputStream in) throws Exception {
    byte[] buf = new byte[4];
    if (!readFromStdin(in, buf, 4)) {
      return -1;
    }
    return (buf[0] & 0xff)
      | ((buf[1] & 0xff) << 8)
      | ((buf[2] & 0xff) << 16)
      | ((buf[3] & 0xff) << 24);
  }

  private static boolean readFromStdin(InputStream in, byte[] buf, int len) throws Exception {
    int ofs = 0;
    while (len > 0) {
      int read = in.read(buf, ofs, len);
      if (read == -1) {
        return false; // EOF
      }
      ofs += read;
      len -= read;
    }
    return true;
  }


  /**
   *
   */
  public static void main(String[] args) throws Exception {
    // Assumes this is embedded and executed as a far jar with conformance_test_runner.
    run(System.in, System.out);

    // Connects to the running docker image most likely started by
    // > docker build -t protobuf/conformance conformance
    // > docker run --rm -it --name conformance -p 4000:4000 protobuf/conformance
     // connectToDockerImage("localhost", 4000);
  }

  private static void connectToDockerImage(String host, int port) throws Exception {
    try (Socket so = new Socket()) {
      so.connect(new InetSocketAddress(host, port));
      InputStream in = so.getInputStream();
      OutputStream out = so.getOutputStream();
      run(in, out);
    }
  }
}
