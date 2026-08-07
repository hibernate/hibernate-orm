/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.temptable;

/**
 * DB2 z/OS specific global temporary table strategy.
 */
public class DB2zGlobalTemporaryTableStrategy extends StandardGlobalTemporaryTableStrategy {

	public static final DB2zGlobalTemporaryTableStrategy INSTANCE = new DB2zGlobalTemporaryTableStrategy();

	@Override
	public boolean supportsTemporaryTablePrimaryKey() {
		return false;
	}
}
