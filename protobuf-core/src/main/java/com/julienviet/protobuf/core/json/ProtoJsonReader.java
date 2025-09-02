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
package com.julienviet.protobuf.core.json;

import com.julienviet.protobuf.core.DecodeException;
import com.julienviet.protobuf.core.ProtoStream;
import com.julienviet.protobuf.core.ProtoVisitor;
import com.julienviet.protobuf.lang.internal.Utils;
import com.julienviet.protobuf.schema.EnumType;
import com.julienviet.protobuf.schema.Field;
import com.julienviet.protobuf.schema.MessageType;
import com.julienviet.protobuf.well_known_types.EnumLiteral;
import com.julienviet.protobuf.well_known_types.FieldLiteral;
import com.julienviet.protobuf.well_known_types.MessageLiteral;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayDeque;
import java.util.Base64;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.OptionalInt;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_TIME;

public class ProtoJsonReader {

  private static final DateTimeFormatter f2 = new DateTimeFormatterBuilder()
    .parseCaseSensitive()
    .append(DateTimeFormatter.ISO_LOCAL_DATE)
    .appendLiteral('T')
    .parseStrict()
    .append(ISO_LOCAL_TIME)
    .toFormatter();

  private static final DateTimeFormatter formatter = new DateTimeFormatterBuilder()
    .parseCaseSensitive()
    .append(f2)
    .parseStrict()
    .appendOffsetId()
    .parseStrict()
    .toFormatter();

  public static final long MIN_DURATION_SECONDS = -315_576_000_000L;
  public static final long MAX_DURATION_SECONDS = 315576000000L;
  public static final int MIN_DURATION_NANOS = -999_999_999;
  public static final int MAX_DURATION_NANOS = 999_999_999;
  public static final OffsetDateTime MIN_TIMESTAMP = OffsetDateTime.parse("0001-01-01T00:00:00Z", formatter);
  public static final OffsetDateTime MAX_TIMESTAMP = OffsetDateTime.parse("9999-12-31T23:59:59Z", formatter);
  public static final Instant MIN_TIMESTAMP_INSTANT = MIN_TIMESTAMP.toInstant();
  public static final Instant MAX_TIMESTAMP_INSTANT = MAX_TIMESTAMP.toInstant();
  public static final long MIN_TIMESTAMP_SECONDS = MIN_TIMESTAMP_INSTANT.getEpochSecond();
  public static final long MAX_TIMESTAMP_SECONDS = MAX_TIMESTAMP_INSTANT.getEpochSecond();

  public static boolean isValidDurationSeconds(long seconds) {
    return seconds >= MIN_DURATION_SECONDS && seconds <= MAX_DURATION_SECONDS;
  }

  public static boolean isValidDurationNanos(int nanos) {
    return nanos >= MIN_DURATION_NANOS && nanos <= MAX_DURATION_NANOS;
  }

  public static boolean isValidDuration(long seconds, int nanos) {
    return isValidDurationSeconds(seconds) && isValidDurationNanos(nanos);
  }

  public static boolean isValidTimestampSeconds(long seconds) {
    return seconds >= MIN_TIMESTAMP_SECONDS && seconds <= MAX_TIMESTAMP_SECONDS;
  }

  public static boolean isValidTimestampNanos(int nanos) {
    return nanos >= 0 && nanos <= 999_999_999;
  }

  public static boolean isValidTimestamp(long seconds, int nanos) {
    return isValidTimestampSeconds(seconds) && isValidTimestampNanos(nanos);
  }

  private static void close(JsonDecoder parser) {
    parser.close();
  }

  public static com.julienviet.protobuf.well_known_types.Duration parseDuration(String s) {
    Matcher matcher = DURATION.matcher(s);
    if (!matcher.matches()) {
      return null;
    }
    boolean negative = matcher.group(1) != null;
    long seconds = Long.parseLong(matcher.group(2));
    int nano = 0;
    String nanoText = matcher.group(3);
    if (nanoText != null) {
      // Optimize this later
      nanoText = "0." + nanoText;
      BigDecimal bd = new BigDecimal(nanoText);
      BigInteger bi = bd.multiply(BigDecimal.valueOf(1000_000_000)).toBigInteger();
      nano = bi.intValue();
    }
    if (negative)  {
      seconds = -seconds;
      nano = -nano;
    }
    return new com.julienviet.protobuf.well_known_types.Duration().setSeconds(seconds).setNanos(nano);
  }

