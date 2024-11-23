/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.testing.dialects.mariadb;

import org.hibernate.spatial.testing.dialects.mysql.MySQL8ExpectationsFactory;

//for now, create the same expectations as for MySQL8
public class MariaDBExpectationsFactory extends MySQL8ExpectationsFactory {
	public MariaDBExpectationsFactory() {
		super();
	}
}
