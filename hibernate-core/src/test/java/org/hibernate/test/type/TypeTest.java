/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.type;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
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

import org.junit.Before;
import org.junit.Test;

import org.hibernate.EntityMode;
import org.hibernate.Session;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.util.SerializationHelper;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.BigDecimalType;
import org.hibernate.type.BigIntegerType;
import org.hibernate.type.BinaryType;
import org.hibernate.type.BooleanType;
import org.hibernate.type.ByteType;
import org.hibernate.type.CalendarDateType;
import org.hibernate.type.CalendarType;
import org.hibernate.type.CharArrayType;
import org.hibernate.type.CharacterArrayType;
import org.hibernate.type.CharacterType;
import org.hibernate.type.ClassType;
import org.hibernate.type.CurrencyType;
import org.hibernate.type.DateType;
import org.hibernate.type.DoubleType;
import org.hibernate.type.FloatType;
import org.hibernate.type.ImageType;
import org.hibernate.type.IntegerType;
import org.hibernate.type.LocaleType;
import org.hibernate.type.LongType;
import org.hibernate.type.MaterializedBlobType;
import org.hibernate.type.MaterializedClobType;
import org.hibernate.type.NumericBooleanType;
import org.hibernate.type.SerializableType;
import org.hibernate.type.ShortType;
import org.hibernate.type.StringType;
import org.hibernate.type.TextType;
import org.hibernate.type.TimeType;
import org.hibernate.type.TimeZoneType;
import org.hibernate.type.TimestampType;
import org.hibernate.type.TrueFalseType;
import org.hibernate.type.YesNoType;

import org.hibernate.testing.junit4.BaseUnitTestCase;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Steve Ebersole
 */
public class TypeTest extends BaseUnitTestCase {
	private SessionImplementor session;

	@Before
	public void setUp() throws Exception {
		session = (SessionImplementor) Proxy.newProxyInstance(
				getClass().getClassLoader(),
				new Class[] { Session.class, SessionImplementor.class },
				new SessionProxyHandler()
		);
	}

