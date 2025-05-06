/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.embeddable.strategy.usertype.embedded;

//tag::embeddable-usertype-domain[]
public class Name {
	private final String first;
	private final String last;

	public Name(String first, String last) {
		this.first = first;
		this.last = last;
	}

	public String firstName() {
		return first;
	}

	public String lastName() {
		return last;
	}
}
//end::embeddable-usertype-domain[]
