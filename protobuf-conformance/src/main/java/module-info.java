module com.julienviet.protobuf.conformance {

  requires com.julienviet.protobuf.lang;
  requires com.julienviet.protobuf.schema;
  requires com.julienviet.protobuf.core;
  requires com.google.protobuf;
  requires com.google.protobuf.util;

  exports com.google.protobuf.conformance;
  exports com.google.protobuf_test_messages.proto3;
}
