/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.embeddable;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

import org.hibernate.Length;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.NumericBooleanConverter;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.YesNoConverter;

import org.hibernate.testing.orm.domain.gambit.EntityOfBasics;
import org.hibernate.testing.orm.domain.gambit.MutableValue;
import org.junit.jupiter.api.Assertions;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@Embeddable
@Access( AccessType.PROPERTY )
public class EmbeddableWithArrayAggregate {

	private Boolean[] theBoolean;
	private Boolean[] theNumericBoolean;
	private Boolean[] theStringBoolean;
	private String[] theString;
	private Integer[] theInteger;
	private int[] theInt;
	private double[] theDouble;
	private URL[] theUrl;
	private String[] theClob;
	private byte[][] theBinary;
	private Date[] theDate;
	private Date[] theTime;
	private Date[] theTimestamp;
	private Instant[] theInstant;
	private UUID[] theUuid;
	private EntityOfBasics.Gender[] gender;
	private EntityOfBasics.Gender[] convertedGender;
	private EntityOfBasics.Gender[] ordinalGender;
	private Duration[] theDuration;

	private LocalDateTime[] theLocalDateTime;
	private LocalDate[] theLocalDate;
	private LocalTime[] theLocalTime;
	private ZonedDateTime[] theZonedDateTime;
	private OffsetDateTime[] theOffsetDateTime;

	private MutableValue[] mutableValue;

	public EmbeddableWithArrayAggregate() {
	}

	@JdbcTypeCode(SqlTypes.ARRAY)
	public String[] getTheString() {
		return theString;
	}

	public void setTheString(String[] theString) {
		this.theString = theString;
	}

	@JdbcTypeCode(SqlTypes.ARRAY)
	public Integer[] getTheInteger() {
		return theInteger;
	}

	public void setTheInteger(Integer[] theInteger) {
		this.theInteger = theInteger;
	}

	@JdbcTypeCode(SqlTypes.ARRAY)
	public int[] getTheInt() {
		return theInt;
	}

	public void setTheInt(int[] theInt) {
		this.theInt = theInt;
	}

	@JdbcTypeCode(SqlTypes.ARRAY)
	public double[] getTheDouble() {
		return theDouble;
	}

	public void setTheDouble(double[] theDouble) {
		this.theDouble = theDouble;
	}

	@JdbcTypeCode(SqlTypes.ARRAY)
	public URL[] getTheUrl() {
		return theUrl;
	}

	public void setTheUrl(URL[] theUrl) {
		this.theUrl = theUrl;
	}

	@Column(length = Length.LONG32)
	@JdbcTypeCode(SqlTypes.ARRAY)
	public String[] getTheClob() {
		return theClob;
	}

	public void setTheClob(String[] theClob) {
		this.theClob = theClob;
	}

	@JdbcTypeCode(SqlTypes.ARRAY)
	public byte[][] getTheBinary() {
		return theBinary;
	}

	public void setTheBinary(byte[][] theBinary) {
		this.theBinary = theBinary;
	}

	@Enumerated( EnumType.STRING )
	@JdbcTypeCode(SqlTypes.ARRAY)
	public EntityOfBasics.Gender[] getGender() {
		return gender;
	}

	public void setGender(EntityOfBasics.Gender[] gender) {
		this.gender = gender;
	}

	@Convert( converter = EntityOfBasics.GenderConverter.class )
	@Column(name = "converted_gender", length = 1)
	@JdbcTypeCode(SqlTypes.ARRAY)
	public EntityOfBasics.Gender[] getConvertedGender() {
		return convertedGender;
	}

	public void setConvertedGender(EntityOfBasics.Gender[] convertedGender) {
		this.convertedGender = convertedGender;
	}

	@Column(name = "ordinal_gender")
	@JdbcTypeCode(SqlTypes.ARRAY)
	public EntityOfBasics.Gender[] getOrdinalGender() {
		return ordinalGender;
	}

	public void setOrdinalGender(EntityOfBasics.Gender[] ordinalGender) {
		this.ordinalGender = ordinalGender;
	}

	@Temporal( TemporalType.DATE )
	@JdbcTypeCode(SqlTypes.ARRAY)
	public Date[] getTheDate() {
		return theDate;
	}

	public void setTheDate(Date[] theDate) {
		this.theDate = theDate;
	}

	@Temporal( TemporalType.TIME )
	@JdbcTypeCode(SqlTypes.ARRAY)
	public Date[] getTheTime() {
		return theTime;
	}

	public void setTheTime(Date[] theTime) {
		this.theTime = theTime;
	}

	@Temporal( TemporalType.TIMESTAMP )
	@JdbcTypeCode(SqlTypes.ARRAY)
	public Date[] getTheTimestamp() {
		return theTimestamp;
	}

