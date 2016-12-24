/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.exec.spi;

import java.sql.SQLException;

import org.hibernate.Incubating;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.spi.ExecutionContext;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.result.Outputs;
import org.hibernate.sql.convert.spi.Callback;
import org.hibernate.sql.convert.spi.SqmSelectInterpretation;

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
			SqmSelectInterpretation sqmSelectInterpretation,
			PreparedStatementCreator statementCreator,
			PreparedStatementExecutor preparedStatementExecutor,
			QueryOptions queryOptions,
			QueryParameterBindings queryParameterBindings,
			RowTransformer<T> rowTransformer,
			Callback callback,
			SharedSessionContractImplementor persistenceContext,
			ExecutionContext executionContext) throws SQLException;

	Object[] executeInsert(
			Object sqlTree,
			PreparedStatementCreator statementCreator,
			QueryOptions queryOptions,
			QueryParameterBindings queryParameterBindings,
			SharedSessionContractImplementor persistenceContext,
			ExecutionContext executionContext);

	int executeUpdate(
			Object sqlTree,
			PreparedStatementCreator statementCreator,
			QueryOptions queryOptions,
			QueryParameterBindings queryParameterBindings,
			SharedSessionContractImplementor persistenceContext,
			ExecutionContext executionContext);

	int executeDelete(
			Object sqlTree,
			PreparedStatementCreator statementCreator,
			QueryOptions queryOptions,
			QueryParameterBindings queryParameterBindings,
			SharedSessionContractImplementor persistenceContext,
			ExecutionContext executionContext);

	<T> Outputs executeCall(
			String callableName,
			QueryOptions queryOptions,
			QueryParameterBindings queryParameterBindings,
			RowTransformer<T> rowTransformer,
			Callback callback,
			SharedSessionContractImplementor persistenceContext,
			ExecutionContext executionContext);
}
