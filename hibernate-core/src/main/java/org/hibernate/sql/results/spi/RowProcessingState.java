/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.spi;

import org.hibernate.query.NavigablePath;
import org.hibernate.sql.exec.spi.ExecutionContext;

/**
 * State pertaining to the processing of a single row of a JdbcValuesSource
 *
 * @author Steve Ebersole
 */
public interface RowProcessingState extends ExecutionContext {
	/**
	 * Access to the "parent state" related to the overall processing
	 * of the results.
	 */
	JdbcValuesSourceProcessingState getJdbcValuesSourceProcessingState();

	void finishRowProcessing();

	Initializer resolveInitializer(NavigablePath path);
}
