/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.spi;

import java.util.List;

import org.hibernate.engine.spi.EntityKey;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;
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
	 * The individual JavaType for each DomainResult
	 */
	List<@Nullable JavaType<?>> getResultJavaTypes();

	int getInitializerCount();

	/**
	 * Called before reading the first row.
	 */
	void startLoading(RowProcessingState processingState);

	/**
	 * The actual coordination of reading a row
	 */
	R readRow(RowProcessingState processingState);

	/**
	 * Called at the end of processing all rows
	 */
	void finishUp(RowProcessingState processingState);

	@Nullable EntityKey resolveSingleResultEntityKey(RowProcessingState rowProcessingState);

	boolean hasCollectionInitializers();

}
