/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.id.sequences.entities;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;

/**
 * Share the generator table decribed by the GEN_TABLE GeneratedIdTable
 * using the Dog key as discriminator
 *
 * @author Emmanuel Bernard
 */
@Entity
@Table(name = "tbl_dog")
@TableGenerator(name = "DogGen", table = "GENERATOR_TABLE", pkColumnName = "pkey",
		valueColumnName = "hi", pkColumnValue = "Dog", allocationSize = 10)
public class Dog {
	private Integer id;
	private String name;

	@Id
	@GeneratedValue(strategy = GenerationType.TABLE, generator = "DogGen")
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
}
