/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.spi.expression;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.query.sqm.spi.NodeBuilder;
import org.hibernate.query.sqm.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.spi.SqmBindableType;
import org.hibernate.query.sqm.tree.spi.AbstractSqmNode;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;
import org.hibernate.query.sqm.tree.spi.SqmRenderContext;
import org.hibernate.query.sqm.tree.spi.SqmTypedNode;

import java.util.Objects;

/**
 * @author Gavin King
 */
public class SqmExtractUnit<T> extends AbstractSqmNode implements SqmTypedNode<T> {
	private final TemporalUnit unit;
	private final ReturnableType<T> type;

	public SqmExtractUnit(TemporalUnit unit, ReturnableType<T> type, NodeBuilder nodeBuilder) {
		super( nodeBuilder );
		this.unit = unit;
		this.type = type;
	}

	@Override
	public SqmExtractUnit<T> copy(SqmCopyContext context) {
		return this;
	}

	public TemporalUnit getUnit() {
		return unit;
	}

	public ReturnableType<T> getType() {
		return type;
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitExtractUnit( this );
	}

	@Override
	public @Nonnull SqmBindableType<T> getNodeType() {
		return nodeBuilder().resolveExpressible( type );
	}

	@Override
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		hql.append( unit );
	}

	@Override
	public boolean equals(@Nullable Object object) {
		return object instanceof SqmExtractUnit<?> that
			&& unit == that.unit
			&& Objects.equals( type, that.type );
	}

	@Override
	public int hashCode() {
		int result = Objects.hashCode( unit );
		result = 31 * result + Objects.hashCode( type );
		return result;
	}

	@Override
	public boolean isCompatible(Object object) {
		return equals( object );
	}

	@Override
	public int cacheHashCode() {
		return hashCode();
	}
}
