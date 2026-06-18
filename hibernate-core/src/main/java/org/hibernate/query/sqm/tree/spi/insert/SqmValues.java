/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.spi.insert;

import jakarta.annotation.Nullable;
import jakarta.annotation.Nonnull;
import org.hibernate.query.criteria.JpaValues;
import org.hibernate.query.sqm.tree.spi.SqmCacheable;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;
import org.hibernate.query.sqm.tree.spi.expression.SqmExpression;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * @author Gavin King
 */
public class SqmValues implements JpaValues, Serializable, SqmCacheable {
	private final List<SqmExpression<?>> expressions;

	public SqmValues(List<SqmExpression<?>> expressions) {
		this.expressions = expressions;
	}

	private SqmValues(SqmValues original, SqmCopyContext context) {
		this.expressions = new ArrayList<>( original.expressions.size() );
		for ( SqmExpression<?> expression : original.expressions ) {
			this.expressions.add( expression.copy( context ) );
		}
	}

	public SqmValues copy(SqmCopyContext context) {
		return new SqmValues( this, context );
	}

	@Nonnull
	@Override
	public List<SqmExpression<?>> getExpressions() {
		return Collections.unmodifiableList( expressions );
	}

	@Override
	public boolean equals(@Nullable Object object) {
		return object instanceof SqmValues that
			&& Objects.equals( expressions, that.expressions );
	}

	@Override
	public int hashCode() {
		return Objects.hashCode( expressions );
	}

	@Override
	public boolean isCompatible(Object object) {
		return object instanceof SqmValues that
			&& SqmCacheable.areCompatible( expressions, that.expressions );
	}

	@Override
	public int cacheHashCode() {
		return SqmCacheable.cacheHashCode( expressions );
	}
}
