/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
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
