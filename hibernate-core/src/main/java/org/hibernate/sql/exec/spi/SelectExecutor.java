/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.sql.exec.spi;

import java.sql.PreparedStatement;
import java.util.function.Function;

import org.hibernate.sql.results.spi.ResultsConsumer;
import org.hibernate.sql.results.spi.RowTransformer;

/**
 * Generalized selection execution contract.  Generalized to help support
 * non-JDBC use cases
 *
 * @author Steve Ebersole
 */
public interface SelectExecutor {
	<T, R> T executeQuery(
			JdbcSelect jdbcSelect,
			JdbcParameterBindings jdbcParameterBindings,
			ExecutionContext executionContext,
			RowTransformer<R> rowTransformer,
			Class<R> domainResultType,
			Function<String, PreparedStatement> statementCreator,
			ResultsConsumer<T, R> resultsConsumer);
}
