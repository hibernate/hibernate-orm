/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.engine.jdbc.spi;

import org.hibernate.ConnectionReleaseMode;

/**
 * The "internal" contract for LogicalConnection
 *
 * @author Steve Ebersole
 */
public interface LogicalConnectionImplementor extends LogicalConnection {
	/**
	 * Obtains the JDBC services associated with this logical connection.
	 *
	 * @return JDBC services
	 */
	public JdbcServices getJdbcServices();

	/**
	 * Obtains the JDBC resource registry associated with this logical connection.
	 *
	 * @return The JDBC resource registry.
	 */
	public JdbcResourceRegistry getResourceRegistry();

	/**
	 * Add an observer interested in notification of connection events.
	 *
	 * @param observer The observer.
	 */
	public void addObserver(ConnectionObserver observer);

	/**
	 * The release mode under which this logical connection is operating.
	 *
	 * @return the release mode.
	 */
	public ConnectionReleaseMode getConnectionReleaseMode();

	/**
	 * Used to signify that a statement has completed execution which may
	 * indicate that this logical connection need to perform an
	 * aggressive release of its physical connection.
	 */
	public void afterStatementExecution();

	/**
	 * Used to signify that a transaction has completed which may indicate
	 * that this logical connection need to perform an aggressive release
	 * of its physical connection.
	 */
	public void afterTransaction();

	/**
	 * Manually (and temporarily) circumvent aggressive release processing.
	 */
	public void disableReleases();

	/**
	 * Re-enable aggressive release processing (after a prior {@link #disableReleases()} call.
	 */
	public void enableReleases();
}
