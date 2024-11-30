/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.entities.ids;


/**
 * @author Slawek Garwol (slawekgarwol at gmail dot com)
 */
public enum CustomEnum {
	YES,
	NO;

	public String toYesNo() {
		return this == YES ? "Y" : "N";
	}

	public static CustomEnum fromYesNo(String value) {
		return "Y".equals( value ) ? YES : NO;
	}
}
