/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.domain.gambit;

import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.annotations.SortComparator;
import org.hibernate.annotations.SortNatural;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("unused")
@Entity
@Table(name = "entity_containing_sets")
public class EntityOfSets {
	@Id
	private Integer id;
	private String name;

	@ElementCollection()
	@CollectionTable( name = "EntityOfSet_basic1")
	@Column(name = "basic_val")
	private Set<String> setOfBasics;

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Sorted

	@ElementCollection()
	@CollectionTable( name = "EntityOfSet_basic2")
	@Column(name = "basic_val")
	private SortedSet<String> sortedSetOfBasicsWithSortNaturalByDefault;

	@ElementCollection()
	@CollectionTable( name = "EntityOfSet_basic3")
	@SortNatural
	@Column(name = "basic_val")
	private SortedSet<String> sortedSetOfBasics;

	@ElementCollection()
	@CollectionTable( name = "EntityOfSet_basic4")
	@SortComparator( SimpleBasicSortComparator.class )
	@Column(name = "basic_val")
	private SortedSet<String> sortedSetOfBasicsWithComparator;

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Ordered

	@ElementCollection()
	@CollectionTable( name = "EntityOfSet_basic5")
	@OrderBy( "" )
	@Column(name = "basic_val")
	private Set<String> orderedSetOfBasics;

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Enum elements

	@ElementCollection
	@Enumerated(EnumType.STRING)
	@CollectionTable(name = "EntityOfSet_enum1")
	@Column(name = "enum_val")
	private Set<EnumValue> setOfEnums;

	@ElementCollection
	@Convert(converter = EnumValueConverter.class)
	@CollectionTable(name = "EntityOfSet_enum2")
	@Column(name = "enum_val")
	private Set<EnumValue> setOfConvertedEnums;

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Embeddables

	@ElementCollection
	@CollectionTable( name = "EntityOfSet_comp1")
	private Set<SimpleComponent> setOfComponents;

	@ElementCollection
	@LazyCollection( LazyCollectionOption.EXTRA )
	@CollectionTable( name = "EntityOfSet_comp2")
	private Set<SimpleComponent> extraLazySetOfComponents;

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Entity associations

	@OneToMany
	@CollectionTable( name = "EntityOfSet_o2m")
	private Set<SimpleEntity> setOfOneToMany;

	@ManyToMany
	@CollectionTable( name = "EntityOfSet_m2m")
	private Set<SimpleEntity> setOfManyToMany;


	public EntityOfSets() {
	}

