/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.procedure;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Currency;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import jakarta.persistence.ParameterMode;
import javax.sql.rowset.serial.SerialBlob;
import javax.sql.rowset.serial.SerialClob;

import org.hibernate.testing.util.uuid.SafeRandomUUIDGenerator;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.query.TypedParameterValue;
import org.hibernate.type.NumericBooleanConverter;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.TrueFalseConverter;
import org.hibernate.type.YesNoConverter;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Vlad Mihalcea
 * @author Yanming Zhou
 */
@DomainModel
@SessionFactory
public class StoredProcedureParameterTypeTest {

	private static final String TEST_STRING = "test_string";
	private static final char[] TEST_CHAR_ARRAY = TEST_STRING.toCharArray();
	private static final byte[] TEST_BYTE_ARRAY = TEST_STRING.getBytes();

	@Test
	@JiraKey( value = "HHH-12661" )
	public void testNumericBooleanTypeInParameter(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> session.createStoredProcedureQuery( "test" )
							.registerStoredProcedureParameter( 1, StandardBasicTypes.NUMERIC_BOOLEAN, ParameterMode.IN )
							.registerStoredProcedureParameter( 2, String.class, ParameterMode.OUT )
							.setParameter( 1, false )
		);
	}

	@Test
	@JiraKey( value = "HHH-12661" )
	public void testNumericBooleanTypeConverterInParameter(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> session.createStoredProcedureQuery( "test" )
						.registerStoredProcedureParameter( 1, NumericBooleanConverter.class, ParameterMode.IN )
						.registerStoredProcedureParameter( 2, String.class, ParameterMode.OUT )
						.setParameter( 1, false )
		);
	}

	@Test
	@JiraKey( value = "HHH-12661" )
	public void testYesNoTypeInParameter(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> session.createStoredProcedureQuery( "test" )
						.registerStoredProcedureParameter( 1, StandardBasicTypes.YES_NO, ParameterMode.IN )
						.registerStoredProcedureParameter( 2, String.class, ParameterMode.OUT )
						.setParameter( 1, false )
		);
	}

	@Test
	@JiraKey( value = "HHH-12661" )
	public void testYesNoTypeConverterInParameter(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> session.createStoredProcedureQuery( "test" )
							.registerStoredProcedureParameter( 1, YesNoConverter.class, ParameterMode.IN )
							.registerStoredProcedureParameter( 2, String.class, ParameterMode.OUT )
							.setParameter( 1, false )
		);
	}

	@Test
	@JiraKey( value = "HHH-12661" )
	public void testTrueFalseTypeInParameter(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> session.createStoredProcedureQuery("test")
						.registerStoredProcedureParameter( 1, StandardBasicTypes.TRUE_FALSE, ParameterMode.IN)
						.setParameter(1, false)
		);
	}

	@Test
	@JiraKey( value = "HHH-12661" )
	public void testTrueFalseTypeConverterInParameter(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> session.createStoredProcedureQuery("test")
						.registerStoredProcedureParameter( 1, TrueFalseConverter.class, ParameterMode.IN)
						.setParameter(1, false)
		);
	}

	@Test
	@JiraKey( value = "HHH-12661" )
	public void testStringTypeInParameter(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> session.createStoredProcedureQuery("test")
							.registerStoredProcedureParameter( 1, StandardBasicTypes.STRING, ParameterMode.IN)
							.setParameter(1, TEST_STRING)
		);
	}

	@Test
	@JiraKey( value = "HHH-12661" )
	public void testMaterializedClobTypeInParameter(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> session.createStoredProcedureQuery("test")
							.registerStoredProcedureParameter( 1, StandardBasicTypes.MATERIALIZED_CLOB, ParameterMode.IN)
							.setParameter(1, TEST_STRING)
		);
	}

	@Test
	@JiraKey( value = "HHH-12661" )
	public void testTextTypeInParameter(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> session.createStoredProcedureQuery("test")
							.registerStoredProcedureParameter( 1, StandardBasicTypes.TEXT, ParameterMode.IN)
							.setParameter(1, TEST_STRING)
		);
	}

	@Test
	@JiraKey( value = "HHH-12661" )
	public void testCharacterTypeInParameter(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> session.createStoredProcedureQuery("test")
							.registerStoredProcedureParameter( 1, StandardBasicTypes.CHARACTER, ParameterMode.IN)
							.setParameter(1, 'a')
		);
	}

	@Test
	@JiraKey( value = "HHH-12661" )
	public void testBooleanTypeInParameter(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> session.createStoredProcedureQuery("test")
							.registerStoredProcedureParameter( 1, StandardBasicTypes.BOOLEAN, ParameterMode.IN)
							.setParameter(1, false)
		);
	}

	@Test
	@JiraKey( value = "HHH-12661" )
	public void testByteTypeInParameter(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> session.createStoredProcedureQuery("test")
							.registerStoredProcedureParameter( 1, StandardBasicTypes.BYTE, ParameterMode.IN)
							.setParameter(1, (byte) 'a')
		);
	}

	@Test
	@JiraKey( value = "HHH-12661" )
	public void testShortTypeInParameter(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> session.createStoredProcedureQuery("test")
							.registerStoredProcedureParameter( 1, StandardBasicTypes.SHORT, ParameterMode.IN)
							.setParameter(1, (short) 2)
		);
	}

	@Test
	@JiraKey( value = "HHH-12661" )
	public void testIntegerTypeInParameter(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> session.createStoredProcedureQuery("test")
							.registerStoredProcedureParameter( 1, StandardBasicTypes.INTEGER, ParameterMode.IN)
							.setParameter(1, 2)
		);
	}

	@Test
	@JiraKey( value = "HHH-12661" )
	public void testLongTypeInParameter(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> session.createStoredProcedureQuery("test")
							.registerStoredProcedureParameter( 1, StandardBasicTypes.LONG, ParameterMode.IN)
							.setParameter(1, 2L)
		);
	}

	@Test
	public void testFloatTypeInParameter(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> session.createStoredProcedureQuery("test")
							.registerStoredProcedureParameter( 1, StandardBasicTypes.FLOAT, ParameterMode.IN)
							.setParameter(1, 2.0F)
		);
	}

	@Test
	@JiraKey( value = "HHH-12661" )
	public void testDoubleTypeInParameter(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> session.createStoredProcedureQuery("test")
							.registerStoredProcedureParameter( 1, StandardBasicTypes.DOUBLE, ParameterMode.IN)
							.setParameter(1, 2.0D)
		);
	}

	@Test
	@JiraKey( value = "HHH-12661" )
	public void testBigIntegerTypeInParameter(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> session.createStoredProcedureQuery("test")
							.registerStoredProcedureParameter( 1, StandardBasicTypes.BIG_INTEGER, ParameterMode.IN)
							.setParameter( 1, BigInteger.ONE)
		);
	}

	@Test
	@JiraKey( value = "HHH-12661" )
	public void testBigDecimalTypeInParameter(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> session.createStoredProcedureQuery("test")
							.registerStoredProcedureParameter( 1, StandardBasicTypes.BIG_DECIMAL, ParameterMode.IN)
							.setParameter( 1, BigDecimal.ONE)
		);
	}

	@Test
	@JiraKey( value = "HHH-12661" )
	public void testTimestampTypeDateInParameter(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> session.createStoredProcedureQuery("test")
							.registerStoredProcedureParameter( 1, StandardBasicTypes.TIMESTAMP, ParameterMode.IN)
							.setParameter(1, new Date())
		);
	}

	@Test
	@JiraKey( value = "HHH-12661" )
	public void testTimestampTypeTimestampInParameter(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> session.createStoredProcedureQuery("test")
							.registerStoredProcedureParameter(1, StandardBasicTypes.TIMESTAMP, ParameterMode.IN)
							.setParameter( 1, Timestamp.valueOf( LocalDateTime.now()))
		);
	}

	@Test
	@JiraKey( value = "HHH-12661" )
	public void testTimeTypeInParameter(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> session.createStoredProcedureQuery("test")
							.registerStoredProcedureParameter( 1, StandardBasicTypes.TIME, ParameterMode.IN)
							.setParameter( 1, Time.valueOf( LocalTime.now()))
		);
	}

	@Test
	@JiraKey( value = "HHH-12661" )
	public void testDateTypeInParameter(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> session.createStoredProcedureQuery("test")
							.registerStoredProcedureParameter( 1, StandardBasicTypes.DATE, ParameterMode.IN)
							.setParameter(1, java.sql.Date.valueOf( LocalDate.now()))
		);
	}

	@Test
	@JiraKey( value = "HHH-12661" )
	public void testCalendarTypeCalendarInParameter(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> session.createStoredProcedureQuery("test")
							.registerStoredProcedureParameter( 1, StandardBasicTypes.CALENDAR, ParameterMode.IN)
							.setParameter( 1, Calendar.getInstance())
		);
	}

	@Test
	@JiraKey( value = "HHH-12661" )
	public void testCurrencyTypeInParameter(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> session.createStoredProcedureQuery("test")
							.registerStoredProcedureParameter( 1, StandardBasicTypes.CURRENCY, ParameterMode.IN)
							.setParameter( 1, Currency.getAvailableCurrencies().iterator().next())
		);
	}

	@Test
	@JiraKey( value = "HHH-12661" )
	public void testLocaleTypeInParameter(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> session.createStoredProcedureQuery("test")
							.registerStoredProcedureParameter( 1, StandardBasicTypes.LOCALE, ParameterMode.IN)
							.setParameter( 1, Locale.ENGLISH)
		);
	}

	@Test
	@JiraKey( value = "HHH-12661" )
	public void testTimeZoneTypeInParameter(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> session.createStoredProcedureQuery("test")
							.registerStoredProcedureParameter( 1, StandardBasicTypes.TIMEZONE, ParameterMode.IN)
							.setParameter( 1, TimeZone.getTimeZone( ZoneId.systemDefault()))
		);
	}

	@Test
	@JiraKey( value = "HHH-12661" )
	public void testUrlTypeInParameter(SessionFactoryScope scope) throws MalformedURLException {
		final URL url = new URL( "http://example.com");
		scope.inTransaction(
				session -> session.createStoredProcedureQuery("test")
							.registerStoredProcedureParameter( 1, StandardBasicTypes.URL, ParameterMode.IN)
							.setParameter(1, url)
		);
	}

	@Test
	@JiraKey( value = "HHH-12661" )
	public void testClassTypeInParameter(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> session.createStoredProcedureQuery("test")
							.registerStoredProcedureParameter( 1, StandardBasicTypes.CLASS, ParameterMode.IN)
							.setParameter(1, Class.class)
		);
	}

	@Test
	@JiraKey( value = "HHH-12661" )
	public void testBlobTypeInParameter(SessionFactoryScope scope) throws SQLException {
		final Blob blob = new SerialBlob( TEST_BYTE_ARRAY);
		scope.inTransaction(
				session -> session.createStoredProcedureQuery("test")
							.registerStoredProcedureParameter( 1, StandardBasicTypes.BLOB, ParameterMode.IN)
							.setParameter(1, blob)
		);
	}

	@Test
	@JiraKey( value = "HHH-12661" )
	public void testClobTypeInParameter(SessionFactoryScope scope) throws SQLException {
		final Clob clob = new SerialClob( TEST_CHAR_ARRAY);
		scope.inTransaction(
				session -> session.createStoredProcedureQuery("test")
							.registerStoredProcedureParameter( 1, StandardBasicTypes.CLOB, ParameterMode.IN)
							.setParameter(1, clob)
		);
	}

	@Test
	@JiraKey( value = "HHH-12661" )
	public void testBinaryTypeInParameter(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> session.createStoredProcedureQuery("test")
							.registerStoredProcedureParameter( 1, StandardBasicTypes.BINARY, ParameterMode.IN)
							.setParameter(1, TEST_BYTE_ARRAY)
		);
	}

	@Test
	@JiraKey( value = "HHH-12661" )
	public void testCharArrayTypeInParameter(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> session.createStoredProcedureQuery("test")
							.registerStoredProcedureParameter( 1, StandardBasicTypes.CHAR_ARRAY, ParameterMode.IN)
							.setParameter(1, TEST_CHAR_ARRAY)
		);
	}

	@Test
	@JiraKey( value = "HHH-12661" )
	public void testUUIDBinaryTypeInParameter(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> session.createStoredProcedureQuery("test")
							.registerStoredProcedureParameter( 1, StandardBasicTypes.UUID_BINARY, ParameterMode.IN)
							.setParameter( 1, SafeRandomUUIDGenerator.safeRandomUUID())
		);
	}

	@Test
	@JiraKey(value = "HHH-12905")
	public void testStringTypeInParameterIsNull(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					ProcedureCall procedureCall = session.createStoredProcedureCall( "test" );
					procedureCall.registerParameter( 1, StandardBasicTypes.STRING, ParameterMode.IN );
					procedureCall.setParameter( 1, null );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-12905")
	public void testStringTypeInParameterIsNullWithoutEnablePassingNulls(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
						ProcedureCall procedureCall = session.createStoredProcedureCall( "test" );
						procedureCall.registerParameter( 1, StandardBasicTypes.STRING, ParameterMode.IN );
						procedureCall.setParameter( 1, null );
				}
		);
	}


	@Test
	@JiraKey(value = "HHH-15618")
	public void testTypedParameterValueInParameter(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					ProcedureCall procedureCall = session.createStoredProcedureCall( "test" );
					procedureCall.registerParameter( 1, StandardBasicTypes.STRING, ParameterMode.IN );
					procedureCall.setParameter( 1, TypedParameterValue.of( StandardBasicTypes.STRING, "test" ) );

					procedureCall = session.createStoredProcedureCall( "test" );
					procedureCall.registerParameter( "test", StandardBasicTypes.STRING, ParameterMode.IN );
					procedureCall.setParameter( "test", TypedParameterValue.of( StandardBasicTypes.STRING, "test" ) );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-15618")
	public void testTypedParameterValueInParameterWithNotSpecifiedType(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					try {
						ProcedureCall procedureCall = session.createStoredProcedureCall( "test" );
						procedureCall.registerParameter( 1, StandardBasicTypes.STRING, ParameterMode.IN );
						procedureCall.setParameter( 1, TypedParameterValue.of( StandardBasicTypes.INTEGER, 1 ) );
					}
					catch (IllegalArgumentException e) {
						assertTrue( e.getMessage().contains( "was not of specified type" ) );
					}
				}
		);
	}
}
