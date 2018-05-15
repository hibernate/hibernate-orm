/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.support.domains.gambit;

import java.net.URL;
import java.sql.Clob;
import java.time.Instant;
import java.util.Date;
import javax.persistence.AttributeConverter;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * @author Steve Ebersole
 */
@Entity
public class EntityOfBasics {

	public enum Gender {
		MALE,
		FEMALE
	}

	private Integer id;
	private String theString;
	private Integer theInteger;
	private int theInt;
	private URL theUrl;
	private Clob theClob;
	private Date theDate;
	private Date theTime;
	private Date theTimestamp;
	private Instant theInstant;
	private Gender gender;
	private Gender convertedGender;
	private Gender ordinalGender;

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
	@Column(name = "converted_gender")
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

	public static class GenderConverter implements AttributeConverter<Gender,Character> {
		@Override
		public Character convertToDatabaseColumn(Gender attribute) {
			if ( attribute == null ) {
				return null;
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

			if ( 'M' == dbData ) {
				return Gender.MALE;
			}

			return Gender.FEMALE;
		}
	}
}

