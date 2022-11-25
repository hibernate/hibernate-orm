/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.engine.jdbc.mutation.spi;

import java.util.function.Supplier;

import org.hibernate.engine.jdbc.batch.spi.BatchKey;
import org.hibernate.engine.jdbc.mutation.MutationExecutor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.service.Service;
import org.hibernate.sql.model.MutationOperationGroup;

/**
 * Service for creating executors for model mutation operations
 *
 * @author Steve Ebersole
 */
public interface MutationExecutorService extends Service {

	MutationExecutor createExecutor(
			Supplier<BatchKey> batchKeySupplier,
			MutationOperationGroup operationGroup,
			SharedSessionContractImplementor session);
}
