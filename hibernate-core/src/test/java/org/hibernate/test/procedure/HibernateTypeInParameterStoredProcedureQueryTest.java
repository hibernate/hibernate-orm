package org.hibernate.test.procedure;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.hibernate.type.*;
import org.junit.Test;

import javax.persistence.ParameterMode;
import javax.sql.rowset.serial.SerialBlob;
import javax.sql.rowset.serial.SerialClob;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import java.util.Date;

@TestForIssue(jiraKey = "HHH-12661")
public class HibernateTypeInParameterStoredProcedureQueryTest extends BaseNonConfigCoreFunctionalTestCase {

    private static final String TEST_STRING = "test_string";
    private static final char[] TEST_CHAR_ARRAY = TEST_STRING.toCharArray();
    private static final byte[] TEST_BYTE_ARRAY = TEST_STRING.getBytes();

    @Test
    public void testStringTypeInParameter() {
        inTransaction(
                session -> session.createStoredProcedureQuery("test")
                        .registerStoredProcedureParameter(1, StringType.class, ParameterMode.IN)
                        .setParameter(1, TEST_STRING)
        );
    }

    @Test
    public void testMaterializedClobTypeInParameter() {
        inTransaction(
                session -> session.createStoredProcedureQuery("test")
                        .registerStoredProcedureParameter(1, MaterializedClobType.class, ParameterMode.IN)
                        .setParameter(1, TEST_STRING)
        );
    }

    @Test
    public void testTextTypeInParameter() {
        inTransaction(
                session -> session.createStoredProcedureQuery("test")
                        .registerStoredProcedureParameter(1, TextType.class, ParameterMode.IN)
                        .setParameter(1, TEST_STRING)
        );
    }

    @Test
    public void testCharacterTypeInParameter() {
        inTransaction(
                session -> session.createStoredProcedureQuery("test")
                        .registerStoredProcedureParameter(1, CharacterType.class, ParameterMode.IN)
                        .setParameter(1, 'a')
        );
    }

    @Test
    public void testNumericBooleanTypeInParameter() {
        inTransaction(
                session -> session.createStoredProcedureQuery("test")
                        .registerStoredProcedureParameter(1, NumericBooleanType.class, ParameterMode.IN)
                        .setParameter(1, false)
        );
    }

    @Test
    public void testYesNoTypeInParameter() {
        inTransaction(
                session -> session.createStoredProcedureQuery("test")
                        .registerStoredProcedureParameter(1, YesNoType.class, ParameterMode.IN)
                        .setParameter(1, false)
        );
    }

    @Test
    public void testTrueFalseTypeInParameter() {
        inTransaction(
                session -> session.createStoredProcedureQuery("test")
                        .registerStoredProcedureParameter(1, TrueFalseType.class, ParameterMode.IN)
                        .setParameter(1, false)
        );
    }

    @Test
    public void testBooleanTypeInParameter() {
        inTransaction(
                session -> session.createStoredProcedureQuery("test")
                        .registerStoredProcedureParameter(1, BooleanType.class, ParameterMode.IN)
                        .setParameter(1, false)
        );
    }

    @Test
    public void testByteTypeInParameter() {
        inTransaction(
                session -> session.createStoredProcedureQuery("test")
                        .registerStoredProcedureParameter(1, ByteType.class, ParameterMode.IN)
                        .setParameter(1, (byte) 'a')
        );
    }

    @Test
    public void testShortTypeInParameter() {
        inTransaction(
                session -> session.createStoredProcedureQuery("test")
                        .registerStoredProcedureParameter(1, ShortType.class, ParameterMode.IN)
                        .setParameter(1, (short) 2)
        );
    }

    @Test
    public void testIntegerTypeInParameter() {
        inTransaction(
                session -> session.createStoredProcedureQuery("test")
                        .registerStoredProcedureParameter(1, IntegerType.class, ParameterMode.IN)
                        .setParameter(1, 2)
        );
    }

    @Test
    public void testLongTypeInParameter() {
        inTransaction(
                session -> session.createStoredProcedureQuery("test")
                        .registerStoredProcedureParameter(1, LongType.class, ParameterMode.IN)
                        .setParameter(1, 2L)
        );
    }

    @Test
    public void testFloatTypeInParameter() {
        inTransaction(
                session -> session.createStoredProcedureQuery("test")
                        .registerStoredProcedureParameter(1, FloatType.class, ParameterMode.IN)
                        .setParameter(1, 2.0F)
        );
    }

    @Test
    public void testDoubleTypeInParameter() {
        inTransaction(
                session -> session.createStoredProcedureQuery("test")
                        .registerStoredProcedureParameter(1, DoubleType.class, ParameterMode.IN)
                        .setParameter(1, 2.0D)
        );
    }

    @Test
    public void testBigIntegerTypeInParameter() {
        inTransaction(
                session -> session.createStoredProcedureQuery("test")
                        .registerStoredProcedureParameter(1, BigIntegerType.class, ParameterMode.IN)
                        .setParameter(1, BigInteger.ONE)
        );
    }

