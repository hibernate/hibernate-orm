/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.exec.spi;

import java.sql.PreparedStatement;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Executor for model-mutation operations
 *
 * @author Steve Ebersole
 */
public interface JdbcMutationExecutor {
	/**
	 * Perform the execution
	 */
	int execute(
			JdbcOperationQueryMutation jdbcMutation,
			JdbcParameterBindings jdbcParameterBindings,
			Function<String, PreparedStatement> statementCreator,
			BiConsumer<Integer, PreparedStatement> expectationCheck,
			ExecutionContext executionContext);
}
