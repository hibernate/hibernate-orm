/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.spi;

import java.util.List;

import org.hibernate.engine.spi.EntityKey;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.internal.InitializersList;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingState;
import org.hibernate.type.descriptor.java.JavaType;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Coordinates the process of reading a single result values row
 *
 * @author Steve Ebersole
 */
public interface RowReader<R> {
	/**
	 * The type actually returned from this reader's {@link #readRow} call,
	 * accounting for any transformers.
	 * <p>
	 * May be null to indicate that no transformation is applied.
	 * <p>
	 * Ultimately intended for use in comparing values are being de-duplicated
	 */
	Class<R> getDomainResultResultJavaType();

	/**
	 * The row result Java type, before any transformations.
	 *
	 * @apiNote along with {@link #getResultJavaTypes()}, describes the "raw"
	 * values as determined from the {@link org.hibernate.sql.results.graph.DomainResult}
	 * references associated with the JdbcValues being processed
	 */
	Class<?> getResultJavaType();

	/**
	 * The individual JavaType for each DomainResult
	 */
	List<JavaType<?>> getResultJavaTypes();

	/**
	 * The initializers associated with this reader.
	 *
	 * @see org.hibernate.sql.results.graph.DomainResult
	 * @deprecated Not needed anymore
	 */
	@Deprecated(forRemoval = true)
	List<Initializer> getInitializers();

	/**
	 * Called before reading the first row.
	 */
	void startLoading(RowProcessingState processingState);

	/**
	 * The actual coordination of reading a row
	 */
	R readRow(RowProcessingState processingState, JdbcValuesSourceProcessingOptions options);

	/**
	 * Called at the end of processing all rows
	 */
	void finishUp(JdbcValuesSourceProcessingState context);

	/**
	 * The initializers associated with this reader.
	 *
	 * @see org.hibernate.sql.results.graph.DomainResult
	 * @deprecated Not needed anymore. Also, was exposing internal type
	 */
	@Deprecated(forRemoval = true)
	InitializersList getInitializersList();

	@Nullable EntityKey resolveSingleResultEntityKey(RowProcessingState rowProcessingState);

	boolean hasCollectionInitializers();

}
