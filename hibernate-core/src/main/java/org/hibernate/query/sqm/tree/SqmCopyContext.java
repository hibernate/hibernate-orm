/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
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
