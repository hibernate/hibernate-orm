/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.collection.mutation;

import org.hibernate.sql.model.ast.MutatingTableReference;
import org.hibernate.sql.model.jdbc.JdbcMutationOperation;

/**
 * Callback for producing a {@link JdbcMutationOperation} given
 * a collection-table reference
 *
 * @see RowMutationOperations
 * @see UpdateRowsCoordinator
 * @see RowMutationOperations
 *
 * @author Steve Ebersole
 */
@FunctionalInterface
public interface OperationProducer {
	JdbcMutationOperation createOperation(MutatingTableReference tableReference);
}
