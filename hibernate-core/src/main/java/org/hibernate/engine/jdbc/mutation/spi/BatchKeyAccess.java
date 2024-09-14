/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.mutation.spi;

import org.hibernate.engine.jdbc.batch.spi.BatchKey;

/**
 * Provides access to a BatchKey as part of creating an {@linkplain MutationExecutorService#createExecutor executor}.
 *
 * @author Steve Ebersole
 */
public interface BatchKeyAccess {
	/**
	 * The BatchKey to use
	 */
	BatchKey getBatchKey();
}
