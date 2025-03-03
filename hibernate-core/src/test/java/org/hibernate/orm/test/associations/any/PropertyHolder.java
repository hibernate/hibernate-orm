/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.associations.any;

import org.hibernate.annotations.Any;
import org.hibernate.annotations.AnyDiscriminator;
import org.hibernate.annotations.AnyDiscriminatorValue;
import org.hibernate.annotations.AnyKeyJavaClass;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

//tag::associations-any-example[]
@Entity
@Table(name = "property_holder")
public class PropertyHolder {

	@Id
	private Long id;

	@Any
	@AnyDiscriminator(DiscriminatorType.STRING)
	@AnyDiscriminatorValue(discriminator = "S", entity = StringProperty.class)
	@AnyDiscriminatorValue(discriminator = "I", entity = IntegerProperty.class)
	@AnyKeyJavaClass(Long.class)
	@Column(name = "property_type")
	@JoinColumn(name = "property_id")
	private Property property;

	//Getters and setters are omitted for brevity

//end::associations-any-example[]
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Property getProperty() {
		return property;
	}

	public void setProperty(Property property) {
		this.property = property;
	}
//tag::associations-any-example[]
}
//end::associations-any-example[]
