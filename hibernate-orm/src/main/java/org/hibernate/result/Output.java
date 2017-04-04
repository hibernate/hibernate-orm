/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.result;

/**
 * Common contract for individual return objects which can be either results ({@link ResultSetOutput}) or update
 * counts ({@link UpdateCountOutput}).
 *
 * @author Steve Ebersole
 */
public interface Output {
	/**
	 * Determine if this return is a result (castable to {@link ResultSetOutput}).  The alternative is that it is
	 * an update count (castable to {@link UpdateCountOutput}).
	 *
	 * @return {@code true} indicates that {@code this} can be safely cast to {@link ResultSetOutput}), other wise
	 * it can be cast to {@link UpdateCountOutput}.
	 */
	public boolean isResultSet();
}
