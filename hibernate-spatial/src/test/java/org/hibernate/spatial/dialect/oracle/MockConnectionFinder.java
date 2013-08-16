package org.hibernate.spatial.dialect.oracle;

import java.sql.Connection;

import org.hibernate.spatial.helper.FinderException;

public class MockConnectionFinder implements ConnectionFinder {
	@Override
	public Connection find(Connection subject) throws FinderException {
		return null;
	}
}
