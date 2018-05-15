/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: J.java 7203 2005-06-19 02:01:05Z oneovthafew $
package org.hibernate.test.legacy;


/**
 * @author Gavin King
 */
public class J extends I {
	private float amount;

	void setAmount(float amount) {
		this.amount = amount;
	}

	float getAmount() {
		return amount;
	}
}
