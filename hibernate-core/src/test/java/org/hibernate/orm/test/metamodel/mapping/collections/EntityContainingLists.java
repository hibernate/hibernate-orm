/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.metamodel.mapping.collections;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Convert;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;
import javax.persistence.Table;

/**
 * @author Steve Ebersole
 */
@Entity(name = "EntityContainingLists")
@Table(name = "entity_containing_lists")
public class EntityContainingLists {
	private Integer id;
	private String name;

	private List<String> listOfBasics;
	private List<EnumValue> listOfConvertedBasics;
	private List<EnumValue> listOfEnums;
	private List<SomeStuff> listOfComponents;
	private List<SimpleEntity> listOfEntities;

	public EntityContainingLists() {
	}

	public EntityContainingLists(Integer id, String name) {
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

	@ElementCollection
	@OrderColumn
	public List<String> getListOfBasics() {
		return listOfBasics;
	}

	public void setListOfBasics(List<String> listOfBasics) {
		this.listOfBasics = listOfBasics;
	}

	public void addBasic(String basic) {
		if ( listOfBasics == null ) {
			listOfBasics = new ArrayList<>();
		}
		listOfBasics.add( basic );
	}

	@ElementCollection
	@OrderColumn
	@Convert(converter = EnumValueConverter.class)
	public List<EnumValue> getListOfConvertedBasics() {
		return listOfConvertedBasics;
	}

	public void setListOfConvertedBasics(List<EnumValue> listOfConvertedBasics) {
		this.listOfConvertedBasics = listOfConvertedBasics;
	}

	public void addConvertedBasic(EnumValue value) {
		if ( listOfConvertedBasics == null ) {
			listOfConvertedBasics = new ArrayList<>();
		}
		listOfConvertedBasics.add( value );
	}

	@ElementCollection
	@Enumerated(EnumType.STRING)
	@OrderColumn
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

	@ElementCollection
	@OrderColumn
	public List<SomeStuff> getListOfComponents() {
		return listOfComponents;
	}

	public void setListOfComponents(List<SomeStuff> listOfComponents) {
		this.listOfComponents = listOfComponents;
	}

	public void addComponent(SomeStuff value) {
		if ( listOfComponents == null ) {
			listOfComponents = new ArrayList<>();
		}
		listOfComponents.add( value );
	}

	@OneToMany(cascade = CascadeType.ALL)
	@OrderColumn
	public List<SimpleEntity> getListOfEntities() {
		return listOfEntities;
	}

	public void setListOfEntities(List<SimpleEntity> listOfEntities) {
		this.listOfEntities = listOfEntities;
	}

	public void addSimpleEntity(SimpleEntity value) {
		if ( listOfEntities == null ) {
			listOfEntities = new ArrayList<>();
		}
		listOfEntities.add( value );
	}
}
