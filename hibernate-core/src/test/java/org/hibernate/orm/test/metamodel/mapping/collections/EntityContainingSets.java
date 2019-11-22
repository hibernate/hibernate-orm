/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.metamodel.mapping.collections;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Convert;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

/**
 * @author Steve Ebersole
 */
@Entity(name = "EntityContainingSets")
@Table(name = "entity_containing_sets")
public class EntityContainingSets {
	private Integer id;
	private String name;

	private Set<String> setOfBasics;
	private Set<EnumValue> setOfConvertedBasics;
	private Set<EnumValue> setOfEnums;
	private Set<SomeStuff> setOfComponents;
	private Set<SimpleEntity> setOfEntities;

	public EntityContainingSets() {
	}

	public EntityContainingSets(Integer id, String name) {
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

	@ElementCollection
	@Convert(converter = EnumValueConverter.class)
	public Set<EnumValue> getSetOfConvertedBasics() {
		return setOfConvertedBasics;
	}

	public void setSetOfConvertedBasics(Set<EnumValue> setOfConvertedBasics) {
		this.setOfConvertedBasics = setOfConvertedBasics;
	}

	public void addConvertedBasic(EnumValue value) {
		if ( setOfConvertedBasics == null ) {
			setOfConvertedBasics = new HashSet<>();
		}
		setOfConvertedBasics.add( value );
	}

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

	@ElementCollection
	@Embedded
	public Set<SomeStuff> getSetOfComponents() {
		return setOfComponents;
	}

	public void setSetOfComponents(Set<SomeStuff> setOfComponents) {
		this.setOfComponents = setOfComponents;
	}

	public void addComponent(SomeStuff value) {
		if ( setOfComponents == null ) {
			setOfComponents = new HashSet<>();
		}
		setOfComponents.add( value );
	}

	@OneToMany(cascade = CascadeType.ALL)
	public Set<SimpleEntity> getSetOfEntities() {
		return setOfEntities;
	}

	public void setSetOfEntities(Set<SimpleEntity> setOfEntities) {
		this.setOfEntities = setOfEntities;
	}

	public void addSimpleEntity(SimpleEntity value) {
		if ( setOfEntities == null ) {
			setOfEntities = new HashSet<>();
		}
		setOfEntities.add( value );
	}
}
