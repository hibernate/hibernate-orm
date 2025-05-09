/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.criteria.JpaCastTarget;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.tree.AbstractSqmNode;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmRenderContext;
import org.hibernate.query.sqm.tree.SqmTypedNode;

import java.util.Objects;


/**
 * @author Gavin King
 */
public class SqmCastTarget<T> extends AbstractSqmNode implements SqmTypedNode<T>, JpaCastTarget<T> {
	private final ReturnableType<T> type;
	private final Long length;
	private final Integer precision;
	private final Integer scale;

	public SqmCastTarget(
			ReturnableType<T> type,
			NodeBuilder nodeBuilder) {
		this( type, null, nodeBuilder );
	}

	public SqmCastTarget(
			ReturnableType<T> type,
			Long length,
			NodeBuilder nodeBuilder) {
		this( type, length, null, null, nodeBuilder );
	}

	public SqmCastTarget(
			ReturnableType<T> type,
			Integer precision,
			Integer scale,
			NodeBuilder nodeBuilder) {
		this( type, null, precision, scale, nodeBuilder );
	}

	public SqmCastTarget(
			ReturnableType<T> type,
			Long length,
			Integer precision,
			Integer scale,
			NodeBuilder nodeBuilder) {
		super( nodeBuilder );
		this.type = type;
		this.length = length;
		this.precision = precision;
		this.scale = scale;
	}

	@Override
	public @Nullable Long getLength() {
		return length;
	}

	@Override
	public @Nullable Integer getPrecision() {
		return precision;
	}

	@Override
	public @Nullable Integer getScale() {
		return scale;
	}

	@Override
	public SqmCastTarget<T> copy(SqmCopyContext context) {
		return this;
	}

	public ReturnableType<T> getType() {
		return type;
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitCastTarget(this);
	}

	@Override
	public SqmExpressible<T> getNodeType() {
		return type.resolveExpressible( nodeBuilder() );
	}

	@Override
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		hql.append( type.getTypeName() );
		if ( precision != null ) {
			hql.append( '(' );
			hql.append( precision );
			if ( scale != null ) {
				hql.append( ", " );
				hql.append( scale );
			}
			hql.append( ')' );
		}
		else if ( length != null ) {
			hql.append( '(' );
			hql.append( length );
			hql.append( ')' );
		}
	}

	@Override
	public boolean equals(Object object) {
		return object instanceof SqmCastTarget<?> that
			&& Objects.equals( type, that.type )
			&& Objects.equals( length, that.length )
			&& Objects.equals( precision, that.precision )
			&& Objects.equals( scale, that.scale );
	}

	@Override
	public int hashCode() {
		return Objects.hash( type, length, precision, scale );
	}
}
