/*
 * This file is part of Hibernate Spatial, an extension to the
 *  hibernate ORM solution for spatial (geographic) data.
 *
 *  Copyright © 2007-2012 Geovise BVBA
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
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

	public GeoDBDataSourceUtils(String jdbcDriver, String jdbcUrl, String jdbcUser, String jdbcPass,
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
		catch ( ClassNotFoundException e ) {
			throw new RuntimeException( errorMsg, e );
		}
		catch ( NoSuchMethodException e ) {
			throw new RuntimeException( errorMsg, e );
		}
		catch ( InvocationTargetException e ) {
			throw new RuntimeException( errorMsg, e );
		}
		catch ( IllegalAccessException e ) {
			throw new RuntimeException( errorMsg, e );
		}
	}
}