  private static final Pattern DURATION = Pattern.compile("(-)?([0-9]+)(?:\\.([0-9]+))?s");

  private static final BigInteger MAX_UINT32 = new BigInteger("FFFFFFFF", 16);
  private static final BigInteger MAX_UINT64 = new BigInteger("FFFFFFFFFFFFFFFF", 16);

  public static ProtoStream readStream(MessageType rootType, String json) {
    return v -> parse(json, rootType, v);
  }

  public static void parse(String json, MessageType messageType, ProtoVisitor visitor) {
    JsonDecoder parser = JsonDecoder.create(json);
    try {
      parse(parser, messageType, visitor);
    } finally {
      parser.close();
    }
  }

  public static void parse(JsonDecoder parser, MessageType messageType, ProtoVisitor visitor) throws DecodeException {
    ProtoJsonReader reader = new ProtoJsonReader(parser, visitor);
    try {
      reader.read(messageType);
    } finally {
      close(parser);
    }
  }

  private final JsonDecoder parser;
  private final ProtoVisitor visitor;
  private boolean ignoreUnknownFields;

  public ProtoJsonReader(String json, ProtoVisitor visitor) {
    this(JsonDecoder.create(json), visitor);
  }

  public ProtoJsonReader(JsonDecoder parser, ProtoVisitor visitor) {
    this.parser = parser;
    this.visitor = visitor;
    this.ignoreUnknownFields = false;
  }

  public ProtoJsonReader ignoreUnknownFields(boolean ignoreUnknownFields) {
    this.ignoreUnknownFields = ignoreUnknownFields;
    return this;
  }

  public void read(MessageType messageType) {
    visitor.init(messageType);
    JsonTokenKind remaining;
    try {
      parser.nextToken();
      readObject(messageType);
      remaining = parser.nextToken();
    } catch (IOException e) {
      throw new DecodeException(e);
    }
    if (remaining != null) {
      throw new DecodeException("Unexpected trailing token");
    }
    visitor.destroy();
  }

  private void readObject(MessageType type) throws IOException {
    if (!parser.hasToken(JsonTokenKind.START_OBJECT)) {
      throw new DecodeException("Unexpected token " + parser.currentToken());
    }
    Set<Field> duplicateChecker = null;
    while (parser.nextToken() == JsonTokenKind.FIELD_NAME) {
      String key = parser.fieldName();
      Field field = type.fieldByJsonName(key);
      if (field == null) {
        field = type.fieldByName(key);
      }
      if (field == null) {
        if (ignoreUnknownFields) {
          parser.nextToken();
          exhaustAny();
        } else {
          throw new DecodeException("Unknown field " + key);
        }
      } else {
        if (duplicateChecker == null) {
          if (field instanceof Enum) {
            // We can safely used this, as the implementation does type check the class
            duplicateChecker = EnumSet.noneOf((Class) field.getClass());
          } else {
            duplicateChecker = new HashSet<>();
          }
        }
        if (!duplicateChecker.add(field)) {
          throw new DecodeException();
        }
        parser.nextToken();
        readAny(field);
      }
    }
  }

  private void readString(Field field) throws IOException, DecodeException {
    if (parser.currentToken() == JsonTokenKind.STRING) {
      visitor.visitString(field, parser.text());
    } else {
      throw new DecodeException("Unexpected token " + parser.currentToken());
    }
  }

  private void readBytes(Field field) throws IOException, DecodeException {
    if (parser.currentToken() == JsonTokenKind.STRING) {
      String text = parser.text();
      byte[] decoded;
      try {
        try {
          decoded = Base64.getDecoder().decode(text);
        } catch (IllegalArgumentException e) {
          // Try URL-safe
          decoded = Base64.getUrlDecoder().decode(text);
        }
      } catch (Exception e) {
        throw new DecodeException(e);
      }
      visitor.visitBytes(field, decoded);
    } else {
      throw new DecodeException("Unexpected token " + parser.currentToken());
    }
  }

