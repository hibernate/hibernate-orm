/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bootstrap.binding.annotations.access;

import jakarta.persistence.AccessType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;

import jakarta.persistence.Access;

/**
 * @author Vlad Mihalcea
 */
@MappedSuperclass
public class BaseFurniture extends Woody {
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

	@Access(AccessType.PROPERTY)
	public long weight;

	public long getWeight() {
		return weight + 1;
	}

	public void setWeight(long weight) {
		this.weight = weight + 1;
	}
}
