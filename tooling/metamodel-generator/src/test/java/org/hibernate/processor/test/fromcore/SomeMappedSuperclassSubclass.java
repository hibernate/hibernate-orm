/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.fromcore;

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
