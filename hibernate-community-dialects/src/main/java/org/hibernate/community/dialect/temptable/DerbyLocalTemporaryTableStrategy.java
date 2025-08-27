/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.temptable;

import org.hibernate.dialect.temptable.StandardLocalTemporaryTableStrategy;

/**
 * Derby specific local temporary table strategy.
 */
public class DerbyLocalTemporaryTableStrategy extends StandardLocalTemporaryTableStrategy {

	public static final DerbyLocalTemporaryTableStrategy INSTANCE = new DerbyLocalTemporaryTableStrategy();

	@Override
	public String adjustTemporaryTableName(String desiredTableName) {
		return "session." + desiredTableName;
	}

	@Override
	public String getTemporaryTableCreateOptions() {
		return "not logged";
	}

	@Override
	public String getTemporaryTableCreateCommand() {
		return "declare global temporary table";
	}

	@Override
	public boolean supportsTemporaryTablePrimaryKey() {
		return false;
	}

}
