/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.embeddable.strategy.instantiator.intf;

//tag::embeddable-instantiator-property[]
public class NameImpl implements Name {
	private final String first;
	private final String last;

	private NameImpl() {
		throw new UnsupportedOperationException();
	}

	public NameImpl(String first, String last) {
		this.first = first;
		this.last = last;
	}

	@Override
	public String getFirstName() {
		return first;
	}

	@Override
	public String getLastName() {
		return last;
	}
}
//end::embeddable-instantiator-property[]
