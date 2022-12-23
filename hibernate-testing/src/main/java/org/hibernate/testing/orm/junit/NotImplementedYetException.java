/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.orm.junit;

/**
 * Marker interface for exceptions that indicate that something hasn't been implemented yet for a certain version
 *
 * @author Jan Schatteman
 *
 * @deprecated By definition, something "not yet implemented" is something we are actively seeking to remove
 */
@SuppressWarnings("DeprecatedIsStillUsed")
@Deprecated(forRemoval = true)
public interface NotImplementedYetException {
}
