/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.xml.internal;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Year;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Currency;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

import org.hibernate.internal.util.StringHelper;
import org.hibernate.type.descriptor.java.BasicJavaType;
import org.hibernate.type.descriptor.java.BigDecimalJavaType;
import org.hibernate.type.descriptor.java.BigIntegerJavaType;
import org.hibernate.type.descriptor.java.BlobJavaType;
import org.hibernate.type.descriptor.java.BooleanJavaType;
import org.hibernate.type.descriptor.java.ByteJavaType;
import org.hibernate.type.descriptor.java.CalendarJavaType;
import org.hibernate.type.descriptor.java.CharacterJavaType;
import org.hibernate.type.descriptor.java.ClassJavaType;
import org.hibernate.type.descriptor.java.ClobJavaType;
import org.hibernate.type.descriptor.java.CurrencyJavaType;
import org.hibernate.type.descriptor.java.DoubleJavaType;
import org.hibernate.type.descriptor.java.DurationJavaType;
import org.hibernate.type.descriptor.java.FloatJavaType;
import org.hibernate.type.descriptor.java.InetAddressJavaType;
import org.hibernate.type.descriptor.java.InstantJavaType;
import org.hibernate.type.descriptor.java.IntegerJavaType;
import org.hibernate.type.descriptor.java.JdbcDateJavaType;
import org.hibernate.type.descriptor.java.JdbcTimeJavaType;
import org.hibernate.type.descriptor.java.JdbcTimestampJavaType;
import org.hibernate.type.descriptor.java.LocalDateJavaType;
import org.hibernate.type.descriptor.java.LocalDateTimeJavaType;
import org.hibernate.type.descriptor.java.LocalTimeJavaType;
import org.hibernate.type.descriptor.java.LocaleJavaType;
import org.hibernate.type.descriptor.java.LongJavaType;
import org.hibernate.type.descriptor.java.NClobJavaType;
import org.hibernate.type.descriptor.java.OffsetDateTimeJavaType;
import org.hibernate.type.descriptor.java.OffsetTimeJavaType;
import org.hibernate.type.descriptor.java.ShortJavaType;
import org.hibernate.type.descriptor.java.StringJavaType;
import org.hibernate.type.descriptor.java.TimeZoneJavaType;
import org.hibernate.type.descriptor.java.UUIDJavaType;
import org.hibernate.type.descriptor.java.UrlJavaType;
import org.hibernate.type.descriptor.java.YearJavaType;
import org.hibernate.type.descriptor.java.ZoneIdJavaType;
import org.hibernate.type.descriptor.java.ZoneOffsetJavaType;
import org.hibernate.type.descriptor.java.ZonedDateTimeJavaType;

/**
 * @author Steve Ebersole
 */
public enum SimpleTypeInterpretation {
	BOOLEAN( Boolean.class, BooleanJavaType.class ),
	BYTE( Byte.class, ByteJavaType.class ),
	SHORT( Short.class, ShortJavaType.class ),
	INTEGER( Integer.class, IntegerJavaType.class ),
	LONG( Long.class, LongJavaType.class ),
	DOUBLE( Double.class, DoubleJavaType.class ),
	FLOAT( Float.class, FloatJavaType.class ),
	BIG_INTEGER( BigInteger.class, BigIntegerJavaType.class ),
	BIG_DECIMAL( BigDecimal.class, BigDecimalJavaType.class ),	
	CHARACTER( Character.class, CharacterJavaType.class ),
	STRING( String.class, StringJavaType.class ),
	INSTANT( Instant.class, InstantJavaType.class ),
	DURATION( Duration.class, DurationJavaType.class ),
	YEAR( Year.class, YearJavaType.class ),
	LOCAL_DATE_TIME( LocalDateTime.class, LocalDateTimeJavaType.class ),
	LOCAL_DATE( LocalDate.class, LocalDateJavaType.class ),
	LOCAL_TIME( LocalTime.class, LocalTimeJavaType.class ),
	OFFSET_DATE_TIME( OffsetDateTime.class, OffsetDateTimeJavaType.class ),
	OFFSET_TIME( OffsetTime.class, OffsetTimeJavaType.class ),
	ZONED_DATE_TIME( ZonedDateTime.class, ZonedDateTimeJavaType.class ),
	ZONE_ID( ZoneId.class, ZoneIdJavaType.class ),
	ZONE_OFFSET( ZoneOffset.class, ZoneOffsetJavaType.class ),	
	UUID( UUID .class, UUIDJavaType.class ),
	URL( java.net.URL.class, UrlJavaType.class ),
	INET_ADDRESS( InetAddress.class, InetAddressJavaType.class ),
	CURRENCY( Currency.class, CurrencyJavaType.class ),
	LOCALE( Locale.class, LocaleJavaType.class ),
	CLASS( Class.class, ClassJavaType.class ),
	BLOB( Blob.class, BlobJavaType.class ),
	CLOB( Clob.class, ClobJavaType.class ),
	NCLOB( NClob.class, NClobJavaType.class ),
	JDBC_TIMESTAMP( Timestamp.class, JdbcTimestampJavaType.class ),
	JDBC_DATE( Date.class, JdbcDateJavaType.class ),
	JDBC_TIME( Time.class, JdbcTimeJavaType.class ),
	CALENDAR( Calendar.class, CalendarJavaType.class ),
	TIME_ZONE( TimeZone.class, TimeZoneJavaType.class )	
	;

