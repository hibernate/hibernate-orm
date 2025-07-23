/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.temptable;

/**
 * H2 specific global temporary table strategy.
 */
public class H2GlobalTemporaryTableStrategy extends StandardGlobalTemporaryTableStrategy {

	public static final H2GlobalTemporaryTableStrategy INSTANCE = new H2GlobalTemporaryTableStrategy();

	@Override
	public String getTemporaryTableCreateOptions() {
		return "transactional";
	}
}
