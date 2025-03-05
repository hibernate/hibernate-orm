/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.embeddable;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
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
import java.util.Objects;
import java.util.UUID;

import org.hibernate.Length;
import org.hibernate.annotations.JdbcTypeCode;

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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@Embeddable
@Access( AccessType.PROPERTY )
public class EmbeddableAggregate {

	private Boolean theBoolean = false;
	private Boolean theNumericBoolean = false;
	private Boolean theStringBoolean = false;
	private String theString;
	private Integer theInteger;
	private int theInt;
	private double theDouble;
	private URL theUrl;
	private String theClob;
	private byte[] theBinary;
	private Date theDate;
	private Date theTime;
	private Date theTimestamp;
	private Instant theInstant;
	private UUID theUuid;
	private EntityOfBasics.Gender gender;
	private EntityOfBasics.Gender convertedGender;
	private EntityOfBasics.Gender ordinalGender;
	private Duration theDuration;

	private LocalDateTime theLocalDateTime;
	private LocalDate theLocalDate;
	private LocalTime theLocalTime;
	private ZonedDateTime theZonedDateTime;
	private OffsetDateTime theOffsetDateTime;

	private MutableValue mutableValue;

	public EmbeddableAggregate() {
	}

	public String getTheString() {
		return theString;
	}

	public void setTheString(String theString) {
		this.theString = theString;
	}

	public Integer getTheInteger() {
		return theInteger;
	}

	public void setTheInteger(Integer theInteger) {
		this.theInteger = theInteger;
	}

	public int getTheInt() {
		return theInt;
	}

	public void setTheInt(int theInt) {
		this.theInt = theInt;
	}

	public double getTheDouble() {
		return theDouble;
	}

	public void setTheDouble(double theDouble) {
		this.theDouble = theDouble;
	}

	public URL getTheUrl() {
		return theUrl;
	}

	public void setTheUrl(URL theUrl) {
		this.theUrl = theUrl;
	}

	@Column(length = Length.LONG32)
	public String getTheClob() {
		return theClob;
	}

	public void setTheClob(String theClob) {
		this.theClob = theClob;
	}

	public byte[] getTheBinary() {
		return theBinary;
	}

	public void setTheBinary(byte[] theBinary) {
		this.theBinary = theBinary;
	}

	@Enumerated( EnumType.STRING )
	public EntityOfBasics.Gender getGender() {
		return gender;
	}

	public void setGender(EntityOfBasics.Gender gender) {
		this.gender = gender;
	}

	@Convert( converter = EntityOfBasics.GenderConverter.class )
	@Column(name = "converted_gender", length = 1)
	@JdbcTypeCode( Types.CHAR )
	public EntityOfBasics.Gender getConvertedGender() {
		return convertedGender;
	}

	public void setConvertedGender(EntityOfBasics.Gender convertedGender) {
		this.convertedGender = convertedGender;
	}

	@Column(name = "ordinal_gender")
	public EntityOfBasics.Gender getOrdinalGender() {
		return ordinalGender;
	}

	public void setOrdinalGender(EntityOfBasics.Gender ordinalGender) {
		this.ordinalGender = ordinalGender;
	}

	@Temporal( TemporalType.DATE )
	public Date getTheDate() {
		return theDate;
	}

	public void setTheDate(Date theDate) {
		this.theDate = theDate;
	}

	@Temporal( TemporalType.TIME )
	public Date getTheTime() {
		return theTime;
	}

	public void setTheTime(Date theTime) {
		this.theTime = theTime;
	}

	@Temporal( TemporalType.TIMESTAMP )
	public Date getTheTimestamp() {
		return theTimestamp;
	}

	public void setTheTimestamp(Date theTimestamp) {
		this.theTimestamp = theTimestamp;
	}

	@Temporal( TemporalType.TIMESTAMP )
	public Instant getTheInstant() {
		return theInstant;
	}

	public void setTheInstant(Instant theInstant) {
		this.theInstant = theInstant;
	}

	public UUID getTheUuid() {
		return theUuid;
	}

	public void setTheUuid(UUID theUuid) {
		this.theUuid = theUuid;
	}

	public LocalDateTime getTheLocalDateTime() {
		return theLocalDateTime;
	}

	public void setTheLocalDateTime(LocalDateTime theLocalDateTime) {
		this.theLocalDateTime = theLocalDateTime;
	}

	public LocalDate getTheLocalDate() {
		return theLocalDate;
	}

	public void setTheLocalDate(LocalDate theLocalDate) {
		this.theLocalDate = theLocalDate;
	}

	public LocalTime getTheLocalTime() {
		return theLocalTime;
	}

	public void setTheLocalTime(LocalTime theLocalTime) {
		this.theLocalTime = theLocalTime;
	}

