/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.temptable.internal;

import org.hibernate.dialect.temptable.spi.StandardLocalTemporaryTableStrategy;

/**
 * Derby specific local temporary table strategy.
 *
 * @author Steve Ebersole
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
