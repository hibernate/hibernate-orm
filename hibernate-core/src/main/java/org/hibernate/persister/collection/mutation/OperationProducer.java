/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
