/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
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
