/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.type;

import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.SerializationHelper;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.type.BasicTypeReference;
import org.hibernate.type.SerializableType;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;
import org.hibernate.type.TypeHelper;
import org.hibernate.type.spi.TypeConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Currency;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * @author Steve Ebersole
 */
@BaseUnitTest
public class TypeTest {

	@Test
	public void testBigDecimalType() {
		final BigDecimal original = BigDecimal.valueOf( 100 );
		final BigDecimal copy = BigDecimal.valueOf( 100 );
		final BigDecimal different = BigDecimal.valueOf( 999 );

		runBasicTests( StandardBasicTypes.BIG_DECIMAL, original, copy, different );
	}

	@Test
	public void testBigIntegerType() {
		final BigInteger original = BigInteger.valueOf( 100 );
		final BigInteger copy = BigInteger.valueOf( 100 );
		final BigInteger different = BigInteger.valueOf( 999 );

		runBasicTests( StandardBasicTypes.BIG_INTEGER, original, copy, different );
	}

	@Test
	public void testBinaryType() {
		final byte[] original = new byte[] { 1, 2, 3, 4 };
		final byte[] copy = new byte[] { 1, 2, 3, 4 };
		final byte[] different = new byte[] { 4, 3, 2, 1 };

		runBasicTests( StandardBasicTypes.BINARY, original, copy, different );
		runBasicTests( StandardBasicTypes.IMAGE, original, copy, different );
		runBasicTests( StandardBasicTypes.MATERIALIZED_BLOB, original, copy, different );
	}

	@Test
	public void testBooleanType() {
		final Boolean original = Boolean.TRUE;
		//noinspection UnnecessaryBoxing,removal,BooleanConstructorCall
		final Boolean copy = new Boolean( true );
		final Boolean different = Boolean.FALSE;

		runBasicTests( StandardBasicTypes.BOOLEAN, original, copy, different );
		runBasicTests( StandardBasicTypes.NUMERIC_BOOLEAN, original, copy, different );
		runBasicTests( StandardBasicTypes.YES_NO, original, copy, different );
		runBasicTests( StandardBasicTypes.TRUE_FALSE, original, copy, different );
	}

	@Test
	public void testByteType() {
		final Byte original = 0;
		//noinspection removal,UnnecessaryBoxing,CachedNumberConstructorCall
		final Byte copy = new Byte( (byte) 0 );
		final Byte different = 9;

		runBasicTests( StandardBasicTypes.BYTE, original, copy, different );
	}

	@Test
	public void testCalendarDateType() {
		final long now = System.currentTimeMillis();
		final Calendar original = new GregorianCalendar();
		original.clear();
		original.setTimeInMillis( now );
		final Calendar copy = new GregorianCalendar();
		copy.clear();
		copy.setTimeInMillis( now );
		final Calendar different = new GregorianCalendar();
		different.clear();
		different.setTimeInMillis( now );
		different.add( Calendar.MONTH, 1 );

		runBasicTests( StandardBasicTypes.CALENDAR_DATE, original, copy, different );
	}

	@Test
	public void testCalendarType() {
		final long now = System.currentTimeMillis();
		final Calendar original = new GregorianCalendar();
		original.clear();
		original.setTimeInMillis( now );
		final Calendar copy = new GregorianCalendar();
		copy.clear();
		copy.setTimeInMillis( now );
		final Calendar different = new GregorianCalendar();
		different.setTimeInMillis( now + 9999 );

		runBasicTests( StandardBasicTypes.CALENDAR, original, copy, different );
	}

	@Test
	public void testCharacterArrayType() {
		final Character[] original = new Character[] { 'a', 'b' };
		final Character[] copy = new Character[] { 'a', 'b' };
		final Character[] different = new Character[] { 'a', 'b', 'c' };

		runBasicTests( StandardBasicTypes.CHARACTER_ARRAY, original, copy, different );
	}

	@Test
	public void testCharacterType() {
		final Character original = 'a';
		//noinspection UnnecessaryBoxing,removal
		final Character copy = new Character( 'a' );
		final Character different = 'b';

		runBasicTests( StandardBasicTypes.CHARACTER, original, copy, different );
	}

