/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

// $Id: Category.java 6507 2005-04-25 16:57:32Z steveebersole $
package org.hibernate.test.filter;
import java.util.Date;
import java.util.Set;

/**
 * Implementation of Category.
 *
 * @author Steve Ebersole
 */
public class Category {
	private Long id;
	private String name;
	private Date effectiveStartDate;
	private Date effectiveEndDate;
	private Set products;

	public Category() {
	}

	public Category(String name) {
		this.name = name;
	}

	public Category(String name, Date effectiveStartDate, Date effectiveEndDate) {
		this.name = name;
		this.effectiveStartDate = effectiveStartDate;
		this.effectiveEndDate = effectiveEndDate;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Date getEffectiveStartDate() {
		return effectiveStartDate;
	}

	public void setEffectiveStartDate(Date effectiveStartDate) {
		this.effectiveStartDate = effectiveStartDate;
	}

	public Date getEffectiveEndDate() {
		return effectiveEndDate;
	}

	public void setEffectiveEndDate(Date effectiveEndDate) {
		this.effectiveEndDate = effectiveEndDate;
	}

	public Set getProducts() {
		return products;
	}

	public void setProducts(Set products) {
		this.products = products;
	}

	public boolean equals(Object o) {
		if ( this == o ) return true;
		if ( !( o instanceof Category ) ) return false;

		final Category category = ( Category ) o;

		if ( !name.equals( category.name ) ) {
			return false;
		}

		if ( effectiveEndDate != null ?
		        !effectiveEndDate.equals( category.effectiveEndDate ) :
		        category.effectiveEndDate != null ) {
			return false;
		}

		if ( effectiveStartDate != null ?
		        !effectiveStartDate.equals( category.effectiveStartDate ) :
		        category.effectiveStartDate != null ) {
			return false;
		}

		return true;
	}

	public int hashCode() {
		int result;
		result = name.hashCode();
		result = 29 * result + ( effectiveStartDate != null ? effectiveStartDate.hashCode() : 0 );
		result = 29 * result + ( effectiveEndDate != null ? effectiveEndDate.hashCode() : 0 );
		return result;
	}
}
