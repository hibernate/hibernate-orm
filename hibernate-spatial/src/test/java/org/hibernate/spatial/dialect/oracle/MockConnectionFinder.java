package org.hibernate.spatial.dialect.oracle;

import java.sql.Connection;

import org.geolatte.geom.codec.db.oracle.ConnectionFinder;


public class MockConnectionFinder implements ConnectionFinder {
	@Override
	public Connection find(Connection subject) {
		return null;
	}
}
