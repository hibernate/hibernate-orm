/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.temptable;

/**
 * DB2 specific global temporary table strategy.
 */
public class DB2GlobalTemporaryTableStrategy extends StandardGlobalTemporaryTableStrategy {

	public static final DB2GlobalTemporaryTableStrategy INSTANCE = new DB2GlobalTemporaryTableStrategy();

	@Override
	public String getTemporaryTableCreateOptions() {
		return "not logged";
	}

	@Override
	public String getTemporaryTableCreateCommand() {
		return "declare global temporary table";
	}
}
