package org.hibernate.test.annotations.enumerated;

import javax.persistence.*;
import javax.persistence.Entity;

import org.hibernate.annotations.*;

import org.hibernate.test.annotations.enumerated.custom_types.LastNumberType;
import org.hibernate.test.annotations.enumerated.enums.Common;
import org.hibernate.test.annotations.enumerated.enums.FirstLetter;
import org.hibernate.test.annotations.enumerated.enums.LastNumber;
import org.hibernate.test.annotations.enumerated.enums.Trimmed;

/**
 * @author Janario Oliveira
 * @author Brett Meyer
 */
@Entity
@TypeDefs({ @TypeDef(typeClass = LastNumberType.class, defaultForType = LastNumber.class) })
public class EntityEnum {

	@Id
	@GeneratedValue
	private long id;
	private Common ordinal;
	@Enumerated(EnumType.STRING)
	private Common string;
	@Type(type = "org.hibernate.test.annotations.enumerated.custom_types.FirstLetterType")
	private FirstLetter firstLetter;
	private LastNumber lastNumber;
	@Enumerated(EnumType.STRING)
	private LastNumber explicitOverridingImplicit;
	@Column(columnDefinition = "char(5)")
	@Enumerated(EnumType.STRING)
	private Trimmed trimmed;

	@Formula("(select 'A' from dual)")
	@Enumerated(EnumType.STRING)
	private Trimmed formula;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public Common getOrdinal() {
		return ordinal;
	}

	public void setOrdinal(Common ordinal) {
		this.ordinal = ordinal;
	}

	public Common getString() {
		return string;
	}

	public void setString(Common string) {
		this.string = string;
	}

	public FirstLetter getFirstLetter() {
		return firstLetter;
	}

	public void setFirstLetter(FirstLetter firstLetter) {
		this.firstLetter = firstLetter;
	}

	public LastNumber getLastNumber() {
		return lastNumber;
	}

	public void setLastNumber(LastNumber lastNumber) {
		this.lastNumber = lastNumber;
	}

	public LastNumber getExplicitOverridingImplicit() {
		return explicitOverridingImplicit;
	}

	public void setExplicitOverridingImplicit(LastNumber explicitOverridingImplicit) {
		this.explicitOverridingImplicit = explicitOverridingImplicit;
	}

	public Trimmed getTrimmed() {
		return trimmed;
	}

	public void setTrimmed(Trimmed trimmed) {
		this.trimmed = trimmed;
	}

	public Trimmed getFormula() {
		return formula;
	}

	public void setFormula(Trimmed formula) {
		this.formula = formula;
	}
}
