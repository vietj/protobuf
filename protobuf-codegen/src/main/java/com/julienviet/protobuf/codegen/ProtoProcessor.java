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

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.GeneratedMessage;
import com.julienviet.protobuf.plugin.reader.ProtoReaderGenerator;
import com.julienviet.protobuf.plugin.schema.SchemaGenerator;
import com.julienviet.protobuf.plugin.writer.ProtoWriterGenerator;
import com.julienviet.protobuf.extension.ExtensionProto;
import com.julienviet.protobuf.lang.ProtoEnum;
import com.julienviet.protobuf.lang.ProtoField;
import com.julienviet.protobuf.lang.ProtoMessage;
import com.julienviet.protobuf.lang.internal.Utils;
import com.julienviet.protobuf.schema.TypeID;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.beans.Introspector;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@SupportedSourceVersion(SourceVersion.RELEASE_11)
public class ProtoProcessor extends AbstractProcessor {

  private static final Pattern PROTO_FIELD_IDENTIFIER = Pattern.compile("\\p{Alpha}[\\p{Alnum}_]*");
  private static final Pattern FIELD_NAME = PROTO_FIELD_IDENTIFIER;

  private static final EnumMap<DescriptorProtos.FieldDescriptorProto.Type, DescriptorProtos.FieldDescriptorProto.Type> CANONICAL_TYPE_MAPPING = new EnumMap<>(DescriptorProtos.FieldDescriptorProto.Type.class);
  private static final EnumMap<TypeID, DescriptorProtos.FieldDescriptorProto.Type> TYPE_ID_MAPPING = new EnumMap<>(TypeID.class);

  static {
    CANONICAL_TYPE_MAPPING.put(DescriptorProtos.FieldDescriptorProto.Type.TYPE_INT32, DescriptorProtos.FieldDescriptorProto.Type.TYPE_INT32);
    CANONICAL_TYPE_MAPPING.put(DescriptorProtos.FieldDescriptorProto.Type.TYPE_SINT32, DescriptorProtos.FieldDescriptorProto.Type.TYPE_INT32);
    CANONICAL_TYPE_MAPPING.put(DescriptorProtos.FieldDescriptorProto.Type.TYPE_UINT32, DescriptorProtos.FieldDescriptorProto.Type.TYPE_INT32);
    CANONICAL_TYPE_MAPPING.put(DescriptorProtos.FieldDescriptorProto.Type.TYPE_FIXED32, DescriptorProtos.FieldDescriptorProto.Type.TYPE_INT32);
    CANONICAL_TYPE_MAPPING.put(DescriptorProtos.FieldDescriptorProto.Type.TYPE_SFIXED32, DescriptorProtos.FieldDescriptorProto.Type.TYPE_INT32);
    CANONICAL_TYPE_MAPPING.put(DescriptorProtos.FieldDescriptorProto.Type.TYPE_INT64, DescriptorProtos.FieldDescriptorProto.Type.TYPE_INT64);
    CANONICAL_TYPE_MAPPING.put(DescriptorProtos.FieldDescriptorProto.Type.TYPE_SINT64, DescriptorProtos.FieldDescriptorProto.Type.TYPE_INT64);
    CANONICAL_TYPE_MAPPING.put(DescriptorProtos.FieldDescriptorProto.Type.TYPE_UINT64, DescriptorProtos.FieldDescriptorProto.Type.TYPE_INT64);
    CANONICAL_TYPE_MAPPING.put(DescriptorProtos.FieldDescriptorProto.Type.TYPE_FIXED64, DescriptorProtos.FieldDescriptorProto.Type.TYPE_INT64);
    CANONICAL_TYPE_MAPPING.put(DescriptorProtos.FieldDescriptorProto.Type.TYPE_SFIXED64, DescriptorProtos.FieldDescriptorProto.Type.TYPE_INT64);

    TYPE_ID_MAPPING.put(TypeID.INT32, DescriptorProtos.FieldDescriptorProto.Type.TYPE_INT32);
    TYPE_ID_MAPPING.put(TypeID.SINT32, DescriptorProtos.FieldDescriptorProto.Type.TYPE_SINT32);
    TYPE_ID_MAPPING.put(TypeID.UINT32, DescriptorProtos.FieldDescriptorProto.Type.TYPE_UINT32);
    TYPE_ID_MAPPING.put(TypeID.FIXED32, DescriptorProtos.FieldDescriptorProto.Type.TYPE_FIXED32);
    TYPE_ID_MAPPING.put(TypeID.SFIXED32, DescriptorProtos.FieldDescriptorProto.Type.TYPE_SFIXED32);
    TYPE_ID_MAPPING.put(TypeID.INT64, DescriptorProtos.FieldDescriptorProto.Type.TYPE_INT64);
    TYPE_ID_MAPPING.put(TypeID.SINT64, DescriptorProtos.FieldDescriptorProto.Type.TYPE_SINT64);
    TYPE_ID_MAPPING.put(TypeID.UINT64, DescriptorProtos.FieldDescriptorProto.Type.TYPE_UINT64);
    TYPE_ID_MAPPING.put(TypeID.FIXED64, DescriptorProtos.FieldDescriptorProto.Type.TYPE_FIXED64);
    TYPE_ID_MAPPING.put(TypeID.SFIXED64, DescriptorProtos.FieldDescriptorProto.Type.TYPE_SFIXED64);
  }

