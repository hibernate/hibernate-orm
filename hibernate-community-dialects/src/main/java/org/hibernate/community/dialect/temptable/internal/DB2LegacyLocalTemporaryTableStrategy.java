/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.temptable.internal;

import org.hibernate.dialect.temptable.spi.StandardLocalTemporaryTableStrategy;
import org.hibernate.query.sqm.mutation.spi.AfterUseAction;

/**
 * Legacy DB2 specific local temporary table strategy.
 *
 * @author Steve Ebersole
 */
public class DB2LegacyLocalTemporaryTableStrategy extends StandardLocalTemporaryTableStrategy {

	public static final DB2LegacyLocalTemporaryTableStrategy INSTANCE = new DB2LegacyLocalTemporaryTableStrategy();

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
	public AfterUseAction getTemporaryTableAfterUseAction() {
		return AfterUseAction.DROP;
	}
}
