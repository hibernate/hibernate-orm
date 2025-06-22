/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.any.annotations;

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

@Entity
@Table(name = "property_holder")
public class PropertyHolder {

	@Id
	private Integer id;

	@Any
	@AnyDiscriminator(DiscriminatorType.STRING)
	@AnyDiscriminatorValue(discriminator = "S", entity = StringProperty.class)
	@AnyDiscriminatorValue(discriminator = "I", entity = IntegerProperty.class)
	@AnyKeyJavaClass(Integer.class)
	@Column(name = "property_type")
	@JoinColumn(name = "property_id")
	private Property property;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Property getProperty() {
		return property;
	}

	public void setProperty(Property property) {
		this.property = property;
	}
}
