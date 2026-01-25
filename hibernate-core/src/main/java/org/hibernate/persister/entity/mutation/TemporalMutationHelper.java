/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import org.hibernate.engine.spi.SharedSessionContractImplementor;

public class TemporalMutationHelper {
	public static boolean isUsingParameters(SharedSessionContractImplementor session) {
		final var options = session.getFactory().getSessionFactoryOptions();
		return !options.isUseServerTransactionTimestampsEnabled()
			&& !options.isUseNativeTemporalTablesEnabled();
	}
}
