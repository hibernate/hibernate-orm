/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.batch.spi;

import java.util.function.Supplier;

import org.hibernate.Incubating;
import org.hibernate.engine.jdbc.mutation.group.PreparedStatementGroup;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.service.Service;

/**
 * A builder for {@link Batch} instances.
 * <p>
 * A custom {@code BatchBuilder} may be selected using the configuration property
 * {@value org.hibernate.cfg.AvailableSettings#BUILDER}.
 *
 * @author Steve Ebersole
 */
@Incubating
public interface BatchBuilder extends Service {
	/**
	 * Build a batch.
	 */
	Batch buildBatch(
			BatchKey key,
			Integer batchSize,
			Supplier<PreparedStatementGroup> statementGroupSupplier,
			JdbcCoordinator jdbcCoordinator);
}
