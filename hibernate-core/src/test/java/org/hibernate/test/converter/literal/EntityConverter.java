/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.converter.literal;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * @author Janario Oliveira
 */
@Entity
@Table(name = "entity_converter")
public class EntityConverter {
	@Id
	@GeneratedValue
	private Integer id;

	private Letter letterOrdinal;
	@Enumerated(EnumType.STRING)
	private Letter letterString;

	private Numbers numbersImplicit;
	@Convert(converter = QueryLiteralTest.NumberStringConverter.class)
	private Numbers numbersImplicitOverrided;

	private IntegerWrapper integerWrapper;
	private StringWrapper stringWrapper;

	@Convert(converter = QueryLiteralTest.PreFixedStringConverter.class)
	@Column(name = "same_type_converter")
	private String sameTypeConverter;

	public Integer getId() {
		return id;
	}

	public Letter getLetterOrdinal() {
		return letterOrdinal;
	}

	public void setLetterOrdinal(Letter letterOrdinal) {
		this.letterOrdinal = letterOrdinal;
	}

	public Letter getLetterString() {
		return letterString;
	}

	public void setLetterString(Letter letterString) {
		this.letterString = letterString;
	}

	public Numbers getNumbersImplicit() {
		return numbersImplicit;
	}

	public void setNumbersImplicit(Numbers numbersImplicit) {
		this.numbersImplicit = numbersImplicit;
	}

	public Numbers getNumbersImplicitOverrided() {
		return numbersImplicitOverrided;
	}

	public void setNumbersImplicitOverrided(Numbers numbersImplicitOverrided) {
		this.numbersImplicitOverrided = numbersImplicitOverrided;
	}

	public IntegerWrapper getIntegerWrapper() {
		return integerWrapper;
	}

	public void setIntegerWrapper(IntegerWrapper integerWrapper) {
		this.integerWrapper = integerWrapper;
	}

	public StringWrapper getStringWrapper() {
		return stringWrapper;
	}

	public void setStringWrapper(StringWrapper stringWrapper) {
		this.stringWrapper = stringWrapper;
	}

	public String getSameTypeConverter() {
		return sameTypeConverter;
	}

	public void setSameTypeConverter(String sameTypeConverter) {
		this.sameTypeConverter = sameTypeConverter;
	}
}