	@Test
	public void testCharArrayType() {
		final char[] original = new char[] { 'a', 'b' };
		final char[] copy = new char[] { 'a', 'b' };
		final char[] different = new char[] { 'a', 'b', 'c' };

		runBasicTests( StandardBasicTypes.CHAR_ARRAY, original, copy, different );
		runBasicTests( StandardBasicTypes.CHAR_ARRAY, original, copy, different );
	}

	@Test
	public void testClassType() {
		final Class<?> original = TypeTest.class;
		final Class<?> copy = (Class<?>) SerializationHelper.clone( original );
		final Class<?> different = String.class;

		runBasicTests( StandardBasicTypes.CLASS, original, copy, different );
	}

	@Test
	public void testCurrencyType() {
		final Currency original = Currency.getInstance( Locale.US );
		final Currency copy = Currency.getInstance( Locale.US );
		final Currency different = Currency.getInstance( Locale.UK );

		runBasicTests( StandardBasicTypes.CURRENCY, original, copy, different );
	}

	@Test
	public void testDateType() {
		final long now = System.currentTimeMillis();
		final java.sql.Date original = new java.sql.Date( now );
		final java.sql.Date copy = new java.sql.Date( now );
		Calendar cal = new GregorianCalendar();
		cal.clear();
		cal.setTimeInMillis( now );
		cal.add( Calendar.YEAR, 1 );
		final java.sql.Date different = new java.sql.Date( cal.getTime().getTime() );

		runBasicTests( StandardBasicTypes.DATE, original, copy, different );
	}

	@Test
	public void testDoubleType() {
		final Double original = 100.0;
		final Double copy = 100.0;
		final Double different = 999.0;

		runBasicTests( StandardBasicTypes.DOUBLE, original, copy, different );
	}

	@Test
	public void testFloatType() {
		final Float original = 100F;
		final Float copy = 100F;
		final Float different = 999F;

		runBasicTests( StandardBasicTypes.FLOAT, original, copy, different );
	}

	@Test
	public void testIntegerType() {
		final Integer original = 100;
		//noinspection UnnecessaryBoxing,removal,CachedNumberConstructorCall
		final Integer copy = new Integer( 100 );
		final Integer different = 999;

		runBasicTests( StandardBasicTypes.INTEGER, original, copy, different );
	}

	@Test
	public void testLocaleType() {
		final Locale original = new Locale( "ab" );
		final Locale copy = new Locale( "ab" );
		final Locale different = new Locale( "yz" );

		runBasicTests( StandardBasicTypes.LOCALE, original, copy, different );
	}

	@Test
	public void testLongType() {
		final Long original = 100L;
		//noinspection UnnecessaryBoxing,CachedNumberConstructorCall,removal
		final Long copy = new Long( 100L );
		final Long different = 999L;

		runBasicTests( StandardBasicTypes.LONG, original, copy, different );
	}

	private static class SerializableImpl implements Serializable {
		private final int number;
		SerializableImpl(int number) {
			this.number = number;
		}
		@SuppressWarnings( {"EqualsWhichDoesntCheckParameterClass"})
		public boolean equals(Object obj) {
			return this.number == ( (SerializableImpl) obj ).number;
		}
	}

	@Test
	public void testSerializableType() {
		final SerializableImpl original = new SerializableImpl(1);
		final SerializableImpl copy = new SerializableImpl(1);
		final SerializableImpl different = new SerializableImpl(2);

		runBasicTests( StandardBasicTypes.SERIALIZABLE, original, copy, different );
		runBasicTests( new SerializableType<>( SerializableImpl.class ), original, copy, different );
	}

	@Test
	public void testShortType() {
		final Short original = 100;
		//noinspection UnnecessaryBoxing,CachedNumberConstructorCall,removal
		final Short copy = new Short( (short) 100 );
		final Short different = 999;

		runBasicTests( StandardBasicTypes.SHORT, original, copy, different );
	}

	@Test
	public void testStringType() {
		final String original = "abc";
		final String copy = new String( original.toCharArray() );
		final String different = "xyz";

		runBasicTests( StandardBasicTypes.STRING, original, copy, different );
		runBasicTests( StandardBasicTypes.TEXT, original, copy, different );
		runBasicTests( StandardBasicTypes.MATERIALIZED_CLOB, original, copy, different );
	}

