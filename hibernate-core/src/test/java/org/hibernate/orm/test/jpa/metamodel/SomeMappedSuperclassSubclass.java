/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.metamodel;

import jakarta.persistence.Entity;

/**
 * @author Steve Ebersole
 */
@Entity
public class SomeMappedSuperclassSubclass extends SomeMappedSuperclass {
	private String theData;

	public String getTheData() {
		return theData;
	}

	public void setTheData(String theData) {
		this.theData = theData;
	}
}
