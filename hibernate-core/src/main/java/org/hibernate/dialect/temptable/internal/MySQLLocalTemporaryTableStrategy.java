/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.temptable.internal;

import org.hibernate.dialect.temptable.spi.StandardLocalTemporaryTableStrategy;
import org.hibernate.query.sqm.mutation.spi.AfterUseAction;

/**
 * MySQL specific local temporary table strategy.
 *
 * @author Steve Ebersole
 */
public class MySQLLocalTemporaryTableStrategy extends StandardLocalTemporaryTableStrategy {

	public static final MySQLLocalTemporaryTableStrategy INSTANCE = new MySQLLocalTemporaryTableStrategy();

	@Override
	public String getTemporaryTableCreateCommand() {
		return "create temporary table if not exists";
	}

	@Override
	public String getTemporaryTableDropCommand() {
		return "drop temporary table";
	}

	@Override
	public AfterUseAction getTemporaryTableAfterUseAction() {
		return AfterUseAction.DROP;
	}

}
