/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
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
		if ( original instanceof SqmParameter<?> ) {
			return original;
		}
		return super.getCopy( original );
	}
}