	SimpleTypeInterpretation(Class<?> javaType, Class<? extends BasicJavaType<?>> javaTypeDescriptorType) {
		this.javaType = javaType;
		this.javaTypeDescriptorType = javaTypeDescriptorType;
	}

	private final Class<?> javaType;
	private final Class<? extends BasicJavaType<?>> javaTypeDescriptorType;

	public Class<?> getJavaType() {
		return javaType;
	}

	public Class<? extends BasicJavaType<?>> getJavaTypeDescriptorType() {
		return javaTypeDescriptorType;
	}
	
	public static SimpleTypeInterpretation interpret(String name) {
		assert StringHelper.isNotEmpty( name );


		if ( name.equalsIgnoreCase( "boolean" )
				|| Boolean.class.getName().equals( name ) ) {
			return BOOLEAN;
		}

		if ( name.equalsIgnoreCase( "byte" )
				|| Byte.class.getName().equals( name ) ) {
			return BYTE;
		}

		if ( name.equalsIgnoreCase( "short" )
				|| Short.class.getName().equals( name ) ) {
			return SHORT;
		}

		if ( name.equalsIgnoreCase( "int" )
				|| name.equalsIgnoreCase( "integer" )
				|| Integer.class.getName().equals( name ) ) {
			return INTEGER;
		}

		if ( name.equalsIgnoreCase( "long" )
				|| Long.class.getName().equals( name ) ) {
			return LONG;
		}

		if ( name.equalsIgnoreCase( "double" )
				|| Double.class.getName().equals( name ) ) {
			return DOUBLE;
		}

		if ( name.equalsIgnoreCase( "float" )
				|| Float.class.getName().equals( name ) ) {
			return FLOAT;
		}

		if ( name.equalsIgnoreCase( "biginteger" )
				|| name.equalsIgnoreCase( "big_integer" )
				|| BigInteger.class.getName().equals( name ) ) {
			return BIG_INTEGER;
		}

		if ( name.equalsIgnoreCase( "bigdecimal" )
				|| name.equalsIgnoreCase( "big_decimal" )
				|| BigDecimal.class.getName().equals( name ) ) {
			return BIG_DECIMAL;
		}

		if ( name.equalsIgnoreCase( "char" )
				|| name.equalsIgnoreCase( "character" )
				|| Character.class.getName().equalsIgnoreCase( name ) ) {
			return CHARACTER;
		}
		
		if ( name.equalsIgnoreCase( "string" )
				|| String.class.getName().equalsIgnoreCase( name ) ) {
			return STRING;
		}

		if ( name.equalsIgnoreCase( "instant" )
				|| Instant.class.getName().equals( name ) ) {
			return INSTANT;
		}

		if ( name.equalsIgnoreCase( "duration" )
				|| Duration.class.getName().equals( name ) ) {
			return DURATION;
		}

		if ( name.equalsIgnoreCase( "year" )
				|| Year.class.getName().equals( name ) ) {
			return YEAR;
		}

		if ( name.equalsIgnoreCase( "localdatetime" )
				|| name.equalsIgnoreCase( "local_date_time" )
				|| LocalDateTime.class.getName().equals( name ) ) {
			return LOCAL_DATE_TIME;
		}

		if ( name.equalsIgnoreCase( "localdate" )
				|| name.equalsIgnoreCase( "local_date" )
				|| LocalDate.class.getName().equals( name ) ) {
			return LOCAL_DATE;
		}

		if ( name.equalsIgnoreCase( "localtime" )
				|| name.equalsIgnoreCase( "local_time" )
				|| LocalTime.class.getName().equals( name ) ) {
			return LOCAL_TIME;
		}

		if ( name.equalsIgnoreCase( "zoneddatetime" )
				|| name.equalsIgnoreCase( "zoned_date_time" )
				|| ZonedDateTime.class.getName().equals( name ) ) {
			return ZONED_DATE_TIME;
		}

		if ( name.equalsIgnoreCase( "offsetdatetime" )
				|| name.equalsIgnoreCase( "offset_date_time" )
				|| OffsetDateTime.class.getName().equals( name ) ) {
			return OFFSET_DATE_TIME;
		}

		if ( name.equalsIgnoreCase( "offsettime" )
				|| name.equalsIgnoreCase( "offset_time" )
				|| OffsetTime.class.getName().equals( name ) ) {
			return OFFSET_TIME;
		}

		if ( name.equalsIgnoreCase( "zoneid" )
				|| name.equalsIgnoreCase( "zone_id" )
				|| ZoneId.class.getName().equals( name ) ) {
			return ZONE_ID;
		}

		if ( name.equalsIgnoreCase( "zoneoffset" )
				|| name.equalsIgnoreCase( "zone_offset" )
				|| ZoneOffset.class.getName().equals( name ) ) {
			return ZONE_OFFSET;
		}

		if ( name.equalsIgnoreCase( "uuid" )
				|| UUID.class.getName().equals( name ) ) {
			return UUID;
		}

		if ( name.equalsIgnoreCase( "url" )
				|| java.net.URL.class.getName().equals( name ) ) {
			return URL;
		}

		if ( name.equalsIgnoreCase( "inet" )
				|| name.equalsIgnoreCase( "inetaddress" )
				|| name.equalsIgnoreCase( "inet_address" )
				|| InetAddress.class.getName().equals( name ) ) {
			return INET_ADDRESS;
		}

		if ( name.equalsIgnoreCase( "currency" )
				|| Currency.class.getName().equals( name ) ) {
			return CURRENCY;
		}

		if ( name.equalsIgnoreCase( "locale" )
				|| Locale.class.getName().equals( name ) ) {
			return LOCALE;
		}

		if ( name.equalsIgnoreCase( "class" )
				|| Class.class.getName().equals( name ) ) {
			return CLASS;
		}

		if ( name.equalsIgnoreCase( "blob" )
				|| Blob.class.getName().equals( name ) ) {
			return BLOB;
		}

		if ( name.equalsIgnoreCase( "clob" )
				|| Clob.class.getName().equals( name ) ) {
			return CLOB;
		}

		if ( name.equalsIgnoreCase( "nclob" )
				|| NClob.class.getName().equals( name ) ) {
			return NCLOB;
		}

		if ( name.equalsIgnoreCase( "timestamp" )
				|| name.equalsIgnoreCase( "time_stamp" )
				|| java.util.Date.class.getName().equals( name )
				|| Timestamp.class.getName().equals( name ) ) {
			return JDBC_TIMESTAMP;
		}

		if ( name.equalsIgnoreCase( "date" )
				|| java.sql.Date.class.getName().equals( name ) ) {
			return JDBC_DATE;
		}

		if ( name.equalsIgnoreCase( "time" )
				|| java.sql.Time.class.getName().equals( name ) ) {
			return JDBC_TIME;
		}

		if ( name.equalsIgnoreCase( "calendar" )
				|| name.equalsIgnoreCase( "gregoriancalendar" )
				|| name.equalsIgnoreCase( "gregorian_calendar" )
				|| Calendar.class.getName().equals( name )
				|| GregorianCalendar.class.getName().equals( name ) ) {
			return CALENDAR;
		}

		if ( name.equalsIgnoreCase( "timezone" )
				|| name.equalsIgnoreCase( "time_zone" )
				|| TimeZone.class.getName().equals( name ) ) {
			return TIME_ZONE;
		}

		return null;
	}
}
