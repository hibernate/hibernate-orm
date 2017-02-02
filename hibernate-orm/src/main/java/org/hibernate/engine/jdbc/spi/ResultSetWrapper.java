/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.spi;

import java.sql.ResultSet;

import org.hibernate.engine.jdbc.ColumnNameCache;

/**
 * Contract for wrapping a {@link ResultSet} in a "column name cache" wrapper.
 *
 * @author Gail Badner
 */
public interface ResultSetWrapper {
	/**
	 * Wrap a result set in a "column name cache" wrapper.
	 *
	 * @param resultSet The result set to wrap
	 * @param columnNameCache The column name cache.
	 *
	 * @return The wrapped result set.
	 */
	public ResultSet wrap(ResultSet resultSet, ColumnNameCache columnNameCache);
}
