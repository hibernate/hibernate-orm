/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria;

import jakarta.persistence.Embeddable;

/**
 * @author Chris Cranford
 */
@Embeddable
public class Color implements Attribute {
	public static String TYPE = "Color";

	@Override
	public String getType() {
		return TYPE;
	}
}
