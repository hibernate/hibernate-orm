/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.engine.jdbc.batch.spi;

import java.util.function.Supplier;

import org.hibernate.engine.jdbc.mutation.group.PreparedStatementGroup;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.service.Service;

/**
 * A builder for {@link Batch} instances.
 * <p>
 * A custom {@code BatchBuilder} may be selected using the configuration property
 * {@value org.hibernate.cfg.AvailableSettings#BATCH_STRATEGY}.
 *
 * @author Steve Ebersole
 */
public interface Batch2Builder extends Service {
	/**
	 * Build a batch.
	 */
	Batch2 buildBatch(
			BatchKey key,
			Integer batchSize,
			Supplier<PreparedStatementGroup> statementGroupSupplier,
			JdbcCoordinator jdbcCoordinator);
}
