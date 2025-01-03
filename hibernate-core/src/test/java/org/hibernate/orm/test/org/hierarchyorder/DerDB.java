/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.org.hierarchyorder;


import jakarta.persistence.Embeddable;

/*
 * Created on 03/12/2024 by Paul Harrison (paul.harrison@manchester.ac.uk).
 */
@Embeddable
public class DerDB extends BaseD {
public DerDB(int b) {
	this.b = b;
}

public int b;
public DerDB() {

}
}
