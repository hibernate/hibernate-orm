/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.associations.any;

import org.hibernate.annotations.Any;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

//tag::associations-any-def-example[]
@Entity
@Table(name = "property_holder2")
public class PropertyHolder2 {

	@Id
	private Long id;

	@Any
	@PropertyDiscriminationDef
	@Column(name = "property_type")
	@JoinColumn(name = "property_id")
	private Property property;

	//Getters and setters are omitted for brevity

//end::associations-any-def-example[]
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
//tag::associations-any-def-example[]
}
//end::associations-any-def-example[]
