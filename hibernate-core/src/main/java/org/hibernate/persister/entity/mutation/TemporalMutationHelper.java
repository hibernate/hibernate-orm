/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import org.hibernate.engine.spi.SharedSessionContractImplementor;

import static org.hibernate.cfg.TemporalTableStrategy.NATIVE;

public class TemporalMutationHelper {
	public static boolean isUsingParameters(SharedSessionContractImplementor session) {
		final var factory = session.getFactory();
		return factory.getSessionFactoryOptions().getTemporalTableStrategy() != NATIVE
			&& !factory.getTransactionIdentifierService().isDisabled();
	}
}
