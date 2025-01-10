/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.columndiscriminator;

public abstract class BookDetails {
	private String information;

	protected BookDetails(String information) {
		this.information = information;
	}

	protected BookDetails() {
		// default
	}

	public String information() {
		return information;
	}
}
