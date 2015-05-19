/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial.dialect.oracle;

import java.sql.Connection;

import org.geolatte.geom.codec.db.oracle.ConnectionFinder;


public class MockConnectionFinder implements ConnectionFinder {
	@Override
	public Connection find(Connection subject) {
		return null;
	}
}
