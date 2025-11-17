/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.collectionbasictype;

import jakarta.persistence.Embeddable;

/**
 * @author Chris Cranford
 */
@Embeddable
public class RoleInternal implements Role {
	public static String TYPE = "Internal";

	@Override
	public String getType() {
		return TYPE;
	}
}
