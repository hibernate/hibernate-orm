/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.mutation.spi;

import org.hibernate.engine.jdbc.batch.spi.BatchKey;

/**
 * Provides access to a {@link BatchKey} as part of creating an
 * {@linkplain MutationExecutorService#createExecutor executor}.
 *
 * @author Steve Ebersole
 */
public interface BatchKeyAccess {
	/**
	 * The BatchKey to use
	 */
	BatchKey getBatchKey();
}
