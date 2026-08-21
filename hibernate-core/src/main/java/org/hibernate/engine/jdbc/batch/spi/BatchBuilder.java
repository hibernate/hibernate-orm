/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.batch.spi;

import java.util.function.Supplier;

import org.hibernate.Incubating;
import org.hibernate.engine.jdbc.mutation.group.PreparedStatementGroup;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.service.JavaServiceLoadable;
import org.hibernate.service.Service;

/**
 * A builder for {@link Batch} instances.
 * <p>
 * A custom {@code BatchBuilder} may be selected either by setting the configuration
 * property {@value org.hibernate.cfg.AvailableSettings#BUILDER}, or by registering it
 * as a {@linkplain java.util.ServiceLoader Java service}.
 *
 * @author Steve Ebersole
 */
@Incubating
@JavaServiceLoadable
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
