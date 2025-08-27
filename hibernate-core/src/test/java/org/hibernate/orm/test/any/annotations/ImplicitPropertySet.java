/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.any.annotations;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Table;
import org.hibernate.annotations.Any;
import org.hibernate.annotations.AnyDiscriminatorValue;
import org.hibernate.annotations.AnyKeyJavaClass;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.ManyToAny;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table( name = "property_set" )
public class ImplicitPropertySet {
	private Integer id;
	private String name;
	private Property someProperty;

	private List<Property> generalProperties = new ArrayList<Property>();

	public ImplicitPropertySet() {
		super();
	}

	public ImplicitPropertySet(String name) {
		this.name = name;
	}

	@ManyToAny
	@Column( name = "property_type" )
	@AnyKeyJavaClass( Integer.class )
	@Cascade( CascadeType.ALL )
	@JoinTable(
			name = "obj_properties",
			joinColumns = @JoinColumn( name = "obj_id" ),
			inverseJoinColumns = @JoinColumn( name = "property_id" ) )
	public List<Property> getGeneralProperties() {
		return generalProperties;
	}

	public void setGeneralProperties(List<Property> generalProperties) {
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
	@Column( name = "property_type" )
	@AnyKeyJavaClass( Integer.class )
	@AnyDiscriminatorValue( discriminator = "S", entity = StringProperty.class )
	@AnyDiscriminatorValue( discriminator = "I", entity = IntegerProperty.class )
	@Cascade( value = { CascadeType.ALL } )
	@JoinColumn( name = "property_id" )
	public Property getSomeProperty() {
		return someProperty;
	}

	public void setSomeProperty(Property someProperty) {
		this.someProperty = someProperty;
	}

	public void addGeneralProperty(Property property) {
		this.generalProperties.add( property );
	}
}
