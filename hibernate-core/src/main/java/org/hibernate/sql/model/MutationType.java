/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.model;

/**
 * The type of mutation
 *
 * @author Steve Ebersole
 */
public enum MutationType {
	INSERT( true ),
	UPDATE( true ),
	DELETE( false );

	private final boolean canSkipTables;

	MutationType(boolean canSkipTables) {
		this.canSkipTables = canSkipTables;
	}

	public boolean canSkipTables() {
		return canSkipTables;
	}
}
