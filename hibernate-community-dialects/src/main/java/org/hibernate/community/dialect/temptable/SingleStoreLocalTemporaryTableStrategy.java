/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.temptable;

import org.hibernate.dialect.temptable.StandardLocalTemporaryTableStrategy;
import org.hibernate.query.sqm.mutation.spi.AfterUseAction;

/**
 * SingleStore specific local temporary table strategy.
 */
public class SingleStoreLocalTemporaryTableStrategy extends StandardLocalTemporaryTableStrategy {

	public static final SingleStoreLocalTemporaryTableStrategy INSTANCE = new SingleStoreLocalTemporaryTableStrategy();

	@Override
	public String getTemporaryTableCreateCommand() {
		return "create temporary table if not exists";
	}

	//SingleStore throws an error on drop temporary table if there are uncommitted statements within transaction.
	//Just 'drop table' statement causes implicit commit, so using 'delete from'.
	@Override
	public String getTemporaryTableDropCommand() {
		return "delete from";
	}

	@Override
	public AfterUseAction getTemporaryTableAfterUseAction() {
		return AfterUseAction.DROP;
	}
}
