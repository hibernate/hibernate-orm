/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.sqm.domain;

/**
 * @author Steve Ebersole
 */
public class ConstructedLookupListItem implements LookupListItem {
	private final Integer id;
	private final String displayValue;

	public ConstructedLookupListItem(Integer id, String displayValue) {
		this.id = id;
		this.displayValue = displayValue;
	}

	@Override
	public Integer getId() {
		return id;
	}

	@Override
	public String getDisplayValue() {
		return displayValue;
	}
}
