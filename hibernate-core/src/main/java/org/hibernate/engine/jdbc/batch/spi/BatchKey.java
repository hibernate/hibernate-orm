/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.batch.spi;

/**
 * Unique key for batch identification.
 *
 * @author Steve Ebersole
 */
public interface BatchKey {
	default String toLoggableString() {
		return toString();
	}
}
