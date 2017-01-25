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
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.spi.ScrollableResultsImplementor;
import org.hibernate.sql.convert.spi.Callback;

/**
 * An executor for JdbcSelect operations.
 *
 * @author Steve Ebersole
 */
@Incubating
public interface JdbcSelectExecutor {

	// todo : need to pass some form of JdbcValuesSourceProcessingOptions to list to be able to have it handle single entity loads -
	//		or just drop the form of loading an entity by passing an instance of itself as the one to load

	<R> List<R> list(
			JdbcSelect jdbcSelect,
			QueryOptions queryOptions,
			QueryParameterBindings queryParameterBindings,
			RowTransformer<R> rowTransformer,
			Callback callback,
			SharedSessionContractImplementor persistenceContext);

	<R> ScrollableResultsImplementor<R> scroll(
			JdbcSelect jdbcSelect,
			ScrollMode scrollMode,
			QueryOptions queryOptions,
			QueryParameterBindings queryParameterBindings,
			RowTransformer<R> rowTransformer,
			Callback callback,
			SharedSessionContractImplementor persistenceContext);

	<R> Stream<R> stream(
			JdbcSelect jdbcSelect,
			QueryOptions queryOptions,
			QueryParameterBindings queryParameterBindings,
			RowTransformer<R> rowTransformer,
			Callback callback,
			SharedSessionContractImplementor persistenceContext);
}
