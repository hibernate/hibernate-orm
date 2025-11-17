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
@Table( name = "char_property" )
public class CharProperty implements Property {
	private Integer id;

	private String name;

	private Character value;

	public CharProperty() {
		super();
	}

	public CharProperty(String name, Character value) {
		super();
		this.name = name;
		this.value = value;
	}

	public String asString() {
		return Character.toString( value );
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
	public Character getValue() {
		return value;
	}

	public void setValue(Character value) {
		this.value = value;
	}

	public void setName(String name) {
		this.name = name;
	}

}
