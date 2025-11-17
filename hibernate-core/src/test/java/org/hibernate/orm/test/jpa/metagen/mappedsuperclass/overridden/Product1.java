/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.metagen.mappedsuperclass.overridden;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * @author Oliver Breidenbach
 */
@Entity
@Table(name = "product")
@Access(AccessType.PROPERTY)
public class Product1 extends AbstractProduct {

	private String overridenName;

	public Product1() {
	}

	public Product1(String name) {
		super( name );
	}


	@Column(name = "overridenName")
	public String getOverridenName() {
		return overridenName;
	}

	public void setOverridenName(String overridenName) {
		this.overridenName = overridenName;
	}
}
