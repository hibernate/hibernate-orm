/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.any.xml2;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name="int_property")
public class NamedIntegerProperty implements NamedProperty {
	private Integer id;
	private String name;
	private Integer value;

	public NamedIntegerProperty() {
		super();
	}

	public NamedIntegerProperty(int id, String name, Integer value) {
		super();
		this.id = id;
		this.name = name;
		this.value = value;
	}

	public String asString() {
		return Integer.toString(value);
	}

	public String getName() {
		return name;
	}

	@Id
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Column(name = "`value`")
	public Integer getValue() {
		return value;
	}

	public void setValue(Integer value) {
		this.value = value;
	}

	public void setName(String name) {
		this.name = name;
	}


}