	@Test
	public void testTimestampType() {
		final long now = System.currentTimeMillis();
		final Timestamp original = new Timestamp( now );
		final Timestamp copy = new Timestamp( now );
		final Timestamp different = new Timestamp( now + 9999 );

		runBasicTests( StandardBasicTypes.TIMESTAMP, original, copy, different );
	}

	@Test
	public void testTimeType() {
		final long now = System.currentTimeMillis();
		final Time original = new Time( now );
		final Time copy = new Time( now );
		final Time different = new Time( now + 9999 );

		runBasicTests( StandardBasicTypes.TIME, original, copy, different );
	}

	@Test
	public void testDates() {
		final long now = System.currentTimeMillis();
		final java.util.Date original = new java.util.Date( now );
		final java.util.Date copy = new java.util.Date( now );
		final java.util.Date different = new java.util.Date( now + 9999 );
		final java.util.Date different2 = new java.util.Date( now + ( 1000L * 60L * 60L * 24L * 365L ) );

		runBasicTests( StandardBasicTypes.TIME, original, copy, different );
		runBasicTests( StandardBasicTypes.TIMESTAMP, original, copy, different );
		runBasicTests( StandardBasicTypes.DATE, original, copy, different2 );
	}

	@Test
	public void testTimeZoneType() {
		final TimeZone original = new SimpleTimeZone( -1, "abc" );
		final TimeZone copy = new SimpleTimeZone( -1, "abc" );
		final TimeZone different = new SimpleTimeZone( -2, "xyz" );

		runBasicTests( StandardBasicTypes.TIMEZONE, original, copy, different );
	}

	protected <T> void runBasicTests(
			BasicTypeReference<T> basicTypeReference,
			T original,
			T copy,
			T different) {
		final TypeConfiguration typeConfiguration = new TypeConfiguration();
		runBasicTests(
				typeConfiguration.getBasicTypeRegistry().getRegisteredType( basicTypeReference.getName() ),
				original,
				copy,
				different
		);
	}

	protected <T> void runBasicTests(Type type, T original, T copy, T different) {
		final SharedSessionContractImplementor session = null; //Not really used
		final boolean nonCopyable = original instanceof Class || original instanceof Currency;
		if ( ! nonCopyable ) {
			// these checks exclude classes which cannot really be cloned (singetons/enums)
			assertNotSame( original, copy );
		}

		Assertions.assertSame( original, type.replace( original, copy, null, null, null ) );

		// following tests assert that types work with properties not yet loaded in bytecode enhanced entities
		Type[] types = new Type[]{ type };
		Assertions.assertSame( copy, TypeHelper.replace( new Object[]{ LazyPropertyInitializer.UNFETCHED_PROPERTY },
				new Object[]{ copy }, types, null, null, null )[0] );
		Assertions.assertNotEquals( LazyPropertyInitializer.UNFETCHED_PROPERTY, TypeHelper.replace( new Object[]{ original },
				new Object[]{ LazyPropertyInitializer.UNFETCHED_PROPERTY }, types, null, null, null )[0] );

		assertTrue( type.isSame( original, copy ) );
		assertTrue( type.isEqual( original, copy ) );
		assertTrue( type.isEqual( original, copy ) );
		assertTrue( type.isEqual( original, copy, null ) );

		assertFalse( type.isSame( original, different ) );
		assertFalse( type.isEqual( original, different ) );
		assertFalse( type.isEqual( original, different ) );
		assertFalse( type.isEqual( original, different, null ) );

		assertFalse( type.isDirty( original, copy , session ) );
		assertFalse( type.isDirty( original, copy , ArrayHelper.FALSE, session ) );
		assertFalse( type.isDirty( original, copy , ArrayHelper.TRUE, session ) );

		assertTrue( type.isDirty( original, different , session ) );
		assertFalse( type.isDirty( original, different , ArrayHelper.FALSE, session ) );
		assertTrue( type.isDirty( original, different , ArrayHelper.TRUE, session ) );

		assertFalse( type.isModified( original, copy, ArrayHelper.FALSE, session ) );
		assertFalse( type.isModified( original, copy, ArrayHelper.TRUE, session ) );

		assertTrue( type.isModified( original, different, ArrayHelper.FALSE, session ) );
		assertTrue( type.isModified( original, different, ArrayHelper.TRUE, session ) );
	}

}
