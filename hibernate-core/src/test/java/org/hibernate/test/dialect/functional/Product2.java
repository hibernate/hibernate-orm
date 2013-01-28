/*
  * Hibernate, Relational Persistence for Idiomatic Java
  *
  * Copyright (c) 2009, Red Hat, Inc. and/or its affiliates or third-
  * party contributors as indicated by the @author tags or express
  * copyright attribution statements applied by the authors.
  * All third-party contributions are distributed under license by
  * Red Hat, Inc.
  *
  * This copyrighted material is made available to anyone wishing to
  * use, modify, copy, or redistribute it subject to the terms and
  * conditions of the GNU Lesser General Public License, as published
  * by the Free Software Foundation.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  * Lesser General Public License for more details.
  *
  * You should have received a copy of the GNU Lesser General Public
  * License along with this distribution; if not, write to:
  *
  * Free Software Foundation, Inc.
  * 51 Franklin Street, Fifth Floor
  * Boston, MA  02110-1301  USA
  */

package org.hibernate.test.dialect.functional;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

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
