/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.filter;
import java.util.Date;
import java.util.Objects;
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

		return Objects.equals( name, category.name) &&
				Objects.equals( effectiveEndDate, category.effectiveEndDate ) &&
				Objects.equals( effectiveStartDate, category.effectiveStartDate );
	}

	public int hashCode() {
		return Objects.hash( name, effectiveEndDate, effectiveStartDate );
	}
}
