/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree;

import org.hibernate.Incubating;
import org.hibernate.query.sqm.internal.NoParamSqmCopyContext;
import org.hibernate.query.sqm.internal.SimpleSqmCopyContext;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 *
 */
public interface SqmCopyContext {

	<T> @Nullable T getCopy(T original);

	<T> T registerCopy(T original, T copy);

	/**
	 * Returns whether the {@code fetch} flag for attribute joins should be copied over.
	 *
	 * @since 6.4
	 */
	@Incubating
	default boolean copyFetchedFlag() {
		return true;
	}

	static SqmCopyContext simpleContext() {
		return new SimpleSqmCopyContext();
	}

	static SqmCopyContext noParamCopyContext() {
		return new NoParamSqmCopyContext();
	}
}
