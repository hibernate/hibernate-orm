/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.domain.gambit;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CollectionTable;
import javax.persistence.Convert;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("unused")
@Entity
public class EntityOfLists {
	private Integer id;
	private String name;

	private List<String> listOfBasics;
	private List<Double> listOfNumbers;

	private List<EnumValue> listOfConvertedEnums;
	private List<EnumValue> listOfEnums;

	private List<SimpleComponent> listOfComponents;

	private List<SimpleEntity> listOfOneToMany;
	private List<SimpleEntity> listOfManyToMany;

	public EntityOfLists() {
	}

	public EntityOfLists(Integer id, String name) {
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
	// listOfBasics

	@ElementCollection
	@OrderColumn
	@CollectionTable(name = "EntityOfLists_basic")
	public List<String> getListOfBasics() {
		return listOfBasics;
	}

	public void setListOfBasics(List<String> listOfBasics) {
		this.listOfBasics = listOfBasics;
	}

	@ElementCollection
	@OrderColumn(name="num_indx")
	@CollectionTable(name = "EntityOfLists_numbers")
	public List<Double> getListOfNumbers() {
		return listOfNumbers;
	}

	public void setListOfNumbers(List<Double> listOfNumbers) {
		this.listOfNumbers = listOfNumbers;
	}

	public void addBasic(String basic) {
		if ( listOfBasics == null ) {
			listOfBasics = new ArrayList<>();
		}
		listOfBasics.add( basic );
	}

	public void addNumber(double number) {
		if ( listOfNumbers == null ) {
			listOfNumbers = new ArrayList<>();
		}
		listOfNumbers.add( number );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// listOfConvertedEnums

	@ElementCollection
	@OrderColumn
	@Convert(converter = EnumValueConverter.class)
	@CollectionTable(name = "EntityOfLists_enum1")
	public List<EnumValue> getListOfConvertedEnums() {
		return listOfConvertedEnums;
	}

	public void setListOfConvertedEnums(List<EnumValue> listOfConvertedEnums) {
		this.listOfConvertedEnums = listOfConvertedEnums;
	}

	public void addConvertedEnum(EnumValue value) {
		if ( listOfConvertedEnums == null ) {
			listOfConvertedEnums = new ArrayList<>();
		}
		listOfConvertedEnums.add( value );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// listOfEnums

	@ElementCollection
	@Enumerated(EnumType.STRING)
	@OrderColumn
	@CollectionTable(name = "EntityOfLists_enum2")
	public List<EnumValue> getListOfEnums() {
		return listOfEnums;
	}

	public void setListOfEnums(List<EnumValue> listOfEnums) {
		this.listOfEnums = listOfEnums;
	}

	public void addEnum(EnumValue value) {
		if ( listOfEnums == null ) {
			listOfEnums = new ArrayList<>();
		}
		listOfEnums.add( value );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// listOfComponents

	@ElementCollection
	@OrderColumn
	@CollectionTable(name = "EntityOfLists_comp")
	public List<SimpleComponent> getListOfComponents() {
		return listOfComponents;
	}

	public void setListOfComponents(List<SimpleComponent> listOfComponents) {
		this.listOfComponents = listOfComponents;
	}

	public void addComponent(SimpleComponent value) {
		if ( listOfComponents == null ) {
			listOfComponents = new ArrayList<>();
		}
		listOfComponents.add( value );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// listOfOneToMany

	@OneToMany
	@OrderColumn
	@CollectionTable(name = "EntityOfLists_o2m")
	public List<SimpleEntity> getListOfOneToMany() {
		return listOfOneToMany;
	}

	public void setListOfOneToMany(List<SimpleEntity> listOfOneToMany) {
		this.listOfOneToMany = listOfOneToMany;
	}

	public void addOneToMany(SimpleEntity value) {
		if ( listOfOneToMany == null ) {
			listOfOneToMany = new ArrayList<>();
		}
		listOfOneToMany.add( value );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// listOfManyToMany

	@ManyToMany
	@OrderColumn
	@CollectionTable(name = "EntityOfLists_m2m")
	public List<SimpleEntity> getListOfManyToMany() {
		return listOfManyToMany;
	}

	public void setListOfManyToMany(List<SimpleEntity> listOfManyToMany) {
		this.listOfManyToMany = listOfManyToMany;
	}

	public void addManyToMany(SimpleEntity value) {
		if ( listOfManyToMany == null ) {
			listOfManyToMany = new ArrayList<>();
		}
		listOfManyToMany.add( value );
	}
}
