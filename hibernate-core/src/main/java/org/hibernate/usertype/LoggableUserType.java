/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
	 * @param value The collection to be logged; guarenteed to be non-null and initialized.
	 * @param factory The factory.
	 * @return The loggable string representation.
	 */
	public String toLoggableString(Object value, SessionFactoryImplementor factory);
}