	public OffsetDateTime getTheOffsetDateTime() {
		return theOffsetDateTime;
	}

	public void setTheOffsetDateTime(OffsetDateTime theOffsetDateTime) {
		this.theOffsetDateTime = theOffsetDateTime;
	}

	public ZonedDateTime getTheZonedDateTime() {
		return theZonedDateTime;
	}

	public void setTheZonedDateTime(ZonedDateTime theZonedDateTime) {
		this.theZonedDateTime = theZonedDateTime;
	}

	public Duration getTheDuration() {
		return theDuration;
	}

	public void setTheDuration(Duration theDuration) {
		this.theDuration = theDuration;
	}

	public Boolean isTheBoolean() {
		return theBoolean;
	}

	public void setTheBoolean(Boolean theBoolean) {
		this.theBoolean = theBoolean;
	}

	@JdbcTypeCode( Types.INTEGER )
	public Boolean isTheNumericBoolean() {
		return theNumericBoolean;
	}

	public void setTheNumericBoolean(Boolean theNumericBoolean) {
		this.theNumericBoolean = theNumericBoolean;
	}

	@JdbcTypeCode( Types.CHAR )
	public Boolean isTheStringBoolean() {
		return theStringBoolean;
	}

	public void setTheStringBoolean(Boolean theStringBoolean) {
		this.theStringBoolean = theStringBoolean;
	}

	@Convert( converter = EntityOfBasics.MutableValueConverter.class )
	public MutableValue getMutableValue() {
		return mutableValue;
	}

	public void setMutableValue(MutableValue mutableValue) {
		this.mutableValue = mutableValue;
	}

	static void assertEquals(EmbeddableAggregate a1, EmbeddableAggregate a2) {
		Assertions.assertEquals( a1.theInt, a2.theInt );
		Assertions.assertEquals( a1.theDouble, a2.theDouble );
		Assertions.assertEquals( a1.theBoolean, a2.theBoolean );
		Assertions.assertEquals( a1.theNumericBoolean, a2.theNumericBoolean );
		Assertions.assertEquals( a1.theStringBoolean, a2.theStringBoolean );
		Assertions.assertEquals( a1.theString, a2.theString );
		Assertions.assertEquals( a1.theInteger, a2.theInteger );
		Assertions.assertEquals( a1.theUrl, a2.theUrl );
		Assertions.assertEquals( a1.theClob, a2.theClob );
		assertArrayEquals( a1.theBinary, a2.theBinary );
		Assertions.assertEquals( a1.theDate, a2.theDate );
		Assertions.assertEquals( a1.theTime, a2.theTime );
		Assertions.assertEquals( a1.theTimestamp, a2.theTimestamp );
		Assertions.assertEquals( a1.theInstant, a2.theInstant );
		Assertions.assertEquals( a1.theUuid, a2.theUuid );
		Assertions.assertEquals( a1.gender, a2.gender );
		Assertions.assertEquals( a1.convertedGender, a2.convertedGender );
		Assertions.assertEquals( a1.ordinalGender, a2.ordinalGender );
		Assertions.assertEquals( a1.theDuration, a2.theDuration );
		Assertions.assertEquals( a1.theLocalDateTime, a2.theLocalDateTime );
		Assertions.assertEquals( a1.theLocalDate, a2.theLocalDate );
		Assertions.assertEquals( a1.theLocalTime, a2.theLocalTime );
		if ( a1.theZonedDateTime == null ) {
			assertNull( a2.theZonedDateTime );
		}
		else {
			assertNotNull( a2.theZonedDateTime );
			Assertions.assertEquals( a1.theZonedDateTime.toInstant(), a2.theZonedDateTime.toInstant() );
		}
		if ( a1.theOffsetDateTime == null ) {
			assertNull( a2.theOffsetDateTime );
		}
		else {
			assertNotNull( a2.theOffsetDateTime );
			Assertions.assertEquals( a1.theOffsetDateTime.toInstant(), a2.theOffsetDateTime.toInstant() );
		}
		if ( a1.mutableValue == null ) {
			assertNull( a2.mutableValue );
		}
		else {
			assertNotNull( a2.mutableValue );
			Assertions.assertEquals( a1.mutableValue.getState(), a2.mutableValue.getState() );
		}
	}

