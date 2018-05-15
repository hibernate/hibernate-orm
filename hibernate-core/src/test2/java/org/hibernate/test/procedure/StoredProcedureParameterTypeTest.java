/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.procedure;

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
import java.util.UUID;
import javax.persistence.ParameterMode;
import javax.sql.rowset.serial.SerialBlob;
import javax.sql.rowset.serial.SerialClob;

import org.hibernate.procedure.ProcedureCall;
import org.hibernate.type.BigDecimalType;
import org.hibernate.type.BigIntegerType;
import org.hibernate.type.BinaryType;
import org.hibernate.type.BlobType;
import org.hibernate.type.BooleanType;
import org.hibernate.type.ByteType;
import org.hibernate.type.CalendarType;
import org.hibernate.type.CharArrayType;
import org.hibernate.type.CharacterType;
import org.hibernate.type.ClassType;
import org.hibernate.type.ClobType;
import org.hibernate.type.CurrencyType;
import org.hibernate.type.DateType;
import org.hibernate.type.DoubleType;
import org.hibernate.type.FloatType;
import org.hibernate.type.IntegerType;
import org.hibernate.type.LocaleType;
import org.hibernate.type.LongType;
import org.hibernate.type.MaterializedClobType;
import org.hibernate.type.NumericBooleanType;
import org.hibernate.type.ShortType;
import org.hibernate.type.StringType;
import org.hibernate.type.TextType;
import org.hibernate.type.TimeType;
import org.hibernate.type.TimeZoneType;
import org.hibernate.type.TimestampType;
import org.hibernate.type.TrueFalseType;
import org.hibernate.type.UUIDBinaryType;
import org.hibernate.type.UrlType;
import org.hibernate.type.YesNoType;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
public class StoredProcedureParameterTypeTest extends BaseNonConfigCoreFunctionalTestCase {

	private static final String TEST_STRING = "test_string";
	private static final char[] TEST_CHAR_ARRAY = TEST_STRING.toCharArray();
	private static final byte[] TEST_BYTE_ARRAY = TEST_STRING.getBytes();

