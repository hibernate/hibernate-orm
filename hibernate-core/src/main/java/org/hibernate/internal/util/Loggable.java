/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.internal.util;

/**
 * Contract for things that expose enhanced logging capabilities
 * for Hibernate to use
 *
 * @author Steve Ebersole
 */
public interface Loggable {
	/**
	 * Obtain the string representation of this value usable within log statements.
	 * Typically this means it is used when this loggable contract is used as
	 * a "fragment" within a larger log message.
	 *
	 * @return The loggable representation
	 */
	String toLoggableFragment();


}
