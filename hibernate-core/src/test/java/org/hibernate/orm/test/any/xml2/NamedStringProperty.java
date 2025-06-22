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
@Table(name="string_property")
public class NamedStringProperty implements NamedProperty {
	private Integer id;
	private String name;
	private String value;

	public NamedStringProperty() {
		super();
	}

	public NamedStringProperty(int id, String name, String value) {
		super();
		this.id = id;
		this.name = name;
		this.value = value;
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

	public String asString() {
		return value;
	}

	@Column(name = "`value`")
	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public void setName(String name) {
		this.name = name;
	}
}
