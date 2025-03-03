/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.sqm.domain;

/**
 * @author Steve Ebersole
 */
public class NestedInjectedLookupListItem extends InjectedLookupListItem implements NestedLookupListItem {
	private LookupListItem nested;

	public void setNested(LookupListItem nested) {
		this.nested = nested;
	}

	@Override
	public LookupListItem getNested() {
		return nested;
	}
}
