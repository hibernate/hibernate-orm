/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.idclass;

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