	public void setTheTimestamp(Date[] theTimestamp) {
		this.theTimestamp = theTimestamp;
	}

	@Temporal( TemporalType.TIMESTAMP )
	@JdbcTypeCode(SqlTypes.ARRAY)
	public Instant[] getTheInstant() {
		return theInstant;
	}

	public void setTheInstant(Instant[] theInstant) {
		this.theInstant = theInstant;
	}

	@JdbcTypeCode(SqlTypes.ARRAY)
	public UUID[] getTheUuid() {
		return theUuid;
	}

	public void setTheUuid(UUID[] theUuid) {
		this.theUuid = theUuid;
	}

	@JdbcTypeCode(SqlTypes.ARRAY)
	public LocalDateTime[] getTheLocalDateTime() {
		return theLocalDateTime;
	}

	public void setTheLocalDateTime(LocalDateTime[] theLocalDateTime) {
		this.theLocalDateTime = theLocalDateTime;
	}

	@JdbcTypeCode(SqlTypes.ARRAY)
	public LocalDate[] getTheLocalDate() {
		return theLocalDate;
	}

	public void setTheLocalDate(LocalDate[] theLocalDate) {
		this.theLocalDate = theLocalDate;
	}

	@JdbcTypeCode(SqlTypes.ARRAY)
	public LocalTime[] getTheLocalTime() {
		return theLocalTime;
	}

	public void setTheLocalTime(LocalTime[] theLocalTime) {
		this.theLocalTime = theLocalTime;
	}

	@JdbcTypeCode(SqlTypes.ARRAY)
	public OffsetDateTime[] getTheOffsetDateTime() {
		return theOffsetDateTime;
	}

	public void setTheOffsetDateTime(OffsetDateTime[] theOffsetDateTime) {
		this.theOffsetDateTime = theOffsetDateTime;
	}

	@JdbcTypeCode(SqlTypes.ARRAY)
	public ZonedDateTime[] getTheZonedDateTime() {
		return theZonedDateTime;
	}

	public void setTheZonedDateTime(ZonedDateTime[] theZonedDateTime) {
		this.theZonedDateTime = theZonedDateTime;
	}

	@JdbcTypeCode(SqlTypes.ARRAY)
	public Duration[] getTheDuration() {
		return theDuration;
	}

	public void setTheDuration(Duration[] theDuration) {
		this.theDuration = theDuration;
	}

	@JdbcTypeCode(SqlTypes.ARRAY)
	public Boolean[] getTheBoolean() {
		return theBoolean;
	}

	public void setTheBoolean(Boolean[] theBoolean) {
		this.theBoolean = theBoolean;
	}

	@Convert(converter = NumericBooleanConverter.class)
	@JdbcTypeCode(SqlTypes.ARRAY)
	public Boolean[] getTheNumericBoolean() {
		return theNumericBoolean;
	}

	public void setTheNumericBoolean(Boolean[] theNumericBoolean) {
		this.theNumericBoolean = theNumericBoolean;
	}

	@Convert(converter = YesNoConverter.class)
	@JdbcTypeCode(SqlTypes.ARRAY)
	public Boolean[] getTheStringBoolean() {
		return theStringBoolean;
	}

	public void setTheStringBoolean(Boolean[] theStringBoolean) {
		this.theStringBoolean = theStringBoolean;
	}

	@Convert( converter = EntityOfBasics.MutableValueConverter.class )
	@JdbcTypeCode(SqlTypes.ARRAY)
	public MutableValue[] getMutableValue() {
		return mutableValue;
	}

	public void setMutableValue(MutableValue[] mutableValue) {
		this.mutableValue = mutableValue;
	}

