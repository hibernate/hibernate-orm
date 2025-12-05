/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmBindableType;
import org.hibernate.query.sqm.tree.AbstractSqmNode;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmRenderContext;
import org.hibernate.query.sqm.tree.SqmTypedNode;

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
	public @NonNull SqmBindableType<T> getNodeType() {
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
