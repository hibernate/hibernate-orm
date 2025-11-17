/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.any.annotations;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.Any;
import org.hibernate.annotations.AnyDiscriminatorValue;
import org.hibernate.annotations.AnyKeyJavaClass;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.ManyToAny;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

@Entity
@Table( name = "property_list" )
public class PropertyList<T extends Property> {
	private Integer id;

	private String name;

	private T someProperty;

	private List<T> generalProperties = new ArrayList<T>();

	public PropertyList() {
		super();
	}

	public PropertyList(String name) {
		this.name = name;
	}

	@ManyToAny
	@Column(name = "property_type")
	@AnyKeyJavaClass( Integer.class )
	@AnyDiscriminatorValue( discriminator = "C", entity = CharProperty.class )
	@AnyDiscriminatorValue( discriminator = "I", entity = IntegerProperty.class)
	@AnyDiscriminatorValue( discriminator = "S", entity = StringProperty.class)
	@AnyDiscriminatorValue( discriminator = "L", entity = LongProperty.class)
	@Cascade( { CascadeType.ALL })
	@JoinTable(name = "list_properties",
			joinColumns = @JoinColumn(name = "obj_id"),
			inverseJoinColumns = @JoinColumn(name = "property_id")
	)
	@OrderColumn(name = "prop_index")
	public List<T> getGeneralProperties() {
		return generalProperties;
	}

	public void setGeneralProperties(List<T> generalProperties) {
		this.generalProperties = generalProperties;
	}

	@Id
	@GeneratedValue
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

	@Any
	@Column(name = "property_type")
	@JoinColumn(name = "property_id")
	@AnyKeyJavaClass( Integer.class )
	@AnyDiscriminatorValue( discriminator = "C", entity = CharProperty.class )
	@AnyDiscriminatorValue( discriminator = "I", entity = IntegerProperty.class)
	@AnyDiscriminatorValue( discriminator = "S", entity = StringProperty.class)
	@AnyDiscriminatorValue( discriminator = "L", entity = LongProperty.class)
	@Cascade( CascadeType.ALL )
	public T getSomeProperty() {
		return someProperty;
	}

	public void setSomeProperty(T someProperty) {
		this.someProperty = someProperty;
	}

	public void addGeneralProperty(T property) {
		this.generalProperties.add( property );
	}
}
