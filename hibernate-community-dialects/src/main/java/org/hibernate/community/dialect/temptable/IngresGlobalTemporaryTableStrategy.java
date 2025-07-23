/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.temptable;

import org.hibernate.dialect.temptable.StandardGlobalTemporaryTableStrategy;

/**
 * Ingres specific global temporary table strategy.
 */
public class IngresGlobalTemporaryTableStrategy extends StandardGlobalTemporaryTableStrategy {

	public static final IngresGlobalTemporaryTableStrategy INSTANCE = new IngresGlobalTemporaryTableStrategy();

	@Override
	public String adjustTemporaryTableName(String desiredTableName) {
		return "session." + desiredTableName;
	}

	@Override
	public String getTemporaryTableCreateOptions() {
		return "on commit preserve rows with norecovery";
	}

	@Override
	public String getTemporaryTableCreateCommand() {
		return "declare global temporary table";
	}
}
