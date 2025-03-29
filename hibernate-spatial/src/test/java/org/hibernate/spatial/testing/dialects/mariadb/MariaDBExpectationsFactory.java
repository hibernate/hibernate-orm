/*
 * SPDX-License-Identifier: Apache-2.0
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