	public static class SessionProxyHandler implements InvocationHandler {
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			if ( "getEntityMode".equals( method.getName() ) ) {
				return EntityMode.POJO;
			}
			throw new UnsupportedOperationException( "Unexpected method call : " + method.getName() );
		}
	}

	@Test
	public void testBigDecimalType() {
		final BigDecimal original = BigDecimal.valueOf( 100 );
		final BigDecimal copy = BigDecimal.valueOf( 100 );
		final BigDecimal different = BigDecimal.valueOf( 999 );

		runBasicTests( BigDecimalType.INSTANCE, original, copy, different );
	}

	@Test
	public void testBigIntegerType() {
		final BigInteger original = BigInteger.valueOf( 100 );
		final BigInteger copy = BigInteger.valueOf( 100 );
		final BigInteger different = BigInteger.valueOf( 999 );

		runBasicTests( BigIntegerType.INSTANCE, original, copy, different );
	}

	@Test
	public void testBinaryType() {
		final byte[] original = new byte[] { 1, 2, 3, 4 };
		final byte[] copy = new byte[] { 1, 2, 3, 4 };
		final byte[] different = new byte[] { 4, 3, 2, 1 };

		runBasicTests( BinaryType.INSTANCE, original, copy, different );
		runBasicTests( ImageType.INSTANCE, original, copy, different );
		runBasicTests( MaterializedBlobType.INSTANCE, original, copy, different );
	}

	@Test
	@SuppressWarnings( {"BooleanConstructorCall"})
	public void testBooleanType() {
		final Boolean original = Boolean.TRUE;
		final Boolean copy = new Boolean( true );
		final Boolean different = Boolean.FALSE;

		runBasicTests( BooleanType.INSTANCE, original, copy, different );
		runBasicTests( NumericBooleanType.INSTANCE, original, copy, different );
		runBasicTests( YesNoType.INSTANCE, original, copy, different );
		runBasicTests( TrueFalseType.INSTANCE, original, copy, different );
	}

	@Test
	public void testByteType() {
		final Byte original = 0;
		final Byte copy = new Byte( (byte) 0 );
		final Byte different = 9;

		runBasicTests( ByteType.INSTANCE, original, copy, different );
	}

	@Test
	public void testCalendarDateType() {
		final Calendar original = new GregorianCalendar();
		final Calendar copy = new GregorianCalendar();
		final Calendar different = new GregorianCalendar();
		different.set( Calendar.MONTH, 9 );
		different.set( Calendar.DAY_OF_MONTH, 9 );
		different.set( Calendar.YEAR, 2999 );

		runBasicTests( CalendarDateType.INSTANCE, original, copy, different );
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

		runBasicTests( CalendarType.INSTANCE, original, copy, different );
	}

	@Test
	public void testCharacterArrayType() {
		final Character[] original = new Character[] { 'a', 'b' };
		final Character[] copy = new Character[] { 'a', 'b' };
		final Character[] different = new Character[] { 'a', 'b', 'c' };

		runBasicTests( CharacterArrayType.INSTANCE, original, copy, different );
	}

	@Test
	public void testCharacterType() {
		final Character original = 'a';
		final Character copy = new Character( 'a' );
		final Character different = 'b';

		runBasicTests( CharacterType.INSTANCE, original, copy, different );
	}

	@Test
	public void testCharArrayType() {
		final char[] original = new char[] { 'a', 'b' };
		final char[] copy = new char[] { 'a', 'b' };
		final char[] different = new char[] { 'a', 'b', 'c' };

		runBasicTests( CharArrayType.INSTANCE, original, copy, different );
		runBasicTests( CharArrayType.INSTANCE, original, copy, different );
	}

	@Test
	public void testClassType() {
		final Class original = TypeTest.class;
		final Class copy = (Class) SerializationHelper.clone( original );
		final Class different = String.class;

		runBasicTests( ClassType.INSTANCE, original, copy, different );
	}

	@Test
	public void testCurrencyType() {
		final Currency original = Currency.getInstance( Locale.US );
		final Currency copy = Currency.getInstance( Locale.US );
		final Currency different = Currency.getInstance( Locale.UK );

		runBasicTests( CurrencyType.INSTANCE, original, copy, different );
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

		runBasicTests( DateType.INSTANCE, original, copy, different );
	}

	@Test
	public void testDoubleType() {
		final Double original = Double.valueOf( 100 );
		final Double copy = Double.valueOf( 100 );
		final Double different = Double.valueOf( 999 );

		runBasicTests( DoubleType.INSTANCE, original, copy, different );
	}

	@Test
	public void testFloatType() {
		final Float original = Float.valueOf( 100 );
		final Float copy = Float.valueOf( 100 );
		final Float different = Float.valueOf( 999 );

		runBasicTests( FloatType.INSTANCE, original, copy, different );
	}

	@Test
	public void testIntegerType() {
		final Integer original = 100;
		final Integer copy = new Integer( 100 );
		final Integer different = 999;

		runBasicTests( IntegerType.INSTANCE, original, copy, different );
	}

	@Test
	public void testLocaleType() {
		final Locale original = new Locale( "ab" );
		final Locale copy = new Locale( "ab" );
		final Locale different = new Locale( "yz" );

		runBasicTests( LocaleType.INSTANCE, original, copy, different );
	}

	@Test
	public void testLongType() {
		final Long original = 100L;
		final Long copy = new Long( 100L );
		final Long different = 999L;

		runBasicTests( LongType.INSTANCE, original, copy, different );
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

		runBasicTests( SerializableType.INSTANCE, original, copy, different );
		runBasicTests( new SerializableType<SerializableImpl>( SerializableImpl.class ), original, copy, different );
	}

	@Test
	public void testShortType() {
		final Short original = 100;
		final Short copy = new Short( (short) 100 );
		final Short different = 999;

		runBasicTests( ShortType.INSTANCE, original, copy, different );
	}

	@Test
	public void testStringType() {
		final String original = "abc";
		final String copy = new String( original.toCharArray() );
		final String different = "xyz";

		runBasicTests( StringType.INSTANCE, original, copy, different );
		runBasicTests( TextType.INSTANCE, original, copy, different );
		runBasicTests( MaterializedClobType.INSTANCE, original, copy, different );
	}

	@Test
	public void testTimestampType() {
		final long now = System.currentTimeMillis();
		final Timestamp original = new Timestamp( now );
		final Timestamp copy = new Timestamp( now );
		final Timestamp different = new Timestamp( now + 9999 );

		runBasicTests( TimestampType.INSTANCE, original, copy, different );
	}

	@Test
	public void testTimeType() {
		final long now = System.currentTimeMillis();
		final Time original = new Time( now );
		final Time copy = new Time( now );
		final Time different = new Time( now + 9999 );

		runBasicTests( TimeType.INSTANCE, original, copy, different );
	}

	@Test
	public void testDates() {
		final long now = System.currentTimeMillis();
		final java.util.Date original = new java.util.Date( now );
		final java.util.Date copy = new java.util.Date( now );
		final java.util.Date different = new java.util.Date( now + 9999 );
		final java.util.Date different2 = new java.util.Date( now + ( 1000L * 60L * 60L * 24L * 365L ) );

		runBasicTests( TimeType.INSTANCE, original, copy, different );
		runBasicTests( TimestampType.INSTANCE, original, copy, different );
		runBasicTests( DateType.INSTANCE, original, copy, different2 );
	}

	@Test
	public void testTimeZoneType() {
		final TimeZone original = new SimpleTimeZone( -1, "abc" );
		final TimeZone copy = new SimpleTimeZone( -1, "abc" );
		final TimeZone different = new SimpleTimeZone( -2, "xyz" );

		runBasicTests( TimeZoneType.INSTANCE, original, copy, different );
	}

	protected <T> void runBasicTests(AbstractSingleColumnStandardBasicType<T> type, T original, T copy, T different) {
		final boolean nonCopyable = Class.class.isInstance( original ) || Currency.class.isInstance( original );
		if ( ! nonCopyable ) {
			// these checks exclude classes which cannot really be cloned (singetons/enums)
			assertFalse( original == copy );
		}

		assertTrue( original == type.replace( original, copy, null, null, null ) );

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
