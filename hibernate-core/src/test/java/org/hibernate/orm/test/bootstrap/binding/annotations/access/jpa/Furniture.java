/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.binding.annotations.access.jpa;
import jakarta.persistence.Access;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Access(jakarta.persistence.AccessType.FIELD)
public class Furniture extends Woody {
	@Id
	@GeneratedValue
	private Integer id;

	private String brand;

	@Transient
	public String getBrand() {
		return brand;
	}

	public void setBrand(String brand) {
		this.brand = brand;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public long weight;

	@Access(jakarta.persistence.AccessType.PROPERTY)
	public long getWeight() {
		return weight + 1;
	}

	public void setWeight(long weight) {
		this.weight = weight + 1;
	}
}
