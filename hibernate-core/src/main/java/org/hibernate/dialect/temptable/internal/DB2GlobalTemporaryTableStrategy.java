/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.temptable.internal;

import org.hibernate.dialect.temptable.spi.StandardGlobalTemporaryTableStrategy;

/**
 * DB2 specific global temporary table strategy.
 *
 * @author Steve Ebersole
 */
public class DB2GlobalTemporaryTableStrategy extends StandardGlobalTemporaryTableStrategy {

	public static final DB2GlobalTemporaryTableStrategy INSTANCE = new DB2GlobalTemporaryTableStrategy();

	@Override
	public String getTemporaryTableCreateOptions() {
		return "not logged";
	}

	@Override
	public boolean supportsTemporaryTablePrimaryKey() {
		return false;
	}
}
