/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.spi;

/**
 * Represents the status of an entity with respect to
 * this session. These statuses are for internal
 * bookkeeping only and are not intended to represent
 * any notion that is visible to the application
 * program.
 */
public enum Status {
	MANAGED,
	READ_ONLY,
	DELETED,
	GONE,
	LOADING,
	SAVING;

	public boolean isDeletedOrGone() {
		return this == DELETED || this == GONE;
	}
}
