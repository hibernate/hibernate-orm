/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
