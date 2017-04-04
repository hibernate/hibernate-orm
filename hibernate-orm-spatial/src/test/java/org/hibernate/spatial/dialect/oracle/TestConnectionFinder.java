/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial.dialect.oracle;

import java.sql.Connection;

import org.geolatte.geom.codec.db.oracle.ConnectionFinder;
import org.geolatte.geom.codec.db.oracle.DefaultConnectionFinder;

/**
 * Created by Karel Maesen, Geovise BVBA on 20/02/16.
 */
public class TestConnectionFinder implements ConnectionFinder {


	final private ConnectionFinder internal = new DefaultConnectionFinder();

	/**
	 * Find an instance of Connection that can be cast to an {@code OracleConnection} instance.
	 *
	 * @param conn the object that is being searched for an OracleConnection
	 *
	 * @return the object sought
	 *
	 * @throws RuntimeException thrown when the feature can be found;
	 */
	@Override
	public Connection find(Connection conn) {
		return internal.find( conn );
	}

}