    @Test
    public void testBigDecimalTypeInParameter() {
        inTransaction(
                session -> session.createStoredProcedureQuery("test")
                        .registerStoredProcedureParameter(1, BigDecimalType.class, ParameterMode.IN)
                        .setParameter(1, BigDecimal.ONE)
        );
    }

    @Test
    public void testTimestampTypeDateInParameter() {
        inTransaction(
                session -> session.createStoredProcedureQuery("test")
                        .registerStoredProcedureParameter(1, TimestampType.class, ParameterMode.IN)
                        .setParameter(1, new Date())
        );
    }

    @Test
    public void testTimestampTypeTimestampInParameter() {
        inTransaction(
                session -> session.createStoredProcedureQuery("test")
                        .registerStoredProcedureParameter(1, TimestampType.class, ParameterMode.IN)
                        .setParameter(1, Timestamp.valueOf(LocalDateTime.now()))
        );
    }

    @Test
    public void testTimeTypeInParameter() {
        inTransaction(
                session -> session.createStoredProcedureQuery("test")
                        .registerStoredProcedureParameter(1, TimeType.class, ParameterMode.IN)
                        .setParameter(1, Time.valueOf(LocalTime.now()))
        );
    }

    @Test
    public void testDateTypeInParameter() {
        inTransaction(
                session -> session.createStoredProcedureQuery("test")
                        .registerStoredProcedureParameter(1, DateType.class, ParameterMode.IN)
                        .setParameter(1, java.sql.Date.valueOf(LocalDate.now()))
        );
    }

    @Test
    public void testCalendarTypeCalendarInParameter() {
        inTransaction(
                session -> session.createStoredProcedureQuery("test")
                        .registerStoredProcedureParameter(1, CalendarType.class, ParameterMode.IN)
                        .setParameter(1, Calendar.getInstance())
        );
    }

    @Test
    public void testCurrencyTypeInParameter() {
        inTransaction(
                session -> session.createStoredProcedureQuery("test")
                        .registerStoredProcedureParameter(1, CurrencyType.class, ParameterMode.IN)
                        .setParameter(1, Currency.getAvailableCurrencies().iterator().next())
        );
    }

    @Test
    public void testLocaleTypeInParameter() {
        inTransaction(
                session -> session.createStoredProcedureQuery("test")
                        .registerStoredProcedureParameter(1, LocaleType.class, ParameterMode.IN)
                        .setParameter(1, Locale.ENGLISH)
        );
    }

    @Test
    public void testTimeZoneTypeInParameter() {
        inTransaction(
                session -> session.createStoredProcedureQuery("test")
                        .registerStoredProcedureParameter(1, TimeZoneType.class, ParameterMode.IN)
                        .setParameter(1, TimeZone.getTimeZone(ZoneId.systemDefault()))
        );
    }

    @Test
    public void testUrlTypeInParameter() throws MalformedURLException {
        final URL url = new URL("http://example.com");
        inTransaction(
                session -> session.createStoredProcedureQuery("test")
                        .registerStoredProcedureParameter(1, UrlType.class, ParameterMode.IN)
                        .setParameter(1, url)
        );
    }

    @Test
    public void testClassTypeInParameter() {
        inTransaction(
                session -> session.createStoredProcedureQuery("test")
                        .registerStoredProcedureParameter(1, ClassType.class, ParameterMode.IN)
                        .setParameter(1, Class.class)
        );
    }

    @Test
    public void testBlobTypeInParameter() throws SQLException {
        final Blob blob = new SerialBlob(TEST_BYTE_ARRAY);
        inTransaction(
                session -> session.createStoredProcedureQuery("test")
                        .registerStoredProcedureParameter(1, BlobType.class, ParameterMode.IN)
                        .setParameter(1, blob)
        );
    }

    @Test
    public void testClobTypeInParameter() throws SQLException {
        final Clob clob = new SerialClob(TEST_CHAR_ARRAY);
        inTransaction(
                session -> session.createStoredProcedureQuery("test")
                        .registerStoredProcedureParameter(1, ClobType.class, ParameterMode.IN)
                        .setParameter(1, clob)
        );
    }

    @Test
    public void testBinaryTypeInParameter() {
        inTransaction(
                session -> session.createStoredProcedureQuery("test")
                        .registerStoredProcedureParameter(1, BinaryType.class, ParameterMode.IN)
                        .setParameter(1, TEST_BYTE_ARRAY)
        );
    }

    @Test
    public void testCharArrayTypeInParameter() {
        inTransaction(
                session -> session.createStoredProcedureQuery("test")
                        .registerStoredProcedureParameter(1, CharArrayType.class, ParameterMode.IN)
                        .setParameter(1, TEST_CHAR_ARRAY)
        );
    }

    @Test
    public void testUUIDBinaryTypeInParameter() {
        inTransaction(
                session -> session.createStoredProcedureQuery("test")
                        .registerStoredProcedureParameter(1, UUIDBinaryType.class, ParameterMode.IN)
                        .setParameter(1, UUID.randomUUID())
        );
    }

}
