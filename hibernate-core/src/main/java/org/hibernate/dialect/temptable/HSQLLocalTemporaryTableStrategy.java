/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.temptable;

import org.hibernate.query.sqm.mutation.spi.AfterUseAction;

/**
 * HSQL specific local temporary table strategy.
 */
public class HSQLLocalTemporaryTableStrategy extends StandardLocalTemporaryTableStrategy {

	public static final HSQLLocalTemporaryTableStrategy INSTANCE = new HSQLLocalTemporaryTableStrategy();

	@Override
	public String adjustTemporaryTableName(String desiredTableName) {
		// With HSQLDB 2.0, the table name is qualified with session to assist the drop
		// statement (in-case there is a global name beginning with HT_)
		return "session." + desiredTableName;
	}

	@Override
	public String getTemporaryTableCreateCommand() {
		return "declare local temporary table";
	}

	@Override
	public AfterUseAction getTemporaryTableAfterUseAction() {
		return AfterUseAction.DROP;
	}
}
