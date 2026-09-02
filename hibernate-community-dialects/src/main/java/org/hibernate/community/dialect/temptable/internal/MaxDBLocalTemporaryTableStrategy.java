/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.temptable.internal;

import org.hibernate.dialect.temptable.spi.StandardLocalTemporaryTableStrategy;
import org.hibernate.query.sqm.mutation.spi.AfterUseAction;

/**
 * MaxDB specific local temporary table strategy.
 *
 * @author Steve Ebersole
 */
public class MaxDBLocalTemporaryTableStrategy extends StandardLocalTemporaryTableStrategy {

	public static final MaxDBLocalTemporaryTableStrategy INSTANCE = new MaxDBLocalTemporaryTableStrategy();

	@Override
	public String adjustTemporaryTableName(String desiredTableName) {
		return "temp." + desiredTableName;
	}

	@Override
	public AfterUseAction getTemporaryTableAfterUseAction() {
		return AfterUseAction.DROP;
	}

	@Override
	public String getTemporaryTableCreateOptions() {
		return "ignore rollback";
	}
}
