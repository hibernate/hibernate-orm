/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.relational;

/**
 * Marker for things which can be logged.
 *
 * @author Steve Ebersole
 *
 * @deprecated (since 6.0) Use {@link org.hibernate.internal.util.Loggable}
 * instead
 */
@Deprecated
public interface Loggable extends org.hibernate.internal.util.Loggable {
	/**
	 * Delegates to {@link #toLoggableFragment()}
	 *
	 * @return The loggable representation
	 *
	 * @apiNote Use {@link #toLoggableFragment()} instead
	 * @deprecated (since 6.0) Use/implement {@link #toLoggableFragment()} instead
	 */
	@Deprecated
	String toLoggableString();

	@Override
	default String toLoggableFragment() {
		return toLoggableString();
	}
}
