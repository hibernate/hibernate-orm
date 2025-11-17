/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.temptable;

import org.hibernate.query.sqm.mutation.spi.AfterUseAction;

/**
 * Transact-SQL specific local temporary table strategy.
 */
public class TransactSQLLocalTemporaryTableStrategy extends StandardLocalTemporaryTableStrategy {

	public static final TransactSQLLocalTemporaryTableStrategy INSTANCE = new TransactSQLLocalTemporaryTableStrategy();

	@Override
	public String adjustTemporaryTableName(String desiredTableName) {
		return '#' + desiredTableName;
	}

	@Override
	public String getTemporaryTableCreateCommand() {
		return "create table";
	}

	@Override
	public AfterUseAction getTemporaryTableAfterUseAction() {
		// sql-server, at least needed this dropped after use; strange!
		return AfterUseAction.DROP;
	}

}
