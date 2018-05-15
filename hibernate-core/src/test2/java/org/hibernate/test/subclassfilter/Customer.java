/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

// $Id: Customer.java 5899 2005-02-24 20:08:04Z steveebersole $
package org.hibernate.test.subclassfilter;


/**
 * Implementation of Customer.
 *
 * @author Steve Ebersole
 */
public class Customer extends Person {
	private Employee contactOwner;

	public Customer() {
	}

	public Customer(String name) {
		super( name );
	}

	public Employee getContactOwner() {
		return contactOwner;
	}

	public void setContactOwner(Employee contactOwner) {
		this.contactOwner = contactOwner;
	}
}