  private void readBoolean(Field field) throws IOException, DecodeException {
    if (parser.currentToken() == JsonTokenKind.TRUE || parser.currentToken() == JsonTokenKind.FALSE) {
      visitor.visitBool(field, parser.booleanValue());
    } else {
      throw new DecodeException("Unexpected token " + parser.currentToken());
    }
  }

  private void readEnum(Field field) throws IOException, DecodeException {
    EnumType enumType = (EnumType) field.type();
    switch (parser.currentToken()) {
      case STRING:
        OptionalInt index = enumType.numberOf(parser.text());
        if (index.isPresent()) {
          visitor.visitEnum(field, index.getAsInt());
        } else {
          throw new DecodeException("Unknown enum " + parser.text());
        }
        break;
      case NUMBER_INT:
        visitor.visitEnum(field, parser.intValue());
        break;
      default:
        throw new DecodeException("Unexpected token " + parser.currentToken());
    }
  }

  private int readInt() throws IOException, DecodeException {
    switch (parser.currentToken()) {
      case NUMBER_FLOAT:
        if (parser.doubleValue() % 1 != 0D) {
          throw new DecodeException("Invalid number " + parser.text());
        }
      case NUMBER_INT:
        return parser.intValue();
      case STRING:
        return parseInt(parser.text());
      default:
        throw new DecodeException("Unexpected token " + parser.currentToken());
    }
  }

  private int readUInt32() throws IOException, DecodeException {
    switch (parser.currentToken()) {
      case NUMBER_FLOAT:
        if (parser.doubleValue() % 1 != 0D) {
          throw new DecodeException("Invalid number " + parser.text());
        }
      case NUMBER_INT:
        try {
          return parser.intValue();
        } catch (IOException e) {
          if (!parser.isCoercionException(e)) {
            throw e;
          }
          // Fallback to parseUInt32
        }
      case STRING:
        return parseUInt32(parser.text());
      default:
        throw new DecodeException("Unexpected token " + parser.currentToken());
    }
  }

  private long readLong() throws IOException, DecodeException {
    switch (parser.currentToken()) {
      case NUMBER_FLOAT:
        if (parser.doubleValue() % 1 != 0D) {
          throw new DecodeException("Invalid number " + parser.text());
        }
      case NUMBER_INT:
        return parser.longValue();
      case STRING:
        return parseLong(parser.text());
      default:
        throw new DecodeException("Unexpected token " + parser.currentToken());
    }
  }

  private long readUInt64() throws IOException, DecodeException {
    switch (parser.currentToken()) {
      case NUMBER_FLOAT:
        if (parser.doubleValue() % 1 != 0D) {
          throw new DecodeException("Invalid number " + parser.text());
        }
      case NUMBER_INT:
        try {
          return parser.longValue();
        } catch (IOException e) {
          if (!parser.isCoercionException(e)) {
            throw e;
          }
          // Fallback to parseUInt64
        }
      case STRING:
        return parseUInt64(parser.text());
      default:
        throw new DecodeException("Unexpected token " + parser.currentToken());
    }
  }

  private double readDouble() throws IOException, DecodeException {
    double value;
    switch (parser.currentToken()) {
      case NUMBER_INT:
      case NUMBER_FLOAT:
        value = parser.doubleValue();
        break;
      case STRING:
        try {
          value = Double.parseDouble(parser.text());
        } catch (NumberFormatException e) {
          throw new DecodeException("Invalid number: " + e.getMessage());
        }
        break;
      default:
        throw new DecodeException("Unexpected token " + parser.currentToken());
    }
    String txt = parser.text();
    if (Double.isInfinite(value) && !"Infinity".equals(txt) && !"-Infinity".equals(txt)) {
      throw new DecodeException("Invalid number: " + txt);
    }
    return value;
  }

