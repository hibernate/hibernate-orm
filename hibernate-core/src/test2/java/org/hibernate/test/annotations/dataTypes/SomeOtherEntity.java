/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.dataTypes;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * @author Steve Ebersole
 */
@Entity
@Access(AccessType.FIELD)
@Table(name = "SOMEOTHERENTITY")
public class SomeOtherEntity {
	@Id
	protected int id;
	protected boolean booleanData;
	protected byte byteData;
	// setting a arbitrary character here to make this test also pass against PostgreSQL
	// PostgreSQL throws otherwise an exception when persisting the null value
	// org.postgresql.util.PSQLException: ERROR: invalid byte sequence for encoding "UTF8": 0x00
	protected char characterData = 'a';
	protected short shortData;
	protected int integerData;
	protected long longData;
	protected double doubleData;
	protected float floatData;
	@Enumerated(EnumType.STRING)
	protected Grade grade;


	public SomeOtherEntity() {
	}

	public SomeOtherEntity(int id) {
		this.id = id;
	}

	public SomeOtherEntity(
			int id,
			boolean booleanData,
			byte byteData,
			char characterData,
			short shortData,
			int integerData,
			long longData,
			double doubleData,
			float floatData) {
		this.id = id;
		this.booleanData = booleanData;
		this.byteData = byteData;
		this.characterData = characterData;
		this.shortData = shortData;
		this.integerData = integerData;
		this.longData = longData;
		this.doubleData = doubleData;
		this.floatData = floatData;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Character getCharacterData() {
		return characterData;
	}

	public void setCharacterData(Character characterData) {
		this.characterData = characterData;
	}

	public Short getShortData() {
		return shortData;
	}

	public void setShortData(Short shortData) {
		this.shortData = shortData;
	}

	public Integer getIntegerData() {
		return integerData;
	}

	public void setIntegerData(Integer integerData) {
		this.integerData = integerData;
	}

	public Long getLongData() {
		return longData;
	}

	public void setLongData(Long longData) {
		this.longData = longData;
	}

	public Double getDoubleData() {
		return doubleData;
	}

	public void setDoubleData(Double doubleData) {
		this.doubleData = doubleData;
	}

	public Float getFloatData() {
		return floatData;
	}

	public void setFloatData(Float floatData) {
		this.floatData = floatData;
	}

	public Grade getGrade() {
		return grade;
	}

	public void setGrade(Grade grade) {
		this.grade = grade;
	}
}
