/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree;

import org.hibernate.Incubating;
import org.hibernate.query.sqm.SqmQuerySource;
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

	/**
	 * Returns the query source to use for copied queries.
	 * {@code null} means, that the original query source should be retained.
	 *
	 * @since 7.0
	 */
	@Incubating
	default @Nullable SqmQuerySource getQuerySource() {
		return null;
	}

	static SqmCopyContext simpleContext() {
		return new SimpleSqmCopyContext();
	}

	static SqmCopyContext simpleContext(SqmQuerySource querySource) {
		return new SimpleSqmCopyContext( querySource );
	}

	static SqmCopyContext noParamCopyContext() {
		return new NoParamSqmCopyContext();
	}

	static SqmCopyContext noParamCopyContext(SqmQuerySource querySource) {
		return new NoParamSqmCopyContext( querySource );
	}
}
