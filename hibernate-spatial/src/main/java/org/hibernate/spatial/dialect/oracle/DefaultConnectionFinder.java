/**
 * $Id: DefaultConnectionFinder.java 286 2011-02-04 21:16:23Z maesenka $
 *
 * This file is part of Hibernate Spatial, an extension to the 
 * hibernate ORM solution for geographic data. 
 *
 * Copyright © 2007 Geovise BVBA
 * Copyright © 2007 K.U. Leuven LRD, Spatial Applications Division, Belgium
 *
 * This work was partially supported by the European Commission, 
 * under the 6th Framework Programme, contract IST-2-004688-STP.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * For more information, visit: http://www.hibernatespatial.org/
 */
package org.hibernate.spatial.dialect.oracle;


import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;

import org.hibernate.HibernateException;
import org.hibernate.spatial.helper.FinderException;

/**
 * Default <code>ConnectionFinder</code> implementation.
 * <p>
 * This implementation attempts to retrieve the <code>OracleConnection</code>
 * by recursive reflection: it searches for methods that return
 * <code>Connection</code> objects, executes these methods and checks the
 * result. If the result is of type <code>OracleConnection</code> the object
 * is returned, otherwise it recurses on it.
 * <p/>
 * </p>
 *
 * @author Karel Maesen
 */
public class DefaultConnectionFinder implements ConnectionFinder {

	private final static Class<?> oracleConnectionClass;

	static {
		try {
			oracleConnectionClass = Class.forName( "oracle.jdbc.driver.OracleConnection" );
		}
		catch ( ClassNotFoundException e ) {
			throw new HibernateException( "Can't find Oracle JDBC Driver on classpath." );
		}
	}

	public Connection find(Connection con) throws FinderException {
		if ( con == null ) {
			return null;
		}

		if ( oracleConnectionClass.isInstance( con ) ) {
			return con;
		}
		// try to find the Oracleconnection recursively
		for ( Method method : con.getClass().getMethods() ) {
			if ( method.getReturnType().isAssignableFrom(
					java.sql.Connection.class
			)
					&& method.getParameterTypes().length == 0 ) {

				try {
					method.setAccessible( true );
					Connection oc = find( (Connection) ( method.invoke( con, new Object[] { } ) ) );
					if ( oc == null ) {
						throw new FinderException(
								String.format(
										"Tried retrieving OracleConnection from %s using method %s, but received null.",
										con.getClass().getCanonicalName(),
										method.getName()
								)
						);
					}
					return oc;
				}
				catch ( IllegalAccessException e ) {
					throw new FinderException(
							String.format(
									"Illegal access on executing method %s when finding OracleConnection",
									method.getName()
							)
					);
				}
				catch ( InvocationTargetException e ) {
					throw new FinderException(
							String.format(
									"Invocation exception on executing method %s when finding OracleConnection",
									method.getName()
							)
					);
				}


			}
		}
		throw new FinderException(
				"Couldn't get at the OracleSpatial Connection object from the PreparedStatement."
		);
	}

}
