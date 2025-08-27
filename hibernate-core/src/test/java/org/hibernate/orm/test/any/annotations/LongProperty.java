/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.any.annotations;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;

@Entity
@Table(name = "long_property")
public class LongProperty implements Property {
	private Integer id;

	private String name;
	private Long value;

	public LongProperty() {
		super();
	}

	public LongProperty(String name, Long value) {
		super();
		this.name = name;
		this.value = value;
	}

	public String asString() {
		return Long.toString(value);
	}

	public String getName() {
		return name;
	}

	@Id
	@GeneratedValue
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Column(name = "`value`")
	public Long getValue() {
		return value;
	}

	public void setValue(Long value) {
		this.value = value;
	}

	public void setName(String name) {
		this.name = name;
	}

}
