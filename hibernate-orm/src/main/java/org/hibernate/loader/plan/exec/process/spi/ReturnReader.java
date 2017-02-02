/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.plan.exec.process.spi;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Handles reading a single root Return object
 *
 * @author Steve Ebersole
 */
public interface ReturnReader {
	/**
	 * Essentially performs the second phase of two-phase loading.
	 *
	 * @param resultSet The result set being processed
	 * @param context The context for the processing
	 *
	 * @return The read object
	 *
	 * @throws java.sql.SQLException Indicates a problem access the JDBC result set
	 */
	public Object read(ResultSet resultSet, ResultSetProcessingContext context) throws SQLException;
}
