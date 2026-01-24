/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.resultmapping.dynamic;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("FieldCanBeLocal")
public class DropDownItem {
	private final Integer key;
	private final String text;

	public DropDownItem(Integer key, String text) {
		this.key = key;
		this.text = text;
	}
}
