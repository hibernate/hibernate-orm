/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.columndiscriminator;

public class BoringBookDetails extends BookDetails {
	private String boringInformation;

	public BoringBookDetails(String information, String boringInformation) {
		super(information);
		this.boringInformation = boringInformation;
	}

	public BoringBookDetails() {
		// default
	}

	public String boringInformation() {
		return boringInformation;
	}
}
