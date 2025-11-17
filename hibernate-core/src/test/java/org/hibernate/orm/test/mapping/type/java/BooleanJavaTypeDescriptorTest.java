/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.type.java;

import jakarta.persistence.AttributeConverter;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.dialect.Dialect;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.type.NumericBooleanConverter;
import org.hibernate.type.TrueFalseConverter;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.descriptor.java.BooleanJavaType;
import org.hibernate.type.descriptor.java.IntegerJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.StringJavaType;
import org.hibernate.type.descriptor.jdbc.IntegerJdbcType;
import org.hibernate.type.descriptor.jdbc.VarcharJdbcType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class BooleanJavaTypeDescriptorTest {
	private final BooleanJavaType underTest = new BooleanJavaType();

	@Test
	@JiraKey( "HHH-17275" )
	public void testCheckConditionShouldReturnCorrectStatementWhenNullXStringGiven() {
		// given
		// when
		String checkCondition = underTest.getCheckCondition("is_active", VarcharJdbcType.INSTANCE, new BooleanXConverter(), new AnyDialect());
		// then
		assertEquals( "is_active in ('X') or is_active is null", checkCondition );
	}

	@Test
	public void testCheckConditionShouldReturnCorrectStatementWhenTFStringGiven() {
		// given
		// when
		String checkCondition = underTest.getCheckCondition("is_active", VarcharJdbcType.INSTANCE, new TrueFalseConverter(), new AnyDialect());
		// then
		assertEquals( "is_active in ('F','T')", checkCondition );
	}

	@Test
	public void testCheckConditionShouldReturnCorrectStatementWhen1AndNullIntegerGiven() {
		// given
		// when
		String checkCondition = underTest.getCheckCondition("is_active", IntegerJdbcType.INSTANCE, new OneNullBooleanConverter(), new AnyDialect());
		// then
		assertEquals( "is_active in (1) or is_active is null", checkCondition );
	}

	@Test
	public void testCheckConditionShouldReturnCorrectStatementWhen1And0IntegerGiven() {
		// given
		// when
		String checkCondition = underTest.getCheckCondition("is_active", IntegerJdbcType.INSTANCE, new NumericBooleanConverter(), new AnyDialect());
		// then
		assertEquals("is_active in (0,1)", checkCondition);
	}

	@Test
	public void testCheckConditionShouldReturnCorrectStatementWhen1And0AndNullIntegerGiven() {
		// given
		// when
		String checkCondition = underTest.getCheckCondition("is_active", IntegerJdbcType.INSTANCE, new TriStateBooleanConverter(), new AnyDialect());
		// then
		assertEquals("is_active in (0,1,-1)", checkCondition);
	}

	@Test
	public void testWrapShouldReturnTrueWhenYStringGiven() {
		// given
		// when
		Boolean result = underTest.wrap("Y", null);
		// then
		assertTrue(result);
	}

	@Test
	public void testWrapShouldReturnFalseWhenFStringGiven() {
		// given
		// when
		Boolean result = underTest.wrap("N", null);
		// then
		assertFalse(result);
	}

	@Test
	public void testWrapShouldReturnFalseWhenRandomStringGiven() {
		// given
		// when
		Boolean result = underTest.wrap("k", null);
		// then
		assertFalse(result);
	}

	@Test
	public void testWrapShouldReturnNullWhenNullStringGiven() {
		// given
		// when
		Boolean result = underTest.wrap(null, null);
		// then
		assertNull(result);
	}

	@Test
	public void testWrapShouldReturnFalseWhenEmptyStringGiven() {
		// given
		// when
		Boolean result = underTest.wrap("", null);
		// then
		assertFalse(result);
	}

	private static class AnyDialect extends Dialect {

	}

	private static class BooleanXConverter implements AttributeConverter<Boolean, String>, BasicValueConverter<Boolean, String> {

		@Override
		public String convertToDatabaseColumn(Boolean value) {
			return value != null && value ? "X" : null;
		}

		@Override
		public Boolean convertToEntityAttribute(String value) {
			return value != null;
		}

		@Override
		public @Nullable Boolean toDomainValue(@Nullable String relationalForm) {
			return convertToEntityAttribute(relationalForm);
		}

		@Override
		public @Nullable String toRelationalValue(@Nullable Boolean domainForm) {
			return convertToDatabaseColumn(domainForm);
		}

		@Override
		public JavaType<Boolean> getDomainJavaType() {
			return BooleanJavaType.INSTANCE;
		}

		@Override
		public JavaType<String> getRelationalJavaType() {
			return StringJavaType.INSTANCE;
		}
	}

	private static class OneNullBooleanConverter implements AttributeConverter<Boolean, Integer>, BasicValueConverter<Boolean, Integer> {
		@Override
		public Integer convertToDatabaseColumn(Boolean attribute) {
			return attribute != null && attribute ? 1 : null;
		}

		@Override
		public Boolean convertToEntityAttribute(Integer dbData) {
			return dbData != null && dbData == 1;
		}

		@Override
		public @Nullable Boolean toDomainValue(@Nullable Integer relationalForm) {
			return convertToEntityAttribute(relationalForm);
		}

		@Override
		public @Nullable Integer toRelationalValue(@Nullable Boolean domainForm) {
			return convertToDatabaseColumn(domainForm);
		}

		@Override
		public JavaType<Boolean> getDomainJavaType() {
			return BooleanJavaType.INSTANCE;
		}

		@Override
		public JavaType<Integer> getRelationalJavaType() {
			return IntegerJavaType.INSTANCE;
		}
	}

	private static class TriStateBooleanConverter implements AttributeConverter<Boolean, Integer>, BasicValueConverter<Boolean, Integer> {
		@Override
		public Integer convertToDatabaseColumn(Boolean attribute) {
			if (attribute == null) return -1;
			return  attribute ? 1 : 0;
		}

		@Override
		public Boolean convertToEntityAttribute(Integer dbData) {
			if (dbData == null || dbData == -1) return null;
			return dbData == 1;
		}

		@Override
		public @Nullable Boolean toDomainValue(@Nullable Integer relationalForm) {
			return convertToEntityAttribute(relationalForm);
		}

		@Override
		public @Nullable Integer toRelationalValue(@Nullable Boolean domainForm) {
			return convertToDatabaseColumn(domainForm);
		}

		@Override
		public JavaType<Boolean> getDomainJavaType() {
			return BooleanJavaType.INSTANCE;
		}

		@Override
		public JavaType<Integer> getRelationalJavaType() {
			return IntegerJavaType.INSTANCE;
		}
	}
}
