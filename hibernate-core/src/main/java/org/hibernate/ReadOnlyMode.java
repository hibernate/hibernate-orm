/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
	 * Read-only entities are not dirty-checked, and snapshots of
	 * persistent state are not maintained. Read-only entities can
	 * be modified, but a modification to a field of a read-only
	 * entity is not made persistent
	 */
	READ_ONLY,
	/**
	 * Specifies that an entity should be loaded in the default
	 * modifiable mode, regardless of the default mode of the
	 * session.
	 */
	READ_WRITE
}
