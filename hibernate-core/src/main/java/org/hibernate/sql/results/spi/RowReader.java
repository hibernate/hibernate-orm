/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.spi;

import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingState;
import org.hibernate.query.named.RowReaderMemento;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * Coordinates the process of reading a single result values row
 *
 * @author Steve Ebersole
 */
public interface RowReader<R> {
	/**
	 * The overall row result Java type.  Might be a scalar type, an
	 * entity type, etc.  Might also be a `Object[].class` for multiple
	 * results (domain selections).
	 */
	Class<R> getResultJavaType();

	/**
	 *  The JavaTypeDescriptors of the result
	 */
	List<JavaType> getResultJavaTypeDescriptors();

	/**
	 * The initializers associated with this reader
	 */
	List<Initializer> getInitializers();

	/**
	 * The actual coordination of reading a row
	 *
	 * todo (6.0) : JdbcValuesSourceProcessingOptions is available through RowProcessingState - why pass it in separately
	 * 		should use one approach or the other
	 */
	R readRow(RowProcessingState processingState, JdbcValuesSourceProcessingOptions options);

	/**
	 * Called at the end of processing all rows
	 */
	void finishUp(JdbcValuesSourceProcessingState context);

	RowReaderMemento toMemento(SessionFactoryImplementor factory);
}
