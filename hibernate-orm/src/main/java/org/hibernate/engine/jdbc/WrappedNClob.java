/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc;

import java.sql.NClob;

/**
 * Contract for {@link NClob} wrappers.
 *
 * @author Steve Ebersole
 */
public interface WrappedNClob extends WrappedClob {
	/**
	 * {@inheritDoc}
	 *
	 * @deprecated Use {@link #getWrappedNClob()} instead
	 */
	@Override
	@Deprecated
	public NClob getWrappedClob();

	/**
	 * Retrieve the wrapped {@link java.sql.Blob} reference
	 *
	 * @return The wrapped {@link java.sql.Blob} reference
	 */
	public NClob getWrappedNClob();
}
