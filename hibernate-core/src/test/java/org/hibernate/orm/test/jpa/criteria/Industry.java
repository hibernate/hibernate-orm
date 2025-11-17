/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria;

import jakarta.persistence.Embeddable;

/**
 * @author Chris Cranford
 */
@Embeddable
public class Industry implements Attribute {
	public static String TYPE = "Industry";

	@Override
	public String getType() {
		return TYPE;
	}
}
