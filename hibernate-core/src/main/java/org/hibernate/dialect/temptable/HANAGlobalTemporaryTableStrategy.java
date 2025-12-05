/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.temptable;

/**
 * HANA specific global temporary table strategy.
 */
public class HANAGlobalTemporaryTableStrategy extends StandardGlobalTemporaryTableStrategy {

	@Override
	public String getTemporaryTableCreateCommand() {
		return "create global temporary row table";
	}

	@Override
	public String getTemporaryTableTruncateCommand() {
		return "truncate table";
	}
}
