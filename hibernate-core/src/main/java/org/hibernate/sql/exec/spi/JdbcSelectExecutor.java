/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.exec.spi;

import java.util.List;
import java.util.stream.Stream;

import org.hibernate.Incubating;
import org.hibernate.ScrollMode;
import org.hibernate.query.spi.ScrollableResultsImplementor;

/**
 * An executor for JdbcSelect operations.
 *
 * @author Steve Ebersole
 */
@Incubating
public interface JdbcSelectExecutor {

	// todo (6.0) : need to pass some form of JdbcValuesSourceProcessingOptions to list to be able to have it handle single entity loads -
	//		or just drop the form of loading an entity by passing an instance of itself as the one to load

	// todo (6.0) : Ideally we'd have a singular place (JdbcServices? ServiceRegistry?) to obtain these executors

	<R> List<R> list(
			JdbcSelect jdbcSelect,
			ExecutionContext executionContext,
			RowTransformer<R> rowTransformer);

	<R> ScrollableResultsImplementor<R> scroll(
			JdbcSelect jdbcSelect,
			ScrollMode scrollMode,
			ExecutionContext executionContext,
			RowTransformer<R> rowTransformer);

	<R> Stream<R> stream(
			JdbcSelect jdbcSelect,
			ExecutionContext executionContext,
			RowTransformer<R> rowTransformer);
}
