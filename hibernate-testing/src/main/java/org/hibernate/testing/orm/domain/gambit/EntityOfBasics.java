/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.domain.gambit;

import java.net.URL;
import java.sql.Clob;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.Date;
import javax.persistence.AttributeConverter;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EntityResult;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.Type;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings( "unused" )
@SqlResultSetMapping(
		name = "entity-of-basics-implicit",
		entities = @EntityResult( entityClass = EntityOfBasics.class )
)
@Entity
public class EntityOfBasics {

	public enum Gender {
		MALE,
		FEMALE,
		OTHER
	}

	private Integer id;
	private Boolean theBoolean = false;
	private Boolean theNumericBoolean = false;
	private Boolean theStringBoolean = false;
	private String theString;
	private Integer theInteger;
	private int theInt;
	private double theDouble;
	private URL theUrl;
	private Clob theClob;
	private Date theDate;
	private Date theTime;
	private Date theTimestamp;
	private Instant theInstant;
	private Gender gender;
	private Gender convertedGender;
	private Gender ordinalGender;
	private Duration theDuration;

	private LocalDateTime theLocalDateTime;
	private LocalDate theLocalDate;
	private LocalTime theLocalTime;
	private ZonedDateTime theZonedDateTime;
	private OffsetDateTime theOffsetDateTime;

	private MutableValue mutableValue;

	public EntityOfBasics() {
	}

	public EntityOfBasics(Integer id) {
		this.id = id;
	}

	@Id
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
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

	public Clob getTheClob() {
		return theClob;
	}

	public void setTheClob(Clob theClob) {
		this.theClob = theClob;
	}

	@Enumerated( EnumType.STRING )
	public Gender getGender() {
		return gender;
	}

	public void setGender(Gender gender) {
		this.gender = gender;
	}

	@Convert( converter = GenderConverter.class )
	@Column(name = "converted_gender", length = 1)
	public Gender getConvertedGender() {
		return convertedGender;
	}

	public void setConvertedGender(Gender convertedGender) {
		this.convertedGender = convertedGender;
	}

	@Column(name = "ordinal_gender")
	public Gender getOrdinalGender() {
		return ordinalGender;
	}

	public void setOrdinalGender(Gender ordinalGender) {
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

	@Type( type = "numeric_boolean" )
	public Boolean isTheNumericBoolean() {
		return theNumericBoolean;
	}

	public void setTheNumericBoolean(Boolean theNumericBoolean) {
		this.theNumericBoolean = theNumericBoolean;
	}

	@Type( type = "true_false" )
	public Boolean isTheStringBoolean() {
		return theStringBoolean;
	}

	public void setTheStringBoolean(Boolean theStringBoolean) {
		this.theStringBoolean = theStringBoolean;
	}

	@Convert( converter = MutableValueConverter.class )
	public MutableValue getMutableValue() {
		return mutableValue;
	}

	public void setMutableValue(MutableValue mutableValue) {
		this.mutableValue = mutableValue;
	}

	public static class MutableValueConverter implements AttributeConverter<MutableValue,String> {
		@Override
		public String convertToDatabaseColumn(MutableValue attribute) {
			return attribute == null ? null : attribute.getState();
		}

		@Override
		public MutableValue convertToEntityAttribute(String dbData) {
			return dbData == null ? null : new MutableValue( dbData );
		}
	}

	public static class GenderConverter implements AttributeConverter<Gender,Character> {
		@Override
		public Character convertToDatabaseColumn(Gender attribute) {
			if ( attribute == null ) {
				return null;
			}

			if ( attribute == Gender.OTHER ) {
				return 'O';
			}

			if ( attribute == Gender.MALE ) {
				return 'M';
			}

			return 'F';
		}

		@Override
		public Gender convertToEntityAttribute(Character dbData) {
			if ( dbData == null ) {
				return null;
			}

			if ( 'O' == dbData ) {
				return Gender.OTHER;
			}

			if ( 'M' == dbData ) {
				return Gender.MALE;
			}

			return Gender.FEMALE;
		}
	}
}

