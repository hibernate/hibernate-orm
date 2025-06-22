/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;


/**
 * Are the columns forced to null, not null or not forced
 *
 * @author Emmanuel Bernard
 */
public enum Nullability {
	FORCED_NULL,
	FORCED_NOT_NULL,
	NO_CONSTRAINT
}
