/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect.functional;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

/**
 * @author Guenther Demetz
 */
@Entity
public class Product2 implements Serializable {
	@Id
	public Integer id;

	@Column(name = "description", nullable = false)
	public String description;

	@ManyToOne
	public Category category;

	public Product2() {
	}

	public Product2(Integer id, String description) {
		this.id = id;
		this.description = description;
	}

	public Product2(Integer id, String description, Category category) {
		this.category = category;
		this.description = description;
		this.id = id;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) return true;
		if ( !(o instanceof Product2) ) return false;

		Product2 product2 = (Product2) o;

		if ( description != null ? !description.equals( product2.description ) : product2.description != null ) return false;
		if ( id != null ? !id.equals( product2.id ) : product2.id != null ) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = id != null ? id.hashCode() : 0;
		result = 31 * result + ( description != null ? description.hashCode() : 0 );
		return result;
	}

	@Override
	public String toString() {
		return "Product2(id = " + id + ", description = " + description + ")";
	}
}