	static void assertEquals(EmbeddableWithArrayAggregate a1, EmbeddableWithArrayAggregate a2) {
		Assertions.assertArrayEquals( a1.theInt, a2.theInt );
		Assertions.assertArrayEquals( a1.theDouble, a2.theDouble );
		Assertions.assertArrayEquals( a1.theBoolean, a2.theBoolean );
		Assertions.assertArrayEquals( a1.theNumericBoolean, a2.theNumericBoolean );
		Assertions.assertArrayEquals( a1.theStringBoolean, a2.theStringBoolean );
		Assertions.assertArrayEquals( a1.theString, a2.theString );
		Assertions.assertArrayEquals( a1.theInteger, a2.theInteger );
		Assertions.assertArrayEquals( a1.theUrl, a2.theUrl );
		Assertions.assertArrayEquals( a1.theClob, a2.theClob );
		Assertions.assertArrayEquals( a1.theBinary, a2.theBinary );
		Assertions.assertArrayEquals( a1.theDate, a2.theDate );
		Assertions.assertArrayEquals( a1.theTime, a2.theTime );
		Assertions.assertArrayEquals( a1.theTimestamp, a2.theTimestamp );
		Assertions.assertArrayEquals( a1.theInstant, a2.theInstant );
		Assertions.assertArrayEquals( a1.theUuid, a2.theUuid );
		Assertions.assertArrayEquals( a1.gender, a2.gender );
		Assertions.assertArrayEquals( a1.convertedGender, a2.convertedGender );
		Assertions.assertArrayEquals( a1.ordinalGender, a2.ordinalGender );
		Assertions.assertArrayEquals( a1.theDuration, a2.theDuration );
		Assertions.assertArrayEquals( a1.theLocalDateTime, a2.theLocalDateTime );
		Assertions.assertArrayEquals( a1.theLocalDate, a2.theLocalDate );
		Assertions.assertArrayEquals( a1.theLocalTime, a2.theLocalTime );
		if ( a1.theZonedDateTime == null ) {
			assertNull( a2.theZonedDateTime );
		}
		else {
			assertNotNull( a2.theZonedDateTime );
			Assertions.assertEquals( a1.theZonedDateTime.length, a2.theZonedDateTime.length );
			for ( int i = 0; i < a1.theZonedDateTime.length; i++ ) {
				if ( a1.theZonedDateTime[i] == null ) {
					assertNull( a2.theZonedDateTime[i] );
				}
				else {
					assertNotNull( a2.theZonedDateTime[i] );
					Assertions.assertEquals( a1.theZonedDateTime[i].toInstant(), a2.theZonedDateTime[i].toInstant() );
				}
			}
		}
		if ( a1.theOffsetDateTime == null ) {
			assertNull( a2.theOffsetDateTime );
		}
		else {
			assertNotNull( a2.theOffsetDateTime );
			Assertions.assertEquals( a1.theOffsetDateTime.length, a2.theOffsetDateTime.length );
			for ( int i = 0; i < a1.theOffsetDateTime.length; i++ ) {
				if ( a1.theOffsetDateTime[i] == null ) {
					assertNull( a2.theOffsetDateTime[i] );
				}
				else {
					assertNotNull( a2.theOffsetDateTime[i] );
					Assertions.assertEquals( a1.theOffsetDateTime[i].toInstant(), a2.theOffsetDateTime[i].toInstant() );
				}
			}
		}
		if ( a1.mutableValue == null ) {
			assertNull( a2.mutableValue );
		}
		else {
			assertNotNull( a2.mutableValue );
			Assertions.assertEquals( a1.mutableValue.length, a2.mutableValue.length );
			for ( int i = 0; i < a1.mutableValue.length; i++ ) {
				if ( a1.mutableValue[i] == null ) {
					assertNull( a2.mutableValue[i] );
				}
				else {
					assertNotNull( a2.mutableValue[i] );
					Assertions.assertEquals( a1.mutableValue[i].getState(), a2.mutableValue[i].getState() );
				}
			}
		}
	}

	public static EmbeddableWithArrayAggregate createAggregate1() {
		final EmbeddableWithArrayAggregate aggregate = new EmbeddableWithArrayAggregate();
		aggregate.theBoolean = new Boolean[]{ true, false, true };
		aggregate.theNumericBoolean = new Boolean[]{ true, false, true };
		aggregate.theStringBoolean = new Boolean[]{ true, false, true };
		aggregate.theString = new String[]{ "String \"<abc>A&B</abc>\"", "{\"text\":\"<abc>A&B</abc>\"}" };
		aggregate.theInteger = new Integer[]{ -1,  };
		aggregate.theInt = new int[]{ Integer.MAX_VALUE };
		aggregate.theDouble = new double[]{ 1.3e20 };
		try {
			aggregate.theUrl = new URL[]{ new URL( "https://hibernate.org" ) };
		}
		catch (MalformedURLException e) {
			throw new RuntimeException( e );
		}
		aggregate.theClob = new String[]{ "Abc" };
		aggregate.theBinary = new byte[][] { new byte[]{ 1 } };
		aggregate.theDate = new java.sql.Date[]{ new java.sql.Date( 2000 - 1900, 0, 1 ) };
		aggregate.theTime = new Time[]{ new Time( 1, 0, 0 ) };
		aggregate.theTimestamp = new Timestamp[]{ new Timestamp( 2000 - 1900, 0, 1, 1, 0, 0, 1000 ) };
		aggregate.theInstant = new Instant[]{LocalDateTime.of( 2000, 1, 1, 0, 0, 0 ).toInstant( ZoneOffset.UTC ) };
		aggregate.theUuid = new UUID[]{ UUID.fromString( "53886a8a-7082-4879-b430-25cb94415be8" ) };
		aggregate.gender = new EntityOfBasics.Gender[]{ EntityOfBasics.Gender.FEMALE };
		aggregate.convertedGender = new EntityOfBasics.Gender[]{ EntityOfBasics.Gender.MALE };
		aggregate.ordinalGender = new EntityOfBasics.Gender[]{ EntityOfBasics.Gender.OTHER };
		aggregate.theDuration = new Duration[]{ Duration.ofHours( 1 ) };
		aggregate.theLocalDateTime = new LocalDateTime[]{ LocalDateTime.of( 2000, 1, 1, 0, 0, 0 ) };
		aggregate.theLocalDate = new LocalDate[]{ LocalDate.of( 2000, 1, 1 ) };
		aggregate.theLocalTime = new LocalTime[]{ LocalTime.of( 1, 0, 0 ) };
		aggregate.theZonedDateTime = new ZonedDateTime[]{ LocalDateTime.of( 2000, 1, 1, 0, 0, 0 ).atZone( ZoneOffset.UTC ) };
		aggregate.theOffsetDateTime = new OffsetDateTime[]{ LocalDateTime.of( 2000, 1, 1, 0, 0, 0 ).atOffset( ZoneOffset.UTC ) };
		aggregate.mutableValue = new MutableValue[]{ new MutableValue( "some state" ) };
		return aggregate;
	}

