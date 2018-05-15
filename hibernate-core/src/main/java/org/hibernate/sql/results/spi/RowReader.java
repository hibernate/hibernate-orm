/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.spi;

import java.sql.SQLException;
import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.named.spi.RowReaderMemento;

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

	List<Initializer> getInitializers();

	/**
	 * How many results (domain selections) are returned by this reader?
	 *
	 * @apiNote If this method returns `> 1` then {@link #getResultJavaType()}
	 * should return either `Object[].class` (or {@link javax.persistence.Tuple}?).
	 *
	 * todo (6.0) : determine this ^^
	 */
	int getNumberOfResults();

	/**
	 * The actual coordination of reading a row
	 *
	 * todo (6.0) : JdbcValuesSourceProcessingOptions is available through RowProcessingState - why pass it in separately
	 * 		should use one approach or the other
	 */
	R readRow(RowProcessingState processingState, JdbcValuesSourceProcessingOptions options) throws SQLException;

	/**
	 * Called at the end of processing all rows
	 */
	void finishUp(JdbcValuesSourceProcessingState context);

	/**
	 * Create a memento capable of being used as a named result set mapping
	 */
	RowReaderMemento toMemento(SessionFactoryImplementor factory);
}
