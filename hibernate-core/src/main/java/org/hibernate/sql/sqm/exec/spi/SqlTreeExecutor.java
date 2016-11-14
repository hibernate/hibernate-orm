/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.sqm.exec.spi;

import org.hibernate.Incubating;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.result.Outputs;
import org.hibernate.sql.sqm.ast.SelectQuery;
import org.hibernate.sql.sqm.convert.spi.Callback;

/**
 * NOTE : look at as the clean room representation of "Loader", although possibly for DML as
 * well (in SQM/SQL-AST form).
 *
 * At the end of the day (procedure/function calls aside) we either:<ul>
 *     <li>
 *         Perform a SELECT query and "process" results, which requires:<ul>
 *             <li>SQL AST (SQL, ParameterBinders, Return descriptors)</li>
 *             <li>StatementPreparer</li>
 *             <li>ExecutionOptions</li>
 *             <li>QueryParameterBindings</li>
 *             <li>ResultSetConsumer</li>
 *             <li>RowTransformer</li>
 *         </ul>
 *     </li>
 *     <li>
 *         Perform an UPDATE/INSERT/DELETE and determine affected-row count, which requires:<ul>
 *             <li>SQL AST (SQL, ParameterBinders)</li>
 *             <li>StatementPreparer</li>
 *             <li>ExecutionOptions</li>
 *             <li>QueryParameterBindings</li>
 *             <li>?generated-keys?</li>
 *         </ul>
 *     </li>
 * </ul>
 *
 * Actually, I think that with PreparedStatementExecutor we can still handle
 * ProcedureCall here.
 *
 *
 * @author Steve Ebersole
 */
@Incubating
public interface SqlTreeExecutor {
	// todo : ExecutionOptions versus PreparedStatementConfigurer?

	<R,T> R executeSelect(
			SelectQuery sqlTree,
			PreparedStatementCreator statementCreator,
			PreparedStatementExecutor<R, T> preparedStatementExecutor,
			QueryOptions queryOptions,
			QueryParameterBindings queryParameterBindings,
			RowTransformer<T> rowTransformer,
			Callback callback,
			SharedSessionContractImplementor session);

	Object[] executeInsert(
			Object sqlTree,
			PreparedStatementCreator statementCreator,
			QueryOptions queryOptions,
			QueryParameterBindings queryParameterBindings,
			SharedSessionContractImplementor session);

	int executeUpdate(
			Object sqlTree,
			PreparedStatementCreator statementCreator,
			QueryOptions queryOptions,
			QueryParameterBindings queryParameterBindings,
			SharedSessionContractImplementor session);

	int executeDelete(
			Object sqlTree,
			PreparedStatementCreator statementCreator,
			QueryOptions queryOptions,
			QueryParameterBindings queryParameterBindings,
			SharedSessionContractImplementor session);

	<T> Outputs executeCall(
			String callableName,
			QueryOptions queryOptions,
			QueryParameterBindings queryParameterBindings,
			RowTransformer<T> rowTransformer,
			Callback callback,
			SharedSessionContractImplementor session);
}
