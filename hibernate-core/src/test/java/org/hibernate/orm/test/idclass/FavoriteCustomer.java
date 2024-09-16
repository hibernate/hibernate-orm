/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.idclass;

/**
 * @author Emmanuel Bernard
 */
public class FavoriteCustomer extends Customer {
	public FavoriteCustomer() {
	}

	public FavoriteCustomer(String orgName, String custName, String add) {
		super( orgName, custName, add );
	}
}
