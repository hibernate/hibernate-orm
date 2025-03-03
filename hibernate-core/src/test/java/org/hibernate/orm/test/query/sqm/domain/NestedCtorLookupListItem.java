/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.sqm.domain;

/**
 * @author Steve Ebersole
 */
public class NestedCtorLookupListItem extends ConstructedLookupListItem implements NestedLookupListItem {
	private final LookupListItem nested;

	public NestedCtorLookupListItem(
			Integer id,
			String displayValue,
			LookupListItem nested) {
		super( id, displayValue );
		this.nested = nested;
	}

	@Override
	public LookupListItem getNested() {
		return nested;
	}
}
