/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.columndiscriminator;

public class SpecialBookDetails extends BookDetails {
	private String specialInformation;

	public SpecialBookDetails(String information, String specialInformation) {
		super(information);
		this.specialInformation = specialInformation;
	}

	protected SpecialBookDetails() {
		// default
	}

	public String specialInformation() {
		return specialInformation;
	}
}
