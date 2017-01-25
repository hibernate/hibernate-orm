/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor.spi;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Contract for extracting value via JDBC (from {@link ResultSet} or as output param from {@link CallableStatement}).
 *
 * @author Steve Ebersole
 */
public interface ValueExtractor<X> {
	/**
	 * Extract value from result set
	 *
	 * @param rs The result set from which to extract the value
	 * @param position The position of the value to extract.
	 * @param options The options
	 *
	 * @return The extracted value
	 *
	 * @throws SQLException Exceptions from the underlying JDBC objects are simply re-thrown.
	 */
	X extract(ResultSet rs, int position, WrapperOptions options) throws SQLException;

	X extract(CallableStatement statement, int index, WrapperOptions options) throws SQLException;

	X extract(CallableStatement statement, String name, WrapperOptions options) throws SQLException;
}
