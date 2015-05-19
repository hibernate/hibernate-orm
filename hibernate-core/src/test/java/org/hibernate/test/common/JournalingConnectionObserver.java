/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.common;

import java.sql.Connection;

import org.hibernate.engine.jdbc.spi.ConnectionObserver;

/**
 * @author Steve Ebersole
 */
public class JournalingConnectionObserver implements ConnectionObserver {
	private int physicalConnectionObtainedCount = 0;
	private int physicalConnectionReleasedCount = 0;
	private int logicalConnectionClosedCount = 0;
	private int statementPreparedCount = 0;

	@Override
	public void physicalConnectionObtained(Connection connection) {
		physicalConnectionObtainedCount++;
	}

	@Override
	public void physicalConnectionReleased() {
		physicalConnectionReleasedCount++;
	}

	@Override
	public void logicalConnectionClosed() {
		logicalConnectionClosedCount++;
	}

	@Override
	public void statementPrepared() {
		statementPreparedCount++;
	}

	public int getPhysicalConnectionObtainedCount() {
		return physicalConnectionObtainedCount;
	}

	public int getPhysicalConnectionReleasedCount() {
		return physicalConnectionReleasedCount;
	}

	public int getLogicalConnectionClosedCount() {
		return logicalConnectionClosedCount;
	}

	public int getStatementPreparedCount() {
		return statementPreparedCount;
	}
}
