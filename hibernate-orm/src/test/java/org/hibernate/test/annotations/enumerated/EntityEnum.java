/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.enumerated;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;

import org.hibernate.annotations.Formula;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

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
@TypeDef(typeClass = LastNumberType.class, defaultForType = LastNumber.class)
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

	@Formula("upper('a')")
	@Enumerated(EnumType.STRING)
	private Trimmed formula;

	@Enumerated(EnumType.STRING)
	@ElementCollection(targetClass = Common.class, fetch = FetchType.LAZY)
	@JoinTable(name = "set_enum", joinColumns = { @JoinColumn(name = "entity_id") })
	@Column(name = "common_enum", nullable = false)
	private Set<Common> set = new HashSet<Common>();

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

	public Set<Common> getSet() {
		return set;
	}

	public void setSet(Set<Common> set) {
		this.set = set;
	}
}
