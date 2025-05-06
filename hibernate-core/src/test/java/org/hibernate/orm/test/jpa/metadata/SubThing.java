/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.metadata;


/**
 * @author Emmanuel Bernard
 */
//not an entity but in between mapped superclass and entity
public class SubThing extends Thing {
	private String blah;

	public String getBlah() {
		return blah;
	}

	public void setBlah(String blah) {
		this.blah = blah;
	}
}
