/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
