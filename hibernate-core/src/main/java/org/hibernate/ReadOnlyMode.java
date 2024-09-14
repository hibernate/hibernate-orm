/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

import jakarta.persistence.FindOption;

/**
 * A {@link jakarta.persistence.FindOption} which requests that
 * entities be loaded in {@link #READ_ONLY} mode or in regular
 * {@link #READ_WRITE} mode.
 *
 * @since 7.0
 *
 * @see Session#setDefaultReadOnly(boolean)
 * @see Session#find(Class, Object, FindOption...)
 *
 * @author Gavin King
 */
public enum ReadOnlyMode implements FindOption {
	/**
	 * Specifies that an entity should be loaded in read-only mode.
	 * <p>
	 * Read-only entities are not dirty-checked and snapshots of
	 * persistent state are not maintained. Read-only entities can
	 * be modified, but changes are not persisted.
	 */
	READ_ONLY,
	/**
	 * Specifies that an entity should be loaded in the default
	 * modifiable mode, regardless of the default mode of the
	 * session.
	 */
	READ_WRITE
}
