/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.temptable;

/**
 * Strategy to interact with Oracle private temporary tables that were introduced in Oracle 18c.
 */
public class OracleLocalTemporaryTableStrategy extends StandardLocalTemporaryTableStrategy {

	public static final OracleLocalTemporaryTableStrategy INSTANCE = new OracleLocalTemporaryTableStrategy();

	@Override
	public String adjustTemporaryTableName(String desiredTableName) {
		return "ora$ptt_" + desiredTableName;
	}

	@Override
	public String getTemporaryTableCreateOptions() {
		return "on commit drop definition";
	}

	@Override
	public String getTemporaryTableCreateCommand() {
		return "create private temporary table";
	}

	@Override
	public boolean supportsTemporaryTablePrimaryKey() {
		return false;
	}

	@Override
	public boolean supportsTemporaryTableNullConstraint() {
		return false;
	}
}
