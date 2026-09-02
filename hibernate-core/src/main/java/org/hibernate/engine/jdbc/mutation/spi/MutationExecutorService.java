/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.mutation.spi;

import org.hibernate.SPI;
import org.hibernate.engine.jdbc.mutation.MutationExecutor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.service.JavaServiceLoadable;
import org.hibernate.service.Service;
import org.hibernate.sql.model.MutationOperationGroup;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;

/**
 * Service for creating executors for model mutation operations.
 * <p>
 * A custom {@code MutationExecutorService} may be selected either by setting the
 * configuration property
 * {@value org.hibernate.engine.jdbc.mutation.internal.MutationExecutorServiceInitiator#EXECUTOR_KEY},
 * or by registering it as a {@linkplain java.util.ServiceLoader Java service}.
 *
 * @author Steve Ebersole
 *
 * @deprecated This service executes the statement groups used only by the legacy
 * action queue. It is unused when the graph-based action queue is selected.
 */
@Deprecated(since = "8.0", forRemoval = true)
@SPI({ IMPLEMENT, SUPPLY })
@JavaServiceLoadable
public interface MutationExecutorService extends Service {

	/**
	 * Create an executor for the given {@code operationGroup}, potentially using batching
	 */
	MutationExecutor createExecutor(
			BatchKeyAccess batchKeySupplier,
			MutationOperationGroup operationGroup,
			SharedSessionContractImplementor session);


	/**
	 * Create an executor for the given {@code operationGroup}, potentially using batching.
	 */
	MutationExecutor createExecutor(
			BatchKeyAccess batchKeySupplier,
			MutationOperationGroup operationGroup,
			JdbcValueBindingsFactory bindingsFactory,
			SharedSessionContractImplementor session
	);
}
