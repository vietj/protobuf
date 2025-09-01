#
# Copyright (C) 2025 Julien Viet
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
#
./conformance_test_runner --enforce_recommended --maximum_edition PROTO3 --output_dir . \
--test Recommended.Proto3.JsonInput.BytesFieldBase64Url.JsonOutput \
--test Recommended.Proto3.JsonInput.FieldNameWithDoubleUnderscores.JsonOutput \
--test Recommended.Proto3.JsonInput.MultilineNoSpaces.JsonOutput \
--test Recommended.Proto3.JsonInput.MultilineWithSpaces.JsonOutput \
--test Recommended.Proto3.JsonInput.OneLineNoSpaces.JsonOutput \
--test Recommended.Proto3.JsonInput.OneLineWithSpaces.JsonOutput \
--test Recommended.Proto3.JsonInput.OneofZeroBool.JsonOutput \
--test Recommended.Proto3.JsonInput.OneofZeroBytes.JsonOutput \
--test Recommended.Proto3.JsonInput.OneofZeroDouble.JsonOutput \
--test Recommended.Proto3.JsonInput.OneofZeroEnum.JsonOutput \
--test Recommended.Proto3.JsonInput.OneofZeroFloat.JsonOutput \
--test Recommended.Proto3.JsonInput.OneofZeroMessage.JsonOutput \
--test Recommended.Proto3.JsonInput.OneofZeroString.JsonOutput \
--test Recommended.Proto3.JsonInput.OneofZeroUint32.JsonOutput \
--test Recommended.Proto3.JsonInput.OneofZeroUint64.JsonOutput \
--test Required.Proto3.JsonInput.AllFieldAcceptNull.JsonOutput \
--test Required.Proto3.JsonInput.Any.JsonOutput \
--test Required.Proto3.JsonInput.AnyNested.JsonOutput \
--test Required.Proto3.JsonInput.AnyUnorderedTypeTag.JsonOutput \
--test Required.Proto3.JsonInput.AnyWithDuration.JsonOutput \
--test Required.Proto3.JsonInput.AnyWithFieldMask.JsonOutput \
--test Required.Proto3.JsonInput.AnyWithInt32ValueWrapper.JsonOutput \
--test Required.Proto3.JsonInput.AnyWithStruct.JsonOutput \
--test Required.Proto3.JsonInput.AnyWithTimestamp.JsonOutput \
--test Required.Proto3.JsonInput.AnyWithValueForInteger.JsonOutput \
--test Required.Proto3.JsonInput.AnyWithValueForJsonObject.JsonOutput \
--test Required.Proto3.JsonInput.BoolFieldFalse.JsonOutput \
--test Required.Proto3.JsonInput.BoolFieldTrue.JsonOutput \
--test Required.Proto3.JsonInput.BoolMapEscapedKey.JsonOutput \
--test Required.Proto3.JsonInput.BoolMapField.JsonOutput \
--test Required.Proto3.JsonInput.BytesField.JsonOutput \
--test Required.Proto3.JsonInput.BytesRepeatedField.JsonOutput \
--test Required.Proto3.JsonInput.DoubleFieldInfinity.JsonOutput \
--test Required.Proto3.JsonInput.DoubleFieldMaxNegativeValue.JsonOutput \
--test Required.Proto3.JsonInput.DoubleFieldMaxPositiveValue.JsonOutput \
--test Required.Proto3.JsonInput.DoubleFieldMinNegativeValue.JsonOutput \
--test Required.Proto3.JsonInput.DoubleFieldMinPositiveValue.JsonOutput \
--test Required.Proto3.JsonInput.DoubleFieldNan.JsonOutput \
--test Required.Proto3.JsonInput.DoubleFieldNegativeInfinity.JsonOutput \
--test Required.Proto3.JsonInput.DoubleFieldQuotedValue.JsonOutput \
--test Required.Proto3.JsonInput.DurationMaxValue.JsonOutput \
--test Required.Proto3.JsonInput.DurationMinValue.JsonOutput \
--test Required.Proto3.JsonInput.DurationNegativeNanos.JsonOutput \
--test Required.Proto3.JsonInput.DurationNegativeSeconds.JsonOutput \
--test Required.Proto3.JsonInput.DurationNull.JsonOutput \
--test Required.Proto3.JsonInput.DurationRepeatedValue.JsonOutput \
--test Required.Proto3.JsonInput.EmptyFieldMask.JsonOutput \
--test Required.Proto3.JsonInput.EnumField.JsonOutput \
--test Required.Proto3.JsonInput.EnumFieldNumericValueNonZero.JsonOutput \
--test Required.Proto3.JsonInput.EnumFieldNumericValueZero.JsonOutput \
--test Required.Proto3.JsonInput.EnumFieldWithAlias.JsonOutput \
--test Required.Proto3.JsonInput.EnumFieldWithAliasDifferentCase.JsonOutput \
--test Required.Proto3.JsonInput.EnumFieldWithAliasLowerCase.JsonOutput \
--test Required.Proto3.JsonInput.EnumFieldWithAliasUseAlias.JsonOutput \
--test Required.Proto3.JsonInput.EnumRepeatedField.JsonOutput \
--test Required.Proto3.JsonInput.FieldMask.JsonOutput \
--test Required.Proto3.JsonInput.FieldNameEscaped.JsonOutput \
--test Required.Proto3.JsonInput.FieldNameInSnakeCase.JsonOutput \
--test Required.Proto3.JsonInput.FieldNameWithMixedCases.JsonOutput \
--test Required.Proto3.JsonInput.FieldNameWithNumbers.JsonOutput \
--test Required.Proto3.JsonInput.FloatFieldInfinity.JsonOutput \
--test Required.Proto3.JsonInput.FloatFieldMaxNegativeValue.JsonOutput \
--test Required.Proto3.JsonInput.FloatFieldMaxPositiveValue.JsonOutput \
--test Required.Proto3.JsonInput.FloatFieldMinNegativeValue.JsonOutput \
--test Required.Proto3.JsonInput.FloatFieldMinPositiveValue.JsonOutput \
--test Required.Proto3.JsonInput.FloatFieldNan.JsonOutput \
--test Required.Proto3.JsonInput.FloatFieldNegativeInfinity.JsonOutput \
--test Required.Proto3.JsonInput.FloatFieldQuotedValue.JsonOutput \
--test Required.Proto3.JsonInput.HelloWorld.JsonOutput \
--test Required.Proto3.JsonInput.Int32FieldExponentialFormat.JsonOutput \
--test Required.Proto3.JsonInput.Int32FieldFloatTrailingZero.JsonOutput \
--test Required.Proto3.JsonInput.Int32FieldMaxFloatValue.JsonOutput \
--test Required.Proto3.JsonInput.Int32FieldMaxValue.JsonOutput \
--test Required.Proto3.JsonInput.Int32FieldMinFloatValue.JsonOutput \
--test Required.Proto3.JsonInput.Int32FieldMinValue.JsonOutput \
--test Required.Proto3.JsonInput.Int32FieldStringValue.JsonOutput \
--test Required.Proto3.JsonInput.Int32FieldStringValueEscaped.JsonOutput \
--test Required.Proto3.JsonInput.Int32MapEscapedKey.JsonOutput \
--test Required.Proto3.JsonInput.Int32MapField.JsonOutput \
--test Required.Proto3.JsonInput.Int64FieldMaxValue.JsonOutput \
--test Required.Proto3.JsonInput.Int64FieldMaxValueNotQuoted.JsonOutput \
--test Required.Proto3.JsonInput.Int64FieldMinValue.JsonOutput \
--test Required.Proto3.JsonInput.Int64FieldMinValueNotQuoted.JsonOutput \
--test Required.Proto3.JsonInput.Int64MapEscapedKey.JsonOutput \
--test Required.Proto3.JsonInput.Int64MapField.JsonOutput \
--test Required.Proto3.JsonInput.MessageField.JsonOutput \
--test Required.Proto3.JsonInput.MessageMapField.JsonOutput \
--test Required.Proto3.JsonInput.MessageRepeatedField.JsonOutput \
--test Required.Proto3.JsonInput.OneofFieldNullFirst.JsonOutput \
--test Required.Proto3.JsonInput.OneofFieldNullSecond.JsonOutput \
--test Required.Proto3.JsonInput.OptionalBoolWrapper.JsonOutput \
--test Required.Proto3.JsonInput.OptionalBytesWrapper.JsonOutput \
--test Required.Proto3.JsonInput.OptionalDoubleWrapper.JsonOutput \
--test Required.Proto3.JsonInput.OptionalFloatWrapper.JsonOutput \
--test Required.Proto3.JsonInput.OptionalInt32Wrapper.JsonOutput \
--test Required.Proto3.JsonInput.OptionalInt64Wrapper.JsonOutput \
--test Required.Proto3.JsonInput.OptionalStringWrapper.JsonOutput \
--test Required.Proto3.JsonInput.OptionalUint32Wrapper.JsonOutput \
--test Required.Proto3.JsonInput.OptionalUint64Wrapper.JsonOutput \
--test Required.Proto3.JsonInput.OptionalWrapperTypesWithNonDefaultValue.JsonOutput \
--test Required.Proto3.JsonInput.OriginalProtoFieldName.JsonOutput \
--test Required.Proto3.JsonInput.PrimitiveRepeatedField.JsonOutput \
--test Required.Proto3.JsonInput.RepeatedBoolWrapper.JsonOutput \
--test Required.Proto3.JsonInput.RepeatedBytesWrapper.JsonOutput \
--test Required.Proto3.JsonInput.RepeatedDoubleWrapper.JsonOutput \
--test Required.Proto3.JsonInput.RepeatedFloatWrapper.JsonOutput \
--test Required.Proto3.JsonInput.RepeatedInt32Wrapper.JsonOutput \
--test Required.Proto3.JsonInput.RepeatedInt64Wrapper.JsonOutput \
--test Required.Proto3.JsonInput.RepeatedListValue.JsonOutput \
--test Required.Proto3.JsonInput.RepeatedStringWrapper.JsonOutput \
--test Required.Proto3.JsonInput.RepeatedUint32Wrapper.JsonOutput \
--test Required.Proto3.JsonInput.RepeatedUint64Wrapper.JsonOutput \
--test Required.Proto3.JsonInput.RepeatedValue.JsonOutput \
--test Required.Proto3.JsonInput.StringField.JsonOutput \
--test Required.Proto3.JsonInput.StringFieldEmbeddedNull.JsonOutput \
--test Required.Proto3.JsonInput.StringFieldEscape.JsonOutput \
--test Required.Proto3.JsonInput.StringFieldSurrogatePair.JsonOutput \
--test Required.Proto3.JsonInput.StringFieldUnicode.JsonOutput \
--test Required.Proto3.JsonInput.StringFieldUnicodeEscape.JsonOutput \
--test Required.Proto3.JsonInput.StringFieldUnicodeEscapeWithLowercaseHexLetters.JsonOutput \
--test Required.Proto3.JsonInput.StringRepeatedField.JsonOutput \
--test Required.Proto3.JsonInput.Struct.JsonOutput \
--test Required.Proto3.JsonInput.StructWithEmptyListValue.JsonOutput \
--test Required.Proto3.JsonInput.TimestampLeap.JsonOutput \
--test Required.Proto3.JsonInput.TimestampMaxValue.JsonOutput \
--test Required.Proto3.JsonInput.TimestampMinValue.JsonOutput \
--test Required.Proto3.JsonInput.TimestampNull.JsonOutput \
--test Required.Proto3.JsonInput.TimestampRepeatedValue.JsonOutput \
--test Required.Proto3.JsonInput.TimestampWithNegativeOffset.JsonOutput \
--test Required.Proto3.JsonInput.TimestampWithPositiveOffset.JsonOutput \
--test Required.Proto3.JsonInput.Uint32FieldMaxFloatValue.JsonOutput \
--test Required.Proto3.JsonInput.Uint32FieldMaxValue.JsonOutput \
--test Required.Proto3.JsonInput.Uint32MapField.JsonOutput \
--test Required.Proto3.JsonInput.Uint64FieldMaxValue.JsonOutput \
--test Required.Proto3.JsonInput.Uint64FieldMaxValueNotQuoted.JsonOutput \
--test Required.Proto3.JsonInput.Uint64MapField.JsonOutput \
--test Required.Proto3.JsonInput.ValueAcceptBool.JsonOutput \
--test Required.Proto3.JsonInput.ValueAcceptFloat.JsonOutput \
--test Required.Proto3.JsonInput.ValueAcceptInteger.JsonOutput \
--test Required.Proto3.JsonInput.ValueAcceptList.JsonOutput \
--test Required.Proto3.JsonInput.ValueAcceptNull.JsonOutput \
--test Required.Proto3.JsonInput.ValueAcceptObject.JsonOutput \
--test Required.Proto3.JsonInput.ValueAcceptString.JsonOutput \
--test Required.Proto3.JsonInput.WrapperTypesWithNullValue.JsonOutput \
--test Required.Proto3.JsonInput.AnyNull.JsonOutput \
--test Required.Proto3.JsonInput.AnyWithNoType.JsonOutput \
--test Required.Proto3.JsonInput.DoubleFieldQuotedExponentialValue.JsonOutput \
--test Required.Proto3.JsonInput.FloatFieldQuotedExponentialValue.JsonOutput \
--test Required.Proto3.JsonInput.Int32FieldQuotedExponentialValue.JsonOutput \
--test Required.Proto3.JsonInput.Int32FieldStringValueZero.JsonOutput \
--test Required.Proto3.JsonInput.TimestampEpochValue.JsonOutput \
--test Required.Proto3.JsonInput.TimestampLittleAfterEpochlValue.JsonOutput \
--test Required.Proto3.JsonInput.TimestampLittleBeforeEpochValue.JsonOutput \
--test Required.Proto3.JsonInput.TimestampNanoAfterEpochlValue.JsonOutput \
--test Required.Proto3.JsonInput.TimestampNanoBeforeEpochValue.JsonOutput \
--test Required.Proto3.JsonInput.TimestampTenAndHalfSecondsAfterEpochValue.JsonOutput \
--test Required.Proto3.JsonInput.TimestampTenAndHalfSecondsBeforeEpochValue.JsonOutput \
--failure_list known_failures.txt \
conformance.sh