	public static EmbeddableWithArrayAggregate createAggregate2() {
		final EmbeddableWithArrayAggregate aggregate = new EmbeddableWithArrayAggregate();
		aggregate.theString = new String[]{ "String 'abc'" };
		return aggregate;
	}

	public static EmbeddableWithArrayAggregate createAggregate3() {
		final EmbeddableWithArrayAggregate aggregate = new EmbeddableWithArrayAggregate();
		aggregate.theString = new String[]{ "ABC" };
		aggregate.theBinary = new byte[][] { new byte[]{ 1 } };
		aggregate.theUuid = new UUID[]{ UUID.fromString( "53886a8a-7082-4879-b430-25cb94415be8" ) };
		aggregate.theLocalDateTime = new LocalDateTime[]{ LocalDateTime.of( 2022, 12, 1, 1, 0, 0 ) };
		return aggregate;
	}

	@Override
	public int hashCode() {
		return 1;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		EmbeddableWithArrayAggregate that = (EmbeddableWithArrayAggregate) o;

		if ( !Arrays.equals( theBoolean, that.theBoolean ) ) {
			return false;
		}
		if ( !Arrays.equals( theNumericBoolean, that.theNumericBoolean ) ) {
			return false;
		}
		if ( !Arrays.equals( theStringBoolean, that.theStringBoolean ) ) {
			return false;
		}
		if ( !Arrays.equals( theString, that.theString ) ) {
			return false;
		}
		if ( !Arrays.equals( theInteger, that.theInteger ) ) {
			return false;
		}
		if ( !Arrays.equals( theInt, that.theInt ) ) {
			return false;
		}
		if ( !Arrays.equals( theDouble, that.theDouble ) ) {
			return false;
		}
		if ( !Arrays.equals( theUrl, that.theUrl ) ) {
			return false;
		}
		if ( !Arrays.equals( theClob, that.theClob ) ) {
			return false;
		}
		if ( !Arrays.deepEquals( theBinary, that.theBinary ) ) {
			return false;
		}
		if ( !Arrays.equals( theDate, that.theDate ) ) {
			return false;
		}
		if ( !Arrays.equals( theTime, that.theTime ) ) {
			return false;
		}
		if ( !Arrays.equals( theTimestamp, that.theTimestamp ) ) {
			return false;
		}
		if ( !Arrays.equals( theInstant, that.theInstant ) ) {
			return false;
		}
		if ( !Arrays.equals( theUuid, that.theUuid ) ) {
			return false;
		}
		if ( !Arrays.equals( gender, that.gender ) ) {
			return false;
		}
		if ( !Arrays.equals( convertedGender, that.convertedGender ) ) {
			return false;
		}
		if ( !Arrays.equals( ordinalGender, that.ordinalGender ) ) {
			return false;
		}
		if ( !Arrays.equals( theDuration, that.theDuration ) ) {
			return false;
		}
		if ( !Arrays.equals( theLocalDateTime, that.theLocalDateTime ) ) {
			return false;
		}
		if ( !Arrays.equals( theLocalDate, that.theLocalDate ) ) {
			return false;
		}
		if ( !Arrays.equals( theLocalTime, that.theLocalTime ) ) {
			return false;
		}
		if ( !Arrays.equals( theZonedDateTime, that.theZonedDateTime ) ) {
			return false;
		}
		if ( !Arrays.equals( theOffsetDateTime, that.theOffsetDateTime ) ) {
			return false;
		}
		return Arrays.equals( mutableValue, that.mutableValue );
	}
}
