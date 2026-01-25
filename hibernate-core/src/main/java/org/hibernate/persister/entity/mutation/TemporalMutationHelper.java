/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import org.hibernate.engine.spi.SharedSessionContractImplementor;

import static org.hibernate.cfg.TemporalTableStrategy.VM_TIMESTAMP;

public class TemporalMutationHelper {
	public static boolean isUsingParameters(SharedSessionContractImplementor session) {
		return session.getFactory().getSessionFactoryOptions()
					.getTemporalTableStrategy() == VM_TIMESTAMP;
	}
}
