/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.spi;
import java.sql.Connection;

/**
 * An observer of logical connection events.
 *
 * @author Steve Ebersole
 */
public interface ConnectionObserver {
	/**
	 * A physical connection was obtained.
	 *
	 * @param connection The physical connection just obtained.
	 */
	public void physicalConnectionObtained(Connection connection);

	/**
	 * A physical connection was released.
	 */
	public void physicalConnectionReleased();

	/**
	 * The logical connection was closed.
	 */
	public void logicalConnectionClosed();

	/**
	 * Notification of a statement being prepared
	 */
	public void statementPrepared();
}