  private float readFloat() throws IOException, DecodeException {
    float value;
    switch (parser.currentToken()) {
      case NUMBER_INT:
      case NUMBER_FLOAT:
        value = parser.floatValue();
        break;
      case STRING:
        try {
          value = Float.parseFloat(parser.text());
        } catch (NumberFormatException e) {
          throw new DecodeException("Invalid float: " + e.getMessage());
        }
        break;
      default:
        throw new DecodeException("Unexpected token " + parser.currentToken());
    }
    String txt = parser.text();
    if (Float.isInfinite(value) && !"Infinity".equals(txt) && !"-Infinity".equals(txt)) {
      throw new DecodeException("Invalid number: " + txt);
    }
    return value;
  }

  private void readEmbedded(Field field) throws IOException, DecodeException {
    if (parser.currentToken() == JsonTokenKind.START_OBJECT) {
      visitor.enter(field);
      readObject((MessageType) field.type());
      visitor.leave(field);
    } else {
      throw new DecodeException("Unexpected token " + parser.currentToken());
    }
  }

  private void readNumber(Field field) throws IOException, DecodeException {
    switch (field.type().id()) {
      case INT32:
        visitor.visitInt32(field, readInt());
        break;
      case UINT32:
        visitor.visitUInt32(field, readUInt32());
        break;
      case SINT32:
        visitor.visitSInt32(field, readInt());
        break;
      case INT64:
        visitor.visitInt64(field, readLong());
        break;
      case UINT64:
        visitor.visitUInt64(field, readUInt64());
        break;
      case SINT64:
        visitor.visitSInt64(field, readLong());
        break;
      case FIXED32:
        visitor.visitFixed32(field, readUInt32());
        break;
      case SFIXED32:
        visitor.visitSFixed32(field, readInt());
        break;
      case FLOAT:
        visitor.visitFloat(field, readFloat());
        break;
      case FIXED64:
        visitor.visitFixed64(field, readUInt64());
        break;
      case SFIXED64:
        visitor.visitSFixed64(field, readLong());
        break;
      case DOUBLE:
        visitor.visitDouble(field, readDouble());
        break;
      default:
        throw new UnsupportedOperationException("Unsupported " + field.type());
    }
  }

  private void readAny(Field field) throws IOException, DecodeException {
    if (parser.currentToken() == JsonTokenKind.NULL) {
      if (field.type() == EnumLiteral.NullValue) {
        visitor.visitEnum(field, 0);
      } else if (field.type() == MessageLiteral.Value) {
        visitor.enter(field);
        visitor.visitEnum(FieldLiteral.Value_null_value, 0);
        visitor.leave(field);
      } else {
        // Use default value
      }
    } else {
      if (field.isMap()) {
        readObjectAsMap(field);
      } else if (field.isRepeated()) {
        readRepeated(field);
      } else {
        readSingleAny(field);
      }
    }
  }

  private void readRepeated(Field field) throws IOException, DecodeException {
    if (parser.currentToken() == JsonTokenKind.START_ARRAY) {
      while (parser.nextToken() != JsonTokenKind.END_ARRAY) {
        readSingleAny(field);
      }
    } else {
      throw new DecodeException("Unexpected token " + parser.currentToken());
    }
  }