	public EntityOfSets(Integer id, String name) {
		this.id = id;
		this.name = name;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// setOfBasics

	public Set<String> getSetOfBasics() {
		return setOfBasics;
	}

	public void setSetOfBasics(Set<String> setOfBasics) {
		this.setOfBasics = setOfBasics;
	}

	public void addBasic(String value) {
		if ( setOfBasics == null ) {
			setOfBasics = new HashSet<>();
		}
		setOfBasics.add( value );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// orderedSetOfBasics

	public Set<String> getOrderedSetOfBasics() {
		return orderedSetOfBasics;
	}

	public void setOrderedSetOfBasics(Set<String> orderedSetOfBasics) {
		this.orderedSetOfBasics = orderedSetOfBasics;
	}

	public void addOrderedBasic(String value) {
		if ( orderedSetOfBasics == null ) {
			orderedSetOfBasics = new TreeSet<>();
		}
		orderedSetOfBasics.add( value );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// sortedSetOfBasics

	public SortedSet<String> getSortedSetOfBasics() {
		return sortedSetOfBasics;
	}

	public void setSortedSetOfBasics(SortedSet<String> sortedSetOfBasics) {
		this.sortedSetOfBasics = sortedSetOfBasics;
	}

	public void addSortedBasic(String value) {
		if ( sortedSetOfBasics == null ) {
			sortedSetOfBasics = new TreeSet<>();
		}
		sortedSetOfBasics.add( value );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// sortedSetOfBasicsWithComparator

	public SortedSet<String> getSortedSetOfBasicsWithComparator() {
		return sortedSetOfBasicsWithComparator;
	}

	public void setSortedSetOfBasicsWithComparator(SortedSet<String> sortedSetOfBasicsWithComparator) {
		this.sortedSetOfBasicsWithComparator = sortedSetOfBasicsWithComparator;
	}

	public void addSortedBasicWithComparator(String value) {
		if ( sortedSetOfBasicsWithComparator == null ) {
			sortedSetOfBasicsWithComparator = new TreeSet<>();
		}
		sortedSetOfBasicsWithComparator.add( value );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// sortedSetOfBasicsWithSortNaturalByDefault

	public SortedSet<String> getSortedSetOfBasicsWithSortNaturalByDefault() {
		return sortedSetOfBasicsWithSortNaturalByDefault;
	}

	public void setSortedSetOfBasicsWithSortNaturalByDefault(SortedSet<String> sortedSetOfBasicsWithSortNaturalByDefault) {
		this.sortedSetOfBasicsWithSortNaturalByDefault = sortedSetOfBasicsWithSortNaturalByDefault;
	}

	public void addSortedBasicWithSortNaturalByDefault(String value) {
		if ( sortedSetOfBasicsWithSortNaturalByDefault == null ) {
			sortedSetOfBasicsWithSortNaturalByDefault = new TreeSet<>();
		}
		sortedSetOfBasicsWithSortNaturalByDefault.add( value );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// setOfConvertedEnums

	public Set<EnumValue> getSetOfConvertedEnums() {
		return setOfConvertedEnums;
	}

	public void setSetOfConvertedEnums(Set<EnumValue> setOfConvertedEnums) {
		this.setOfConvertedEnums = setOfConvertedEnums;
	}

	public void addConvertedEnum(EnumValue value) {
		if ( setOfConvertedEnums == null ) {
			setOfConvertedEnums = new HashSet<>();
		}
		setOfConvertedEnums.add( value );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// setOfEnums

	public Set<EnumValue> getSetOfEnums() {
		return setOfEnums;
	}

	public void setSetOfEnums(Set<EnumValue> setOfEnums) {
		this.setOfEnums = setOfEnums;
	}

	public void addEnum(EnumValue value) {
		if ( setOfEnums == null ) {
			setOfEnums = new HashSet<>();
		}
		setOfEnums.add( value );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// setOfComponents

	public Set<SimpleComponent> getSetOfComponents() {
		return setOfComponents;
	}

	public void setSetOfComponents(Set<SimpleComponent> setOfComponents) {
		this.setOfComponents = setOfComponents;
	}

	public void addComponent(SimpleComponent value) {
		if ( setOfComponents == null ) {
			setOfComponents = new HashSet<>();
		}
		setOfComponents.add( value );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// setOfExtraLazyComponents

	public Set<SimpleComponent> getExtraLazySetOfComponents() {
		return extraLazySetOfComponents;
	}

	public void setExtraLazySetOfComponents(Set<SimpleComponent> extraLazySetOfComponents) {
		this.extraLazySetOfComponents = extraLazySetOfComponents;
	}

	public void addExtraLazyComponent(SimpleComponent value) {
		if ( extraLazySetOfComponents == null ) {
			extraLazySetOfComponents = new HashSet<>();
		}
		extraLazySetOfComponents.add( value );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// setOfOneToMany

	public Set<SimpleEntity> getSetOfOneToMany() {
		return setOfOneToMany;
	}

	public void setSetOfOneToMany(Set<SimpleEntity> setOfOneToMany) {
		this.setOfOneToMany = setOfOneToMany;
	}

	public void addOneToMany(SimpleEntity value) {
		if ( setOfOneToMany == null ) {
			setOfOneToMany = new HashSet<>();
		}
		setOfOneToMany.add( value );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// setOfManyToMany

	public Set<SimpleEntity> getSetOfManyToMany() {
		return setOfManyToMany;
	}

	public void setSetOfManyToMany(Set<SimpleEntity> setOfManyToMany) {
		this.setOfManyToMany = setOfManyToMany;
	}

	public void addManyToMany(SimpleEntity value) {
		if ( setOfManyToMany == null ) {
			setOfManyToMany = new HashSet<>();
		}
		setOfManyToMany.add( value );
	}
}
