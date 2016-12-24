/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.exec.results.process.spi;

import org.hibernate.loader.plan.spi.EntityFetch;

/**
 * State pertaining to the processing of a single row of a JdbcValuesSource
 *
 * @author Steve Ebersole
 */
public interface RowProcessingState {
	JdbcValuesSourceProcessingState getJdbcValuesSourceProcessingState();

//	boolean next() throws SQLException;
	Object[] getJdbcValues();

	void registerNonExists(EntityFetch fetch);

	void finishRowProcessing();
}