  private void readSingleAny(Field field) throws IOException, DecodeException {
    if (field.type() instanceof MessageLiteral) {
      switch ((MessageLiteral)field.type()) {
        case Struct:
          visitor.enter(field);
          StructParser.parseObject(parser, visitor);
          visitor.leave(field);
          break;
        case Value:
          visitor.enter(field);
          StructParser.parseValue(parser, visitor);
          visitor.leave(field);
          break;
        case ListValue:
          visitor.enter(field);
          StructParser.parseArray(parser, visitor);
          visitor.leave(field);
          break;
        case DoubleValue:
          visitor.enter(field);
          readNumber((FieldLiteral.DoubleValue_value));
          visitor.leave(field);
          break;
        case FloatValue:
          visitor.enter(field);
          readNumber(FieldLiteral.FloatValue_value);
          visitor.leave(field);
          break;
        case Int64Value:
          visitor.enter(field);
          readNumber(FieldLiteral.Int64Value_value);
          visitor.leave(field);
          break;
        case UInt64Value:
          visitor.enter(field);
          readNumber(FieldLiteral.UInt64Value_value);
          visitor.leave(field);
          break;
        case Int32Value:
          visitor.enter(field);
          readNumber(FieldLiteral.Int32Value_value);
          visitor.leave(field);
          break;
        case UInt32Value:
          visitor.enter(field);
          readNumber(FieldLiteral.UInt32Value_value);
          visitor.leave(field);
          break;
        case BoolValue:
          visitor.enter(field);
          readBoolean(FieldLiteral.BoolValue_value);
          visitor.leave(field);
          break;
        case StringValue:
          visitor.enter(field);
          readString(FieldLiteral.StringValue_value);
          visitor.leave(field);
          break;
        case BytesValue:
          visitor.enter(field);
          readBytes(FieldLiteral.BytesValue_value);
          visitor.leave(field);
          break;
        case Duration:
          if (parser.currentToken() != JsonTokenKind.STRING) {
            throw new DecodeException();
          }
          String durationText = parser.text();
          com.julienviet.protobuf.well_known_types.Duration duration = parseDuration(durationText);
          if (duration == null || !isValidDuration(duration.getSeconds(), duration.getNanos())) {
            throw new DecodeException("Invalid duration " + durationText);
          }
          visitor.enter(field);
          if (duration.getSeconds() != 0) {
            visitor.visitInt64(FieldLiteral.Duration_seconds, duration.getSeconds());
          }
          if (duration.getNanos() != 0) {
            visitor.visitInt32(FieldLiteral.Duration_nanos, duration.getNanos());
          }
          visitor.leave(field);
          break;
        case Timestamp:
          if (parser.currentToken() != JsonTokenKind.STRING) {
            throw new DecodeException();
          }
          String timestampText = parser.text();
          OffsetDateTime odt;
          try {
            odt = OffsetDateTime.parse(timestampText, formatter);
          } catch (Exception e) {
            throw new DecodeException("Failed to parse timestamp: " + e.getMessage());
          }
          Instant i = odt.toInstant();
          if (i.compareTo(MIN_TIMESTAMP_INSTANT) < 0) {
            throw new DecodeException();
          }
          visitor.enter(field);
          if (i.getEpochSecond() != 0) {
            visitor.visitInt64(FieldLiteral.Timestamp_seconds, i.getEpochSecond());
          }
          if (i.getNano() != 0) {
            visitor.visitInt32(FieldLiteral.Timestamp_nanos, i.getNano());
          }
          visitor.leave(field);
          break;
        case FieldMask:
          if (parser.currentToken() != JsonTokenKind.STRING) {
            throw new DecodeException();
          }
          String text = parser.text();
          String[] paths = text.isEmpty() ? new String[0] : text.split(",");
          visitor.enter(field);
          for (String path : paths) {
            visitor.visitString(FieldLiteral.FieldMask_paths, Utils.lowerCamelToSnake(path));
          }
          visitor.leave(field);
          break;
        case Any:
//          JsonObject entries = new JsonObject(JacksonCodec.parseObject(parser));
//          String type = entries.getString("@type");
//          if ("type.googleapis.com/google.protobuf.Struct".equals(type)) {
//            Object value = entries.getValue("value");
//            StructParser.parseValue();
//          }
          throw new DecodeException();
        default:
          throw new UnsupportedOperationException("Unsupported " + field.type());
      }
    } else {
      switch (field.type().id()) {
        case STRING:
          readString(field);
          break;
        case BYTES:
          readBytes(field);
          break;
        case INT32:
        case UINT32:
        case SINT32:
        case INT64:
        case UINT64:
        case SINT64:
        case FIXED32:
        case SFIXED32:
        case FLOAT:
        case FIXED64:
        case SFIXED64:
        case DOUBLE:
          readNumber(field);
          break;
        case BOOL:
          readBoolean(field);
          break;
        case ENUM:
          readEnum(field);
          break;
        case MESSAGE:
          readEmbedded(field);
          break;
        default:
          throw new UnsupportedOperationException("" + field.type());
      }
    }
  }