  private final boolean testMode;
  private Types typeUtils;
  private Elements elementUtils;

  private TypeMirror javaLangLong;
  private TypeMirror javaLangString;
  private TypeMirror javaLangBoolean;
  private TypeMirror javaUtilMap;
  private TypeMirror javaUtilList;
  private TypeMirror ioVertxCoreJsonJsonObject;

  Map<String, ProcessingUnit> protoMap = new HashMap<>();
  Map<String, Descriptors.FileDescriptor> compiled = new HashMap<>();

  public ProtoProcessor() {
    this(false);
  }

  public ProtoProcessor(boolean testMode) {
    this.testMode = testMode;
  }

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return Set.of(ProtoMessage.class.getName(), ProtoEnum.class.getName());
  }

  private ProcessingUnit blah(String pkg, boolean stub) {
    return blah(pkg, pkg, stub);
  }

  private ProcessingUnit blah(String javaPkg, String protoPkg, boolean stub) {
    ProcessingUnit unit = protoMap.get(protoPkg);
    if (unit == null) {
      unit = new ProcessingUnit(javaPkg, protoPkg, stub);
      protoMap.put(javaPkg, unit);
    }
    return unit;
  }

  private static String pkgOf(TypeElement elt) {
    Name qn = elt.getQualifiedName();
    return qn.subSequence(0, qn.length() - elt.getSimpleName().length() - 1).toString();
  }

  private Map<String, Descriptors.FileDescriptor> build() {
    Set<String> stubs = new HashSet<>();
    protoMap.forEach((s, pu) -> {
      if (pu.stub) {
        stubs.add(s);
      }
    });
    while (!protoMap.isEmpty()) {
      ProcessingUnit entry = protoMap.values().iterator().next();
      build(entry);
    }
    HashMap<String, Descriptors.FileDescriptor> ret = new HashMap<>(compiled);
    ret.keySet().removeAll(stubs);
    return ret;
  }

  private Descriptors.FileDescriptor build(ProcessingUnit unit) {
    List<Descriptors.FileDescriptor> dependencies = new ArrayList<>();
    for (String pkg : unit.dependencies) {
      Optional<Descriptors.FileDescriptor> opt = compiled.values().stream().filter(fd -> fd.getPackage().equals(pkg)).findFirst();
      Descriptors.FileDescriptor here;
      if (opt.isPresent()) {
        here = opt.get();
      } else {
        Optional<ProcessingUnit> opt2 = protoMap.values().stream().filter(pu -> pu.protoPkg.equals(pkg)).findFirst();
        if (opt2.isPresent()) {
          here = build(opt2.get());
        } else {
          throw new AssertionError("Could not find " + pkg);
        }
      }
      dependencies.add(here);
      unit.fileBuilder.addDependency(here.getName());
    }
    try {
      Descriptors.FileDescriptor f = Descriptors.FileDescriptor.buildFrom(unit.fileBuilder.build(), dependencies.toArray(new Descriptors.FileDescriptor[0]));
      protoMap.remove(unit.javaPkg);
      compiled.put(unit.javaPkg, f);
      return f;
    } catch (Descriptors.DescriptorValidationException e) {
      throw new RuntimeException(e);
    }
  }

  private String throwableToMessageString(Throwable e) {
    StringWriter writer = new StringWriter();
    e.printStackTrace(new PrintWriter(writer));
    return writer.toString();
  }

  private void reportGenException(ValidationException e) {
    String name = e.element.toString();
    if (e.element.getKind() == ElementKind.METHOD) {
      name = e.element.getEnclosingElement() + "#" + name;
    }
    String msg = "Could not generate model for " + name + ": " + e.message;
    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, msg + "\nCaused by: " + throwableToMessageString(e));
    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, msg, e.element);
  }

  private void reportException(Exception e, Element elt) {
    String msg = "Could not generate element for " + elt + ": " + e.getMessage();
    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, msg + "\nCaused by: " + throwableToMessageString(e));
    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, msg, elt);
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    if (!annotations.isEmpty()) {
      try {
        process2(annotations, roundEnv);
      } catch (ValidationException e) {
        if (testMode) {
          throw e;
        } else {
          reportGenException(e);
        }
      }
    }

    return true;
  }

  public void process2(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

    Set<? extends Element> rootElements = roundEnv.getRootElements();

    // Process enums
    rootElements
      .stream()
      .filter(elt -> elt.getAnnotation(ProtoEnum.class) != null)
      .map(TypeElement.class::cast)
      .forEach(enumElt -> {
        ProcessingUnit fileDesc = blah(pkgOf(enumElt), false);
        DescriptorProtos.EnumDescriptorProto.Builder enumDesc = DescriptorProtos.EnumDescriptorProto.newBuilder();
        enumDesc.setName(enumElt.getSimpleName().toString());
        List<? extends Element> constantElts = enumElt
          .getEnclosedElements()
          .stream()
          .filter(elt -> elt.getKind() == ElementKind.ENUM_CONSTANT).collect(Collectors.toList());
        for (int i = 0;i < constantElts.size();i++) {
          DescriptorProtos.EnumValueDescriptorProto.Builder enumValueDesc = DescriptorProtos.EnumValueDescriptorProto
            .newBuilder()
            .setNumber(i)
            .setName(constantElts.get(i).getSimpleName().toString());
          enumDesc.addValue(enumValueDesc);
        }
        fileDesc.fileBuilder.addEnumType(enumDesc);
      });

    List<TypeElement> blah = new ArrayList<>();
    rootElements
      .stream()
      .filter(elt -> elt.getAnnotation(ProtoMessage.class) != null)
      .forEach(annotatedElt -> {
        if (annotatedElt.getKind() != ElementKind.CLASS) {
          throw new ValidationException(annotatedElt, ValidationError.MESSAGE_INVALID_JAVA_CLASS, "@ProtoMessage must annotated a class");
        }
        if (annotatedElt.getModifiers().contains(Modifier.ABSTRACT)) {
          throw new ValidationException(annotatedElt, ValidationError.MESSAGE_INVALID_JAVA_CLASS, "Message class cannot be abstract");
        }
        if (annotatedElt.getEnclosingElement().getKind() != ElementKind.PACKAGE) {
          throw new ValidationException(annotatedElt, ValidationError.MESSAGE_INVALID_JAVA_CLASS, "Message class must be top level");
        }
        Map<Integer, ExecutableElement> map = annotatedElt.getEnclosedElements().stream()
          .filter(elt -> elt.getKind() == ElementKind.CONSTRUCTOR)
          .map(ExecutableElement.class::cast)
          .collect(Collectors.toMap(elt -> elt.getParameters().size(), elt -> elt));
        ExecutableElement defaultConstructor = map.get(0);
        if (defaultConstructor == null) {
          if (map.isEmpty()) {
            // Implicit no arg constructor
          } else {
            throw new ValidationException(annotatedElt, ValidationError.MESSAGE_INVALID_JAVA_CLASS, "Message class must have a no argument constructor");
          }
        } else if (defaultConstructor.getModifiers().contains(Modifier.PRIVATE)) {
          throw new ValidationException(annotatedElt, ValidationError.MESSAGE_INVALID_JAVA_CLASS, "Message class no argument constructor must not be private");
        }
        blah.add((TypeElement) annotatedElt);
      });



    blah.forEach(msgElt -> {
      DescriptorProtos.DescriptorProto.Builder b = DescriptorProtos.DescriptorProto.newBuilder();
      b.setName(msgElt.getSimpleName().toString());
      ProcessingUnit pu = blah(pkgOf(msgElt), false);
      pu.messages.put(msgElt.getSimpleName().toString(), b);
    });

    blah
      .forEach(msgElt -> {

        String pkg = pkgOf(msgElt);
        ProcessingUnit bbbb = blah(pkg, false);
        DescriptorProtos.DescriptorProto.Builder b = bbbb.messages.get(msgElt.getSimpleName().toString());

        List<ExecutableElement> list = msgElt
          .getEnclosedElements()
          .stream()
          .filter(elt ->
            elt.getKind() == ElementKind.METHOD && elt.getAnnotation(ProtoField.class) != null && accept(elt))
          .map(ExecutableElement.class::cast)
          .collect(Collectors.toList());

        Set<Integer> numbers = new HashSet<>();
        for (ExecutableElement fieldElt : list) {
          ProtoField protoField = fieldElt.getAnnotation(ProtoField.class);
          int fieldNumber = protoField.number();
          if (fieldNumber <= 0) {
            throw new ValidationException(fieldElt, ValidationError.FIELD_INVALID_NUMBER, "Illegal field number");
          }
          if (!numbers.add(fieldNumber)) {
            throw new ValidationException(fieldElt, ValidationError.FIELD_DUPLICATE_NUMBER, "Duplicate field number");
          }
          JavaMethod javaMethod = determineMethod(msgElt, fieldElt);
          if (javaMethod == null) {
            throw new ValidationException(fieldElt, ValidationError.FIELD_INVALID_JAVA_METHOD, "Method must be a getter or a setter");
          }
          ProtoType type = protoTypeOf(javaMethod.type);
          if (type == null) {
            throw new ValidationException(fieldElt, ValidationError.FIELD_INVALID_JAVA_TYPE, "Field type is not supported");
          }

          String protoName = protoField.protoName();
          String jsonName = protoField.jsonName();
          int v = (protoName.isEmpty() ? 0 : 1) + (jsonName.isEmpty() ? 0 : 2);
          switch (v) {
            case 0:
              // Full inference from property name
              jsonName = javaMethod.propertyName;
              protoName = Utils.lowerCamelToSnake(jsonName);
              break;
            case 1:
              // Infer json name from proto name
              jsonName = Utils.snakeToLowerCamel(protoName);
              break;
            case 2:
              // Infer from json name
              protoName = Utils.lowerCamelToSnake(jsonName);
              break;
            case 3:
              // Nothing to do all specified
              break;
          }

          Matcher protoNameMatcher = PROTO_FIELD_IDENTIFIER.matcher(protoName);
          if (!protoNameMatcher.matches()) {
            throw new ValidationException(fieldElt, ValidationError.FIELD_INVALID_PROTO_NAME, "Field proto name is illegal");
          }

          if (protoField.type() != TypeID.UNDEFINED) {
            DescriptorProtos.FieldDescriptorProto.Type actual = TYPE_ID_MAPPING.get(protoField.type());
            if (CANONICAL_TYPE_MAPPING.get(actual) != type.type) {
              throw new ValidationException(fieldElt, ValidationError.FIELD_TYPE_MISMATCH, "Field proto type does not match the java field type");
            }
            type.type = actual;
          }

          DescriptorProtos.FieldDescriptorProto.Builder f = DescriptorProtos.FieldDescriptorProto
            .newBuilder()
            .setNumber(fieldNumber)
            .setName(protoName)
            .setJsonName(jsonName)
            .setType(type.type)
            .setOptions(DescriptorProtos.FieldOptions
              .newBuilder()
              .setExtension(ExtensionProto.readerMethod, javaMethod.readerMethod)
              .setExtension(ExtensionProto.writerMethod, javaMethod.writerMethod)
              .build());
          if (type instanceof ProtoScalarType) {
            //
          } else if (type instanceof ProtoEnumType) {
            DeclaredType dt = (DeclaredType) javaMethod.type;
            TypeElement element = (TypeElement) dt.asElement();
            String p = pkgOf(element);
            String name = element.getSimpleName().toString();
            if (p.equals(pkg)) {
              Optional<DescriptorProtos.EnumDescriptorProto.Builder> found = bbbb.fileBuilder.getEnumTypeBuilderList().stream()
                .filter(ee -> ee.getName().equals(name))
                .findFirst();
              if (found.isPresent()) {
                DescriptorProtos.EnumDescriptorProto.Builder b2 = found.get();
                f.setTypeName(b2.getName());
              } else {
                throw new ValidationException(fieldElt, ValidationError.MESSAGE_INVALID_JAVA_CLASS, "Referenced enum must be annotated with @ProtoEnum");
              }
            } else {
              throw new UnsupportedOperationException("Not yet implemented");
            }
          } else if (type instanceof ProtoMessageType) {
            ProtoMessageType pmt = (ProtoMessageType) type;
            f.setProto3Optional(true);
            f.setTypeName(pmt.protoPkg.equals(pkg) ? pmt.name : "." + pmt.protoPkg + "." + pmt.name);
            f.setOptions(DescriptorProtos.FieldOptions.newBuilder().setExtension(ExtensionProto.typeInterop, pmt.interop).build());
            if (!pmt.protoPkg.equals(pkg)) {
              bbbb.dependencies.add(pmt.protoPkg);
            }
          } else if (type instanceof ProtoMapType) {
            ProtoMapType protoMapType = (ProtoMapType) type;
            DescriptorProtos.FieldDescriptorProto.Builder keyField = DescriptorProtos.FieldDescriptorProto.newBuilder();
            DescriptorProtos.FieldDescriptorProto.Builder valueField = DescriptorProtos.FieldDescriptorProto.newBuilder();
            keyField.setName("key");
            keyField.setNumber(1);
            keyField.setType(protoMapType.key.type);
            valueField.setName("value");
            valueField.setNumber(2);
            valueField.setType(protoMapType.value.type);
            // Not correct name like in actual protoc, but it's like anonymous here
            String name = b.getName() + "_" + f.getName() + "_Entry";
            DescriptorProtos.DescriptorProto mapEntry = DescriptorProtos
              .DescriptorProto.newBuilder()
              .setName(name)
              .setOptions(DescriptorProtos.MessageOptions.newBuilder().setMapEntry(true).build())
              .addField(keyField)
              .addField(valueField)
              .build();
            // For now not nested as it's not really needed, and we don't handle nested types in the processor
            // b.addNestedType(mapEntry);
            bbbb.fileBuilder.addMessageType(mapEntry);
            f.setTypeName(name);
          } else {
            throw new UnsupportedOperationException();
          }
          if (type.repeated) {
            f.setLabel(DescriptorProtos.FieldDescriptorProto.Label.LABEL_REPEATED);
          }
          b.addField(f);
        }
      });

    // Move message descriptors to file
    protoMap.values().forEach(pu -> {
      pu.messages.values().forEach(b -> {
        pu.fileBuilder.addMessageType(b.build());
      });
      // Not needed anymore
      pu.messages.clear();
    });

    Map<String, Descriptors.FileDescriptor> build = build();

    for (Descriptors.FileDescriptor p : build.values()) {
      String javaPkg = p.getOptions().getJavaPackage();
      SchemaGenerator schemaGenerator = new SchemaGenerator(javaPkg);
      schemaGenerator.init(p.getMessageTypes(), p.getEnumTypes());
      ProtoReaderGenerator protoReaderGenerator = new ProtoReaderGenerator(javaPkg, true, p.getMessageTypes());
      ProtoWriterGenerator protoWriterGenerator = new ProtoWriterGenerator(javaPkg, true, false, p.getMessageTypes());
      try {
        Filer filer = processingEnv.getFiler();
        JavaFileObject messageLiteralFile = filer.createSourceFile(javaPkg + ".MessageLiteral");
        JavaFileObject fieldLiteralFile = filer.createSourceFile(javaPkg + ".FieldLiteral");
        JavaFileObject enumLiteralFile = filer.createSourceFile(javaPkg + ".EnumLiteral");
        JavaFileObject protoReaderFile = filer.createSourceFile(javaPkg + ".ProtoReader");
        JavaFileObject protoWriterFile = filer.createSourceFile(javaPkg + ".ProtoWriter");
        try (Writer writer = messageLiteralFile.openWriter()) {
          writer.write(schemaGenerator.generateMessageLiterals());
        }
        try (Writer writer = fieldLiteralFile.openWriter()) {
          writer.write(schemaGenerator.generateFieldLiterals());
        }
        try (Writer writer = enumLiteralFile.openWriter()) {
          writer.write(schemaGenerator.generateEnumLiterals());
        }
        try (Writer writer = protoReaderFile.openWriter()) {
          writer.write(protoReaderGenerator.generate());
        }
        try (Writer writer = protoWriterFile.openWriter()) {
          writer.write(protoWriterGenerator.generate());
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  public boolean accept(Element element) {
    Set<Modifier> modifiers = element.getModifiers();
    return modifiers.contains(Modifier.PUBLIC) && !modifiers.contains(Modifier.STATIC);
  }

  private static final Predicate<Element> PUBLIC_METHODS = e ->
    e.getKind() == ElementKind.METHOD &&
      e.getModifiers().contains(Modifier.PUBLIC) &&
      !e.getModifiers().contains(Modifier.STATIC);

  private boolean hasSetter(TypeElement msgElt, TypeMirror type, String name) {
    return msgElt.getEnclosedElements()
      .stream()
      .filter(PUBLIC_METHODS)
      .map(ExecutableElement.class::cast)
      .filter(exe -> exe.getParameters().size() == 1 && typeUtils.isSameType(type, exe.getParameters().get(0).asType()))
      .anyMatch(exe -> exe.getSimpleName().toString().equals(name));
  }

  private Map<String, ExecutableElement> findGetters(TypeElement msgElt, TypeMirror type) {
    return msgElt.getEnclosedElements()
      .stream()
      .filter(PUBLIC_METHODS)
      .map(ExecutableElement.class::cast)
      .filter(exe -> typeUtils.isSameType(type, exe.getReturnType()) && exe.getParameters().isEmpty())
      .collect(Collectors.toMap(exec -> exec.getSimpleName().toString(), exec -> exec));

  }

  private JavaMethod determineMethod(TypeElement msgElt, ExecutableElement elt) {
    String name = elt.getSimpleName().toString();
    if (name.startsWith("get") && name.length() > 3 && Character.isUpperCase(name.charAt(3)) && elt.getParameters().isEmpty() && elt.getReturnType().getKind() != TypeKind.VOID) {
      String propertyName = Introspector.decapitalize(name.substring(3));
      String writeMethod = "set" + name.substring(3);
      if (!hasSetter(msgElt, elt.getReturnType(), writeMethod)) {
        throw new ValidationException(elt, ValidationError.FIELD_MISSING_JAVA_SETTER_METHOD, "Method does not have a corresponding setter");
      }
      return JavaMethod.createGet(propertyName, name, writeMethod, elt.getReturnType(), elt);
    } else if (name.startsWith("is") && name.length() > 2 && Character.isUpperCase(name.charAt(2)) &&
      elt.getParameters().isEmpty() && elt.getReturnType().getKind() != TypeKind.VOID) {
      String jsonName = Introspector.decapitalize(name.substring(2));
      String writeMethod = "set" + name.substring(2);
      if (!hasSetter(msgElt, elt.getReturnType(), writeMethod)) {
        throw new ValidationException(elt, ValidationError.FIELD_MISSING_JAVA_SETTER_METHOD, "Method does not have a corresponding setter");
      }
      return JavaMethod.createIs(jsonName, name, writeMethod, elt.getReturnType(), elt);
    } else {
      if (name.startsWith("set") && name.length() > 3 && Character.isUpperCase(name.charAt(3)) && elt.getParameters().size() == 1 &&
        elt.getReturnType().getKind() == TypeKind.VOID) {
        String jsonName = Introspector.decapitalize(name.substring(3));
        TypeMirror type = elt.getParameters().get(0).asType();
        Map<String, ExecutableElement> results = findGetters(msgElt, type);
        String getKey = "get" + name.substring(3);
        String readerMethod;
        if (type.getKind() == TypeKind.BOOLEAN) {
          String isKey = "is" + name.substring(3);
          if (results.containsKey(isKey)) {
            readerMethod = isKey;
          } else if (results.containsKey(getKey)) {
            readerMethod = getKey;
          } else {
            throw new ValidationException(elt, ValidationError.FIELD_MISSING_JAVA_GETTER_METHOD, "Method does not have a corresponding getter");
          }
        } else {
          if (results.containsKey(getKey)) {
            readerMethod = getKey;
          } else {
            throw new ValidationException(elt, ValidationError.FIELD_MISSING_JAVA_GETTER_METHOD, "Method does not have a corresponding getter");
          }
        }
        return JavaMethod.createSet(jsonName, readerMethod, name, type, elt);
      } else {
        return null;
      }
    }
  }

  private ProtoType protoTypeOf(TypeMirror type) {
    switch (type.getKind()) {
      case ARRAY:
        ArrayType arrayType = (ArrayType) type;
        if (arrayType.getComponentType().getKind() == TypeKind.BYTE) {
          return new ProtoScalarType(DescriptorProtos.FieldDescriptorProto.Type.TYPE_BYTES);
        } else {
          return null;
        }
      case BOOLEAN:
        return new ProtoScalarType(DescriptorProtos.FieldDescriptorProto.Type.TYPE_BOOL);
      case INT:
        return new ProtoScalarType(DescriptorProtos.FieldDescriptorProto.Type.TYPE_INT32);
      case LONG:
        return new ProtoScalarType(DescriptorProtos.FieldDescriptorProto.Type.TYPE_INT64);
      case FLOAT:
        return new ProtoScalarType(DescriptorProtos.FieldDescriptorProto.Type.TYPE_FLOAT);
      case DOUBLE:
        return new ProtoScalarType(DescriptorProtos.FieldDescriptorProto.Type.TYPE_DOUBLE);
      case DECLARED:
        DeclaredType declaredType = (DeclaredType) type;
        Element typeElt = declaredType.asElement();
        switch (typeElt.getKind()) {
          case INTERFACE:
            if (isMap(type)) {
              TypeMirror key = declaredType.getTypeArguments().get(0);
              TypeMirror value = declaredType.getTypeArguments().get(1);
              ProtoMapType protoMapType = new ProtoMapType();
              protoMapType.key = protoTypeOf(key);
              protoMapType.value = protoTypeOf(value);
              return protoMapType;
            } else if (isList(type)) {
              TypeMirror element = declaredType.getTypeArguments().get(0);
              ProtoType p = protoTypeOf(element);
              if (p != null) {
                p.repeated = true;
              }
              return p;
            }
            break;
          case CLASS:
            DeclaredType dt = (DeclaredType) type;
            TypeElement typeElement = (TypeElement) dt.asElement();
            if (typeUtils.isSameType(javaLangString, type)) {
              return new ProtoScalarType(DescriptorProtos.FieldDescriptorProto.Type.TYPE_STRING);
            } else if (ioVertxCoreJsonJsonObject != null && typeUtils.isSameType(ioVertxCoreJsonJsonObject, type)) {
              ProtoMessageType interop = new ProtoMessageType();
              ProcessingUnit pu = blah("com.google.protobuf", "google.protobuf", true);
              // Might do that a lot of time
              DescriptorProtos.DescriptorProto.Builder builder = DescriptorProtos.DescriptorProto.newBuilder();
              builder.setName("Struct");
              pu.messages.put("Struct", builder);
              interop.javaPkg = "com.julienviet.protobuf.well_known_types";
              interop.protoPkg = "google.protobuf";
              interop.name = "Struct";
              interop.interop = true;
              return interop;
            } else {
              ProtoMessageType pt;
              ProtoMessage protoMessage = typeElt.getAnnotation(ProtoMessage.class);
              if (protoMessage != null) {
                pt = new ProtoMessageType();
              } else {
                TypeElement te = (TypeElement) typeElt;
                String pkg = pkgOf(te);
//                  Filer filer = processingEnv.getFiler();
//                  FileObject resource = filer.getResource(StandardLocation.CLASS_PATH, pkg, "descriptor.bin");
//                  try (InputStream is = resource.openInputStream()) {
//                    byte[] bytes = is.readAllBytes();
//                    DescriptorProtos.FileDescriptorSet set = DescriptorProtos.FileDescriptorSet.parseFrom(bytes);
//                  }
                TypeElement msgLiteralTypeElement = processingEnv.getElementUtils().getTypeElement(pkg + ".MessageLiteral");
                if (msgLiteralTypeElement == null) {
                  return null;
                }
                // Create a stub
                ProcessingUnit pu = blah(pkg, true);
                DescriptorProtos.DescriptorProto.Builder builder = DescriptorProtos.DescriptorProto.newBuilder();
                builder.setName(te.getSimpleName().toString());
                pu.messages.put(te.getSimpleName().toString(), builder);
                pt = new ProtoMessageType();
              }

              String p = pkgOf(typeElement);
              ProcessingUnit foo = protoMap.get(p);
              if (foo == null) {
                throw new UnsupportedOperationException("handle me");
              }
              DescriptorProtos.DescriptorProto.Builder ref = foo.messages.get(typeElement.getSimpleName().toString());
              if (ref == null) {
                throw new RuntimeException("Handle that with a proper processor failure");
              }
              pt.javaPkg = p;
              pt.protoPkg = p;
              pt.name = ref.getName();
              return pt;
            }
          case ENUM:
            return new ProtoEnumType();
        }
        break;
    }
    return null;
  }

  private boolean isMap(TypeMirror type) {
    if (type.getKind() == TypeKind.DECLARED) {
      DeclaredType dt = (DeclaredType) type;
      TypeMirror a = processingEnv.getTypeUtils().erasure(dt);
      return processingEnv.getTypeUtils().isSameType(javaUtilMap, a);
    }
    return false;
  }

  private boolean isList(TypeMirror type) {
    if (type.getKind() == TypeKind.DECLARED) {
      DeclaredType dt = (DeclaredType) type;
      TypeMirror a = processingEnv.getTypeUtils().erasure(dt);
      return processingEnv.getTypeUtils().isSameType(javaUtilList, a);
    }
    return false;
  }

  static abstract class ProtoType {
    boolean repeated;
    DescriptorProtos.FieldDescriptorProto.Type type;
    ProtoType(DescriptorProtos.FieldDescriptorProto.Type type) {
      this.type = type;
    }
  }

  static class ProtoEnumType extends ProtoType {
    public ProtoEnumType() {
      super(DescriptorProtos.FieldDescriptorProto.Type.TYPE_ENUM);
    }
  }

  static class ProtoMessageType extends ProtoType {
    String javaPkg;
    String protoPkg;
    String name;
    boolean interop;
    public ProtoMessageType() {
      super(DescriptorProtos.FieldDescriptorProto.Type.TYPE_MESSAGE);
    }
  }

  static class ProtoMapType extends ProtoType {
    ProtoType key;
    ProtoType value;
    public ProtoMapType() {
      super(DescriptorProtos.FieldDescriptorProto.Type.TYPE_MESSAGE);
      repeated = true;
    }
  }

  static class ProtoScalarType extends ProtoType {
    ProtoScalarType(DescriptorProtos.FieldDescriptorProto.Type type) {
      super(type);
    }
  }

  @Override
  public synchronized void init(ProcessingEnvironment processingEnv) {

    ExtensionRegistry extensionRegistry = ExtensionRegistry.newInstance();
    for (GeneratedMessage.GeneratedExtension<?, ?> extension : List.of(ExtensionProto.typeInterop, ExtensionProto.readerMethod, ExtensionProto.writerMethod)) {
      extensionRegistry.add(extension);
    }

    typeUtils = processingEnv.getTypeUtils();
    elementUtils = processingEnv.getElementUtils();
    super.init(processingEnv);
    javaLangLong = elementUtils.getTypeElement("java.lang.Long").asType();
    javaLangString = elementUtils.getTypeElement("java.lang.String").asType();
    javaLangBoolean = elementUtils.getTypeElement("java.lang.Boolean").asType();
    javaUtilMap = processingEnv.getTypeUtils().erasure(elementUtils.getTypeElement("java.util.Map").asType());
    javaUtilList = processingEnv.getTypeUtils().erasure(elementUtils.getTypeElement("java.util.List").asType());
    // ioVertxCoreJsonJsonObject = elementUtils.getTypeElement("io.vertx.core.json.JsonObject").asType();
  }
}
