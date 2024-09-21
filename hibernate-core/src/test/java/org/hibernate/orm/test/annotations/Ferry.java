/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations;
import jakarta.persistence.Entity;


/**
 * @author Emmanuel Bernard
 */
@Entity()
public class Ferry extends Boat {
	private String sea;

	public String getSea() {
		return sea;
	}

	public void setSea(String string) {
		sea = string;
	}

}
