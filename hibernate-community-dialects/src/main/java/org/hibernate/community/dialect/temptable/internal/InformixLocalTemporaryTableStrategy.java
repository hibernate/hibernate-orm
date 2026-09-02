/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.temptable.internal;

import org.hibernate.dialect.temptable.spi.StandardLocalTemporaryTableStrategy;

/**
 * Informix specific local temporary table strategy.
 *
 * @author Steve Ebersole
 */
public class InformixLocalTemporaryTableStrategy extends StandardLocalTemporaryTableStrategy {

	public static final InformixLocalTemporaryTableStrategy INSTANCE = new InformixLocalTemporaryTableStrategy();

	@Override
	public String getTemporaryTableCreateOptions() {
		return "with no log";
	}

	@Override
	public String getTemporaryTableCreateCommand() {
		return "create temp table";
	}

}
