/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.connections.internal;

import org.hibernate.internal.log.ConnectionInfoLogger;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;

class PoolState implements Runnable {

	//Protecting any lifecycle state change:
	private final ReadWriteLock statelock = new ReentrantReadWriteLock();
	private volatile boolean active = false;
	private ScheduledExecutorService executorService;

	private final PooledConnections pool;
	private final long validationInterval;

	PoolState(PooledConnections pool, long validationInterval) {
		this.pool = pool;
		this.validationInterval = validationInterval;
	}

	private void startIfNeeded() {
		if ( active ) {
			return;
		}
		statelock.writeLock().lock();
		try {
			if ( active ) {
				return;
			}
			executorService =
					newSingleThreadScheduledExecutor( runnable -> {
						final Thread thread = new Thread( runnable );
						thread.setDaemon( true );
						thread.setName( "Hibernate Connection Pool Validation Thread" );
						return thread;
					} );
			executorService.scheduleWithFixedDelay(
					this,
					validationInterval,
					validationInterval,
					TimeUnit.SECONDS
			);
			active = true;
		}
		finally {
			statelock.writeLock().unlock();
		}
	}

	@Override
	public void run() {
		if ( active ) {
			pool.validate();
		}
	}

	public PooledConnections getPool() {
		return pool;
	}

	void stop() {
		statelock.writeLock().lock();
		try {
			if ( !active ) {
				return;
			}
			ConnectionInfoLogger.INSTANCE.cleaningUpConnectionPool( pool.getUrl() );
			active = false;
			if ( executorService != null ) {
				executorService.shutdown();
			}
			executorService = null;
			try {
				pool.close();
			}
			catch (SQLException e) {
				ConnectionInfoLogger.INSTANCE.unableToDestroyConnectionPool( e );
			}
		}
		finally {
			statelock.writeLock().unlock();
		}
	}

	Connection getConnection() {
		startIfNeeded();
		statelock.readLock().lock();
		try {
			return pool.poll();
		}
		finally {
			statelock.readLock().unlock();
		}
	}

	void closeConnection(Connection conn) {
		if ( conn == null ) {
			return;
		}
		startIfNeeded();
		statelock.readLock().lock();
		try {
			pool.add( conn );
		}
		finally {
			statelock.readLock().unlock();
		}
	}

	void validateConnections(ConnectionValidator validator) {
		if ( !active ) {
			return;
		}
		statelock.writeLock().lock();
		try {
			RuntimeException ex = null;
			for ( var connection : pool.getAllConnections() ) {
				SQLException e = null;
				boolean isValid = false;
				try {
					isValid = validator.isValid( connection );
				}
				catch (SQLException sqlException) {
					e = sqlException;
				}
				if ( !isValid ) {
					pool.closeConnection( connection, e );
					if ( ex == null ) {
						ex = new RuntimeException( e );
					}
					else if ( e != null ) {
						ex.addSuppressed( e );
					}
				}
			}
			if ( ex != null ) {
				throw ex;
			}
		}
		finally {
			statelock.writeLock().unlock();
		}
	}
}