	public static EmbeddableAggregate createAggregate1() {
		final EmbeddableAggregate aggregate = new EmbeddableAggregate();
		aggregate.theBoolean = true;
		aggregate.theNumericBoolean = true;
		aggregate.theStringBoolean = true;
		aggregate.theString = "String \"<abc>A&B</abc>\"";
		aggregate.theInteger = -1;
		aggregate.theInt = Integer.MAX_VALUE;
		aggregate.theDouble = 1.3e20;
		try {
			aggregate.theUrl = new URL( "https://hibernate.org" );
		}
		catch (MalformedURLException e) {
			throw new RuntimeException( e );
		}
		aggregate.theClob = "Abc";
		aggregate.theBinary = new byte[] { 1 };
		aggregate.theDate = new java.sql.Date( 2000 - 1900, 0, 1 );
		aggregate.theTime = new Time( 1, 0, 0 );
		aggregate.theTimestamp = new Timestamp( 2000 - 1900, 0, 1, 1, 0, 0, 3000000 ); // Use 3 millis to allow representation on Sybase
		aggregate.theInstant = LocalDateTime.of( 2000, 1, 1, 0, 0, 0 ).toInstant( ZoneOffset.UTC );
		aggregate.theUuid = UUID.fromString( "53886a8a-7082-4879-b430-25cb94415be8" );
		aggregate.gender = EntityOfBasics.Gender.FEMALE;
		aggregate.convertedGender = EntityOfBasics.Gender.MALE;
		aggregate.ordinalGender = EntityOfBasics.Gender.OTHER;
		aggregate.theDuration = Duration.ofHours( 1 );
		aggregate.theLocalDateTime = LocalDateTime.of( 2000, 1, 1, 0, 0, 0 );
		aggregate.theLocalDate = LocalDate.of( 2000, 1, 1 );
		aggregate.theLocalTime = LocalTime.of( 1, 0, 0 );
		aggregate.theZonedDateTime = LocalDateTime.of( 2000, 1, 1, 0, 0, 0 ).atZone( ZoneOffset.UTC );
		aggregate.theOffsetDateTime = LocalDateTime.of( 2000, 1, 1, 0, 0, 0 ).atOffset( ZoneOffset.UTC );
		aggregate.mutableValue = new MutableValue( "some state" );
		return aggregate;
	}

	public static EmbeddableAggregate createAggregate2() {
		final EmbeddableAggregate aggregate = new EmbeddableAggregate();
		aggregate.theString = "String 'abc'";
		return aggregate;
	}

	public static EmbeddableAggregate createAggregate3() {
		final EmbeddableAggregate aggregate = new EmbeddableAggregate();
		aggregate.theString = "ABC";
		aggregate.theBinary = new byte[] { 1 };
		aggregate.theUuid = UUID.fromString( "53886a8a-7082-4879-b430-25cb94415be8" );
		aggregate.theLocalDateTime = LocalDateTime.of( 2022, 12, 1, 1, 0, 0 );
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

		EmbeddableAggregate that = (EmbeddableAggregate) o;

		if ( theInt != that.theInt ) {
			return false;
		}
		if ( Double.compare( that.theDouble, theDouble ) != 0 ) {
			return false;
		}
		if ( !Objects.equals( theBoolean, that.theBoolean ) ) {
			return false;
		}
		if ( !Objects.equals( theNumericBoolean, that.theNumericBoolean ) ) {
			return false;
		}
		if ( !Objects.equals( theStringBoolean, that.theStringBoolean ) ) {
			return false;
		}
		if ( !Objects.equals( theString, that.theString ) ) {
			return false;
		}
		if ( !Objects.equals( theInteger, that.theInteger ) ) {
			return false;
		}
		if ( !Objects.equals( theUrl, that.theUrl ) ) {
			return false;
		}
		if ( !Objects.equals( theClob, that.theClob ) ) {
			return false;
		}
		if ( !Arrays.equals( theBinary, that.theBinary ) ) {
			return false;
		}
		if ( !Objects.equals( theDate, that.theDate ) ) {
			return false;
		}
		if ( !Objects.equals( theTime, that.theTime ) ) {
			return false;
		}
		if ( !Objects.equals( theTimestamp, that.theTimestamp ) ) {
			return false;
		}
		if ( !Objects.equals( theInstant, that.theInstant ) ) {
			return false;
		}
		if ( !Objects.equals( theUuid, that.theUuid ) ) {
			return false;
		}
		if ( gender != that.gender ) {
			return false;
		}
		if ( convertedGender != that.convertedGender ) {
			return false;
		}
		if ( ordinalGender != that.ordinalGender ) {
			return false;
		}
		if ( !Objects.equals( theDuration, that.theDuration ) ) {
			return false;
		}
		if ( !Objects.equals( theLocalDateTime, that.theLocalDateTime ) ) {
			return false;
		}
		if ( !Objects.equals( theLocalDate, that.theLocalDate ) ) {
			return false;
		}
		if ( !Objects.equals( theLocalTime, that.theLocalTime ) ) {
			return false;
		}
		if ( !Objects.equals( theZonedDateTime, that.theZonedDateTime ) ) {
			return false;
		}
		if ( !Objects.equals( theOffsetDateTime, that.theOffsetDateTime ) ) {
			return false;
		}
		return Objects.equals( mutableValue, that.mutableValue );
	}
}