  private void readObjectAsMap(Field field) throws IOException {
    assert parser.hasToken(JsonTokenKind.START_OBJECT);
    MessageType mt = (MessageType) field.type();
    Field keyField = mt.field(1);
    Field valueField = mt.field(2);
    while (parser.nextToken() == JsonTokenKind.FIELD_NAME) {
      String key = parser.fieldName();
      parser.nextToken();
      visitor.enter(field);
      switch (keyField.type().id()) {
        case BOOL:
          visitor.visitBool(keyField, Boolean.parseBoolean(key));
          break;
        case INT32:
          visitor.visitInt32(keyField, Integer.parseInt(key));
          break;
        case INT64:
          visitor.visitInt64(keyField, Long.parseLong(key));
          break;
        case UINT32:
          visitor.visitUInt32(keyField, Integer.parseInt(key));
          break;
        case UINT64:
          visitor.visitUInt64(keyField, parseUInt64(key));
          break;
        case SINT32:
          visitor.visitSInt32(keyField, Integer.parseInt(key));
          break;
        case SINT64:
          visitor.visitSInt64(keyField, Long.parseLong(key));
          break;
        case STRING:
          visitor.visitString(keyField, key);
          break;
        case FIXED64:
          visitor.visitFixed64(keyField, Long.parseLong(key));
          break;
        case SFIXED64:
          visitor.visitSFixed64(keyField, Long.parseLong(key));
          break;
        case FIXED32:
          visitor.visitFixed32(keyField, Integer.parseInt(key));
          break;
        case SFIXED32:
          visitor.visitSFixed32(keyField, Integer.parseInt(key));
          break;
        default:
          throw new UnsupportedOperationException();
      }
      readSingleAny(valueField);
      visitor.leave(field);
    }
  }

  private int parseInt(String s) {
    try {
      return Integer.parseInt(s);
    } catch (NumberFormatException e) {
      try {
        return new BigDecimal(s).toBigIntegerExact().intValueExact();
      } catch (Exception ex) {
        throw new DecodeException(e.getMessage());
      }
    }
  }

  private long parseLong(String s) {
    try {
      return Long.parseLong(s);
    } catch (NumberFormatException e) {
      try {
        return new BigDecimal(s).toBigIntegerExact().longValueExact();
      } catch (Exception ex) {
        throw new DecodeException(e.getMessage());
      }
    }
  }

  private static int parseUInt32(String value) {
    BigInteger parsed;
    try {
      parsed = new BigDecimal(value).toBigIntegerExact();
    } catch (ArithmeticException | NumberFormatException e) {
      throw new DecodeException("Invalid uint32: " + e.getMessage());
    }
    if (parsed.compareTo(BigInteger.ZERO) < 0 || parsed.compareTo(MAX_UINT32) > 0) {
      throw new DecodeException("Invalid uint64 value");
    }
    return parsed.intValue();
  }

  private static long parseUInt64(String value) {
    BigInteger parsed;
    try {
      parsed = new BigDecimal(value).toBigIntegerExact();
    } catch (ArithmeticException | NumberFormatException e) {
      throw new DecodeException("Invalid uint64: " + e.getMessage());
    }
    if (parsed.compareTo(BigInteger.ZERO) < 0 || parsed.compareTo(MAX_UINT64) > 0) {
      throw new DecodeException("Invalid uint64 value");
    }
    return parsed.longValue();
  }

  private void exhaustAny() throws IOException, DecodeException {
    switch (parser.currentToken()) {
      case START_OBJECT:
        exhaustObject();
        break;
      case START_ARRAY:
        exhaustArray();
        break;
      case STRING:
      case NUMBER_FLOAT:
      case NUMBER_INT:
      case TRUE:
      case FALSE:
      case NULL:
        break;
      default:
        throw new DecodeException("Unexpected token");
    }
  }

  private void exhaustObject() throws IOException {
    assert parser.hasToken(JsonTokenKind.START_OBJECT);
    while (parser.nextToken() == JsonTokenKind.FIELD_NAME) {
      parser.nextToken();
      exhaustAny();
    }
  }

  private void exhaustArray() throws IOException {
    assert parser.hasToken(JsonTokenKind.START_ARRAY);
    while (parser.nextToken() != JsonTokenKind.END_ARRAY) {
      exhaustAny();
    }
  }
}
