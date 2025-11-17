/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.usertype;
import org.hibernate.engine.spi.SessionFactoryImplementor;

/**
 * Marker interface for user types which want to perform custom
 * logging of their corresponding values
 *
 * @author Steve Ebersole
 */
public interface LoggableUserType {
	/**
	 * Generate a loggable string representation of the collection (value).
	 *
	 * @param value The collection to be logged; guaranteed to be non-null and initialized.
	 * @param factory The factory.
	 * @return The loggable string representation.
	 */
	String toLoggableString(Object value, SessionFactoryImplementor factory);
}
