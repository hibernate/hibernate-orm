/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.internal;

import java.sql.ResultSet;

import org.hibernate.engine.jdbc.ColumnNameCache;
import org.hibernate.engine.jdbc.ResultSetWrapperProxy;
import org.hibernate.engine.jdbc.spi.ResultSetWrapper;

/**
 * Standard Hibernate implementation for wrapping a {@link ResultSet} in a
 " column name cache" wrapper.
 *
 * @author Steve Ebersole
 * @author Gail Badner
 */
public class ResultSetWrapperImpl implements ResultSetWrapper {
	/**
	 * Singleton access
	 */
	public static final ResultSetWrapper INSTANCE = new ResultSetWrapperImpl();

	private ResultSetWrapperImpl() {
	}

	@Override
	public ResultSet wrap(ResultSet resultSet, ColumnNameCache columnNameCache) {
		return ResultSetWrapperProxy.generateProxy( resultSet, columnNameCache );
	}
}
