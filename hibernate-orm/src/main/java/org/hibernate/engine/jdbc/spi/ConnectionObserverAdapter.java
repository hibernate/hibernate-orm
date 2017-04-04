/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.spi;

import java.sql.Connection;

/**
 * A no-op adapter for ConnectionObserver.
 *
 * @author Steve Ebersole
 */
public class ConnectionObserverAdapter implements ConnectionObserver {
	@Override
	public void physicalConnectionObtained(Connection connection) {
	}

	@Override
	public void physicalConnectionReleased() {
	}

	@Override
	public void logicalConnectionClosed() {
	}

	@Override
	public void statementPrepared() {
	}
}
