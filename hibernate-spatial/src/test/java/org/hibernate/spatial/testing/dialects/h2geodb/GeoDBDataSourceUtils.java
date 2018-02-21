/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

/**
 *
 */
package org.hibernate.spatial.testing.dialects.h2geodb;


import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;

import org.hibernate.spatial.testing.DataSourceUtils;
import org.hibernate.spatial.testing.SQLExpressionTemplate;

/**
 * Extension of the {@link DataSourceUtils} class which sets up an in-memory
 * GeoDB database. The specified SQL file is used to generate a schema in the
 * database.
 *
 * @author Jan Boonen, Geodan IT b.v.
 */
public class GeoDBDataSourceUtils extends DataSourceUtils {

	public GeoDBDataSourceUtils(
			String jdbcDriver, String jdbcUrl, String jdbcUser, String jdbcPass,
			SQLExpressionTemplate sqlExpressionTemplate)
			throws SQLException, IOException {
		super( jdbcDriver, jdbcUrl, jdbcUser, jdbcPass, sqlExpressionTemplate );
		Connection conn = this.getConnection();
		initGeoDB( conn );
	}

	//initialise the GeoDB connection using Reflection
	// so we do not introduce a compile-time dependency
	private void initGeoDB(Connection conn) {
		String errorMsg = "Problem initializing GeoDB.";
		try {
			Class<?> geoDB = Thread.currentThread().getContextClassLoader().loadClass( "geodb.GeoDB" );
			Method m = geoDB.getDeclaredMethod( "InitGeoDB", new Class[] { Connection.class } );
			m.invoke( null, conn );
		}
		catch (ClassNotFoundException e) {
			throw new RuntimeException( errorMsg, e );
		}
		catch (NoSuchMethodException e) {
			throw new RuntimeException( errorMsg, e );
		}
		catch (InvocationTargetException e) {
			throw new RuntimeException( errorMsg, e );
		}
		catch (IllegalAccessException e) {
			throw new RuntimeException( errorMsg, e );
		}
	}
}
