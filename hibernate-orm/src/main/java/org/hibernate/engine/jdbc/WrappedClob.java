/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc;

import java.sql.Clob;

/**
 * Contract for {@link Clob} wrappers.
 *
 * @author Steve Ebersole
 */
public interface WrappedClob {
	/**
	 * Retrieve the wrapped {@link java.sql.Blob} reference
	 *
	 * @return The wrapped {@link java.sql.Blob} reference
	 */
	public Clob getWrappedClob();
}
