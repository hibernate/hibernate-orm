/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import org.hibernate.engine.spi.SharedSessionContractImplementor;

import static org.hibernate.cfg.TemporalTableStrategy.HISTORY_TABLE;
import static org.hibernate.cfg.TemporalTableStrategy.SINGLE_TABLE;

public class TemporalMutationHelper {
	public static boolean isUsingParameters(SharedSessionContractImplementor session) {
		final var options = session.getFactory().getSessionFactoryOptions();
		final var strategy = options.getTemporalTableStrategy();
		return ( strategy == SINGLE_TABLE || strategy == HISTORY_TABLE )
			&& !options.isUseServerTransactionTimestampsEnabled();
	}
}
