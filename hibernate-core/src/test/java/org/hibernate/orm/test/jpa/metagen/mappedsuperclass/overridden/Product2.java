/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.metagen.mappedsuperclass.overridden;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

/**
 * @author Oliver Breidenbach
 */
@Entity
@Access(AccessType.PROPERTY)
public class Product2 extends Product1 {

	@Column(name = "overridenName"/*, insertable = false, updatable = false*/)
	public String getOverridenName() {
		return super.getOverridenName();
	}

	public void setOverridenName(String overridenName) {
		super.setOverridenName(overridenName);
	}
}
