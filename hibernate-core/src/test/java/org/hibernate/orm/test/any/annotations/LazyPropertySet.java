/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.any.annotations;

import jakarta.persistence.JoinTable;
import org.hibernate.annotations.Any;
import org.hibernate.annotations.AnyDiscriminator;
import org.hibernate.annotations.AnyDiscriminatorValue;
import org.hibernate.annotations.AnyKeyJavaClass;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import org.hibernate.annotations.ManyToAny;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table( name = "lazy_property_set" )
public class LazyPropertySet {
	private Integer id;
	private String name;
	private Property someProperty;
	private List<Property> generalProperties = new ArrayList<Property>();

	public LazyPropertySet() {
		super();
	}

	public LazyPropertySet(String name) {
		this.name = name;
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

	@Any( fetch = FetchType.LAZY )
	@Column( name = "property_type" )
	@AnyDiscriminator( DiscriminatorType.STRING )
	@AnyKeyJavaClass( Integer.class )
	@AnyDiscriminatorValue( discriminator = "S", entity = StringProperty.class)
	@AnyDiscriminatorValue( discriminator = "I", entity = IntegerProperty.class)
	@Cascade( value = { CascadeType.ALL } )
	@JoinColumn( name = "property_id" )
	public Property getSomeProperty() {
		return someProperty;
	}

	public void setSomeProperty(Property someProperty) {
		this.someProperty = someProperty;
	}

	@ManyToAny(
			fetch = FetchType.LAZY )
	@Column( name = "property_type" )
	@AnyDiscriminator( DiscriminatorType.STRING )
	@AnyKeyJavaClass( Integer.class )
	@AnyDiscriminatorValue( discriminator = "S", entity = StringProperty.class )
	@AnyDiscriminatorValue( discriminator = "I", entity = IntegerProperty.class )
	@Cascade( { org.hibernate.annotations.CascadeType.ALL } )
	@JoinTable( name = "lazy_obj_properties", joinColumns = @JoinColumn( name = "obj_id" ),
			inverseJoinColumns = @JoinColumn( name = "property_id" ) )
	public List<Property> getGeneralProperties() {
		return generalProperties;
	}

	public void setGeneralProperties(List<Property> generalProperties) {
		this.generalProperties = generalProperties;
	}

	public void addGeneralProperty(Property property) {
		this.generalProperties.add( property );
	}
}