	@Test
	@TestForIssue( jiraKey = "HHH-12661" )
	public void testNumericBooleanTypeInParameter() {
		doInHibernate( this::sessionFactory, session -> {
			session.createStoredProcedureQuery( "test" )
					.registerStoredProcedureParameter( 1, NumericBooleanType.class, ParameterMode.IN )
					.registerStoredProcedureParameter( 2, String.class, ParameterMode.OUT )
					.setParameter( 1, false );
		} );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12661" )
	public void testYesNoTypeInParameter() {
		doInHibernate( this::sessionFactory, session -> {
			session.createStoredProcedureQuery( "test" )
					.registerStoredProcedureParameter( 1, YesNoType.class, ParameterMode.IN )
					.registerStoredProcedureParameter( 2, String.class, ParameterMode.OUT )
					.setParameter( 1, false );
		} );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12661" )
	public void testStringTypeInParameter() {
		inTransaction(
				session -> session.createStoredProcedureQuery("test")
						.registerStoredProcedureParameter( 1, StringType.class, ParameterMode.IN)
						.setParameter(1, TEST_STRING)
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12661" )
	public void testMaterializedClobTypeInParameter() {
		inTransaction(
				session -> session.createStoredProcedureQuery("test")
						.registerStoredProcedureParameter( 1, MaterializedClobType.class, ParameterMode.IN)
						.setParameter(1, TEST_STRING)
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12661" )
	public void testTextTypeInParameter() {
		inTransaction(
				session -> session.createStoredProcedureQuery("test")
						.registerStoredProcedureParameter( 1, TextType.class, ParameterMode.IN)
						.setParameter(1, TEST_STRING)
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12661" )
	public void testCharacterTypeInParameter() {
		inTransaction(
				session -> session.createStoredProcedureQuery("test")
						.registerStoredProcedureParameter( 1, CharacterType.class, ParameterMode.IN)
						.setParameter(1, 'a')
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12661" )
	public void testTrueFalseTypeInParameter() {
		inTransaction(
				session -> session.createStoredProcedureQuery("test")
						.registerStoredProcedureParameter( 1, TrueFalseType.class, ParameterMode.IN)
						.setParameter(1, false)
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12661" )
	public void testBooleanTypeInParameter() {
		inTransaction(
				session -> session.createStoredProcedureQuery("test")
						.registerStoredProcedureParameter( 1, BooleanType.class, ParameterMode.IN)
						.setParameter(1, false)
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12661" )
	public void testByteTypeInParameter() {
		inTransaction(
				session -> session.createStoredProcedureQuery("test")
						.registerStoredProcedureParameter( 1, ByteType.class, ParameterMode.IN)
						.setParameter(1, (byte) 'a')
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12661" )
	public void testShortTypeInParameter() {
		inTransaction(
				session -> session.createStoredProcedureQuery("test")
						.registerStoredProcedureParameter( 1, ShortType.class, ParameterMode.IN)
						.setParameter(1, (short) 2)
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12661" )
	public void testIntegerTypeInParameter() {
		inTransaction(
				session -> session.createStoredProcedureQuery("test")
						.registerStoredProcedureParameter( 1, IntegerType.class, ParameterMode.IN)
						.setParameter(1, 2)
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12661" )
	public void testLongTypeInParameter() {
		inTransaction(
				session -> session.createStoredProcedureQuery("test")
						.registerStoredProcedureParameter( 1, LongType.class, ParameterMode.IN)
						.setParameter(1, 2L)
		);
	}

	@Test
	public void testFloatTypeInParameter() {
		inTransaction(
				session -> session.createStoredProcedureQuery("test")
						.registerStoredProcedureParameter( 1, FloatType.class, ParameterMode.IN)
						.setParameter(1, 2.0F)
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12661" )
	public void testDoubleTypeInParameter() {
		inTransaction(
				session -> session.createStoredProcedureQuery("test")
						.registerStoredProcedureParameter( 1, DoubleType.class, ParameterMode.IN)
						.setParameter(1, 2.0D)
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12661" )
	public void testBigIntegerTypeInParameter() {
		inTransaction(
				session -> session.createStoredProcedureQuery("test")
						.registerStoredProcedureParameter( 1, BigIntegerType.class, ParameterMode.IN)
						.setParameter( 1, BigInteger.ONE)
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12661" )
	public void testBigDecimalTypeInParameter() {
		inTransaction(
				session -> session.createStoredProcedureQuery("test")
						.registerStoredProcedureParameter( 1, BigDecimalType.class, ParameterMode.IN)
						.setParameter( 1, BigDecimal.ONE)
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12661" )
	public void testTimestampTypeDateInParameter() {
		inTransaction(
				session -> session.createStoredProcedureQuery("test")
						.registerStoredProcedureParameter( 1, TimestampType.class, ParameterMode.IN)
						.setParameter(1, new Date())
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12661" )
	public void testTimestampTypeTimestampInParameter() {
		inTransaction(
				session -> session.createStoredProcedureQuery("test")
						.registerStoredProcedureParameter(1, TimestampType.class, ParameterMode.IN)
						.setParameter( 1, Timestamp.valueOf( LocalDateTime.now()))
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12661" )
	public void testTimeTypeInParameter() {
		inTransaction(
				session -> session.createStoredProcedureQuery("test")
						.registerStoredProcedureParameter( 1, TimeType.class, ParameterMode.IN)
						.setParameter( 1, Time.valueOf( LocalTime.now()))
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12661" )
	public void testDateTypeInParameter() {
		inTransaction(
				session -> session.createStoredProcedureQuery("test")
						.registerStoredProcedureParameter( 1, DateType.class, ParameterMode.IN)
						.setParameter(1, java.sql.Date.valueOf( LocalDate.now()))
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12661" )
	public void testCalendarTypeCalendarInParameter() {
		inTransaction(
				session -> session.createStoredProcedureQuery("test")
						.registerStoredProcedureParameter( 1, CalendarType.class, ParameterMode.IN)
						.setParameter( 1, Calendar.getInstance())
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12661" )
	public void testCurrencyTypeInParameter() {
		inTransaction(
				session -> session.createStoredProcedureQuery("test")
						.registerStoredProcedureParameter( 1, CurrencyType.class, ParameterMode.IN)
						.setParameter( 1, Currency.getAvailableCurrencies().iterator().next())
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12661" )
	public void testLocaleTypeInParameter() {
		inTransaction(
				session -> session.createStoredProcedureQuery("test")
						.registerStoredProcedureParameter( 1, LocaleType.class, ParameterMode.IN)
						.setParameter( 1, Locale.ENGLISH)
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12661" )
	public void testTimeZoneTypeInParameter() {
		inTransaction(
				session -> session.createStoredProcedureQuery("test")
						.registerStoredProcedureParameter( 1, TimeZoneType.class, ParameterMode.IN)
						.setParameter( 1, TimeZone.getTimeZone( ZoneId.systemDefault()))
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12661" )
	public void testUrlTypeInParameter() throws MalformedURLException {
		final URL url = new URL( "http://example.com");
		inTransaction(
				session -> session.createStoredProcedureQuery("test")
						.registerStoredProcedureParameter( 1, UrlType.class, ParameterMode.IN)
						.setParameter(1, url)
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12661" )
	public void testClassTypeInParameter() {
		inTransaction(
				session -> session.createStoredProcedureQuery("test")
						.registerStoredProcedureParameter( 1, ClassType.class, ParameterMode.IN)
						.setParameter(1, Class.class)
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12661" )
	public void testBlobTypeInParameter() throws SQLException {
		final Blob blob = new SerialBlob( TEST_BYTE_ARRAY);
		inTransaction(
				session -> session.createStoredProcedureQuery("test")
						.registerStoredProcedureParameter( 1, BlobType.class, ParameterMode.IN)
						.setParameter(1, blob)
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12661" )
	public void testClobTypeInParameter() throws SQLException {
		final Clob clob = new SerialClob( TEST_CHAR_ARRAY);
		inTransaction(
				session -> session.createStoredProcedureQuery("test")
						.registerStoredProcedureParameter( 1, ClobType.class, ParameterMode.IN)
						.setParameter(1, clob)
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12661" )
	public void testBinaryTypeInParameter() {
		inTransaction(
				session -> session.createStoredProcedureQuery("test")
						.registerStoredProcedureParameter( 1, BinaryType.class, ParameterMode.IN)
						.setParameter(1, TEST_BYTE_ARRAY)
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12661" )
	public void testCharArrayTypeInParameter() {
		inTransaction(
				session -> session.createStoredProcedureQuery("test")
						.registerStoredProcedureParameter( 1, CharArrayType.class, ParameterMode.IN)
						.setParameter(1, TEST_CHAR_ARRAY)
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12661" )
	public void testUUIDBinaryTypeInParameter() {
		inTransaction(
				session -> session.createStoredProcedureQuery("test")
						.registerStoredProcedureParameter( 1, UUIDBinaryType.class, ParameterMode.IN)
						.setParameter( 1, UUID.randomUUID())
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-12905")
	public void testStringTypeInParameterIsNull() {
		inTransaction(
				session -> {
					ProcedureCall procedureCall = session.createStoredProcedureCall( "test" );
					procedureCall.registerParameter( 1, StringType.class, ParameterMode.IN ).enablePassingNulls( true );
					procedureCall.setParameter( 1, null );
				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-12905")
	public void testStringTypeInParameterIsNullWithoutEnablePassingNulls() {
		inTransaction(
				session -> {
					try {
						ProcedureCall procedureCall = session.createStoredProcedureCall( "test" );
						procedureCall.registerParameter( 1, StringType.class, ParameterMode.IN );
						procedureCall.setParameter( 1, null );

						fail("Should have thrown exception");
					}
					catch (IllegalArgumentException e) {
						assertTrue( e.getMessage().endsWith( "You need to call ParameterRegistration#enablePassingNulls(true) in order to pass null parameters." ) );
					}
				}
		);
	}
}