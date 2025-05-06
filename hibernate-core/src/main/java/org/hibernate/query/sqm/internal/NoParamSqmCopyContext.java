/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.internal;

import org.hibernate.query.sqm.SqmQuerySource;
import org.hibernate.query.sqm.tree.expression.SqmParameter;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * @author Marco Belladelli
 */
public class NoParamSqmCopyContext extends SimpleSqmCopyContext {
	public NoParamSqmCopyContext() {
	}

	public NoParamSqmCopyContext(@Nullable SqmQuerySource querySource) {
		super( querySource );
	}

	@Override
	public <T> @Nullable T getCopy(T original) {
		return original instanceof SqmParameter<?> ? original : super.getCopy( original );
	}
}
