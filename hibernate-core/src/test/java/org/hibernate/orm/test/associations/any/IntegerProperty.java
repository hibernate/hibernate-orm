/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.associations.any;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

//tag::associations-any-property-example[]

@Entity
@Table(name="integer_property")
public class IntegerProperty implements Property<Integer> {

	@Id
	private Long id;

	@Column(name = "`name`")
	private String name;

	@Column(name = "`value`")
	private Integer value;

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Integer getValue() {
		return value;
	}

	//Getters and setters omitted for brevity
//end::associations-any-property-example[]

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setValue(Integer value) {
		this.value = value;
	}
//tag::associations-any-property-example[]
}
//end::associations-any-property-example[]
