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
package io.vertx.protobuf.tests.core.experiment;

import com.google.protobuf.Any;
import io.vertx.protobuf.core.ProtoStream;
import io.vertx.protobuf.core.ProtoVisitor;
import io.vertx.protobuf.core.ProtobufReader;
import io.vertx.protobuf.core.ProtobufWriter;
import io.vertx.protobuf.core.json.ProtoJsonWriter;
import io.vertx.protobuf.schema.*;
import io.vertx.protobuf.schema.Field;
import io.vertx.protobuf.well_known_types.*;
import org.junit.Test;

import java.io.InputStream;
import java.net.URL;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ExperimentTest {

  public static class AddressBookSchema {

    final MessageType phoneNumber;
    final Field number;
    final Field type;
    final MessageType person;
    final Field name;
    final Field id;
    final Field email;
    final Field phone;

    public AddressBookSchema() {

      DefaultSchema schema = new DefaultSchema();

      DefaultEnumType phoneType = new DefaultEnumType()
        .addValue(0, "MOBILE")
        .addValue(1, "HOME")
        .addValue(2, "WORK");

      DefaultMessageType phoneNumber = schema.of("PhoneNumber");
      DefaultField number = phoneNumber.addField(1, "number", ScalarType.STRING);// number
      DefaultField type = phoneNumber.addField(2, "type", phoneType);// type

      DefaultMessageType person = schema.of("Person");
      DefaultField name = person.addField(1, "name", ScalarType.STRING);// name
      DefaultField id = person.addField(2, "id", ScalarType.INT32);// id
      DefaultField email = person.addField(3, "email", ScalarType.STRING);// email
      DefaultField phones = person.addField(4, "phone", phoneNumber);// phones

      this.phoneNumber = phoneNumber;
      this.person = person;
      this.type = type;
      this.number = number;
      this.name = name;
      this.id = id;
      this.email = email;
      this.phone = phones;
    }
  }

  @Test
  public void testSchema() {

    AddressBookSchema schema = new AddressBookSchema();

    ProtoStream daleCooper = visitor -> {
      visitor.init(schema.person);
      visitor.visitString(schema.name, "Dale Cooper");
      visitor.visitInt32(schema.id, 3);
      visitor.enter(schema.phone);
      visitor.visitString(schema.number, "0123456789");
      visitor.visitEnum(schema.type, 0);
      visitor.leave(schema.phone);
      visitor.visitString(schema.email, "dale.cooper@fbi.org");
      visitor.destroy();
    };

    // Assume we have a protobuf version
    byte[] protobuf = ProtobufWriter.encodeToByteArray(daleCooper);

    // Encode directly to JSON without an intermediate model
    String json = ProtoJsonWriter.encode(new ProtoStream() {
      @Override
      public void accept(ProtoVisitor visitor) {
        ProtobufReader.parse(schema.person, visitor, protobuf);
      }
    });
  }

  @Test
  public void testLoadProtobufDescriptors() throws Exception {

    // Let's create a serialized descriptor from "test.proto"
    try (InputStream resource = ExperimentTest.class.getClassLoader().getResourceAsStream("descriptor.bin")) {
      assertNotNull(resource);
      byte[] bytes = resource.readAllBytes();
      FileDescriptorSet descriptorSet = ProtoReader.readFileDescriptorSet(new ProtoStream() {
        @Override
        public void accept(ProtoVisitor visitor) {
          ProtobufReader.parse(MessageLiteral.FileDescriptorSet, visitor, bytes);
        }
      });
      assertEquals(1, descriptorSet.getFile().size());
      List<DescriptorProto> descriptorProto = descriptorSet.getFile().get(0).getMessageType();
      assertEquals(2, descriptorProto.size());
      DescriptorProto type1 = descriptorProto.get(0);
      DescriptorProto type2 = descriptorProto.get(1);
      assertEquals("SimpleMessage", type1.getName());
      assertEquals("Recursive", type2.getName());
    }
  }
}
