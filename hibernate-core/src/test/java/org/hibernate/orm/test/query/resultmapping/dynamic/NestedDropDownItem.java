/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.resultmapping.dynamic;

/**
 * @author Steve Ebersole
 */
public class NestedDropDownItem {
	private final Object key;
	private final String text;
	private final DropDownItem nested;

	public NestedDropDownItem(Integer key, String text, DropDownItem nested) {
		this.key = key;
		this.text = text;
		this.nested = nested;
	}

	public NestedDropDownItem(Object key, String text, DropDownItem nested) {
		this.key = key;
		this.text = text;
		this.nested = nested;
	}
}
