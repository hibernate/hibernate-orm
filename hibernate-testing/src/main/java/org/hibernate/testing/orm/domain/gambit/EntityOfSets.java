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
import javax.persistence.CollectionTable;
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

import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.annotations.SortComparator;
import org.hibernate.annotations.SortNatural;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("unused")
@Entity
@Table(name = "entity_containing_sets")
public class EntityOfSets {
	private Integer id;
	private String name;

	private Set<String> setOfBasics;
	private SortedSet<String> sortedSetOfBasics;
	private SortedSet<String> sortedSetOfBasicsWithComparator;

	private Set<String> orderedSetOfBasics;

	private Set<EnumValue> setOfEnums;
	private Set<EnumValue> setOfConvertedEnums;

	private Set<SimpleComponent> setOfComponents;
	private Set<SimpleComponent> extraLazySetOfComponents;

	private Set<SimpleEntity> setOfOneToMany;
	private Set<SimpleEntity> setOfManyToMany;


	public EntityOfSets() {
	}

	public EntityOfSets(Integer id, String name) {
		this.id = id;
		this.name = name;
	}

	@Id
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

	@ElementCollection()
	@CollectionTable( name = "EntityOfSet_basics")
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

	@ElementCollection()
	@CollectionTable( name = "EntityOfSet_orderedSetOfBasics")
	@OrderBy( "" )
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

	@ElementCollection()
	@CollectionTable( name = "EntityOfSet_sortedBasics")
	@SortNatural
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

	@ElementCollection()
	@CollectionTable( name = "EntityOfSet_sortedBasicsWithComparator")
	@SortComparator( SimpleBasicSortComparator.class )
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
	// setOfConvertedEnums

	@ElementCollection
	@Convert(converter = EnumValueConverter.class)
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

	@ElementCollection
	@Enumerated(EnumType.STRING)
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

	@ElementCollection
	@CollectionTable( name = "EntityOfSet_components")
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

	@ElementCollection
	@LazyCollection( LazyCollectionOption.EXTRA )
	@CollectionTable( name = "EntityOfSet_extraLazyComponents")
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

	@OneToMany
	@CollectionTable( name = "EntityOfSet_oneToMany")
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

	@ManyToMany
	@CollectionTable( name = "EntityOfSet_manyToMany")
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
