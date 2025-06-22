/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.hql.HqlInterpretationException;
import org.hibernate.query.hql.spi.SemanticPathPart;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmRenderContext;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.select.SqmSelectableNode;
import org.hibernate.type.BasicType;

import java.util.Objects;

public class SqmAnyDiscriminatorValue<T> extends AbstractSqmExpression<T>
		implements SqmSelectableNode<T>, SemanticPathPart {

	private final EntityDomainType value;
	private final BasicType domainType;
	private final String pathName;

	public SqmAnyDiscriminatorValue(
			String pathName,
			EntityDomainType entityValue,
			BasicType<T> domainType,
			NodeBuilder nodeBuilder) {
		super( domainType, nodeBuilder );
		this.value = entityValue;
		this.pathName = pathName;
		this.domainType = domainType;
	}

	public BasicType<T> getDomainType(){
		return domainType;
	}

	@Override
	public SqmAnyDiscriminatorValue<T> copy(SqmCopyContext context) {
		final SqmAnyDiscriminatorValue<T> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmAnyDiscriminatorValue<T> expression = context.registerCopy(
				this,
				new SqmAnyDiscriminatorValue<>(
						pathName,
						value,
						domainType,
						nodeBuilder()
				)
		);
		copyTo( expression, context );
		return expression;
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitAnyDiscriminatorTypeValueExpression( this );
	}

	public EntityDomainType getEntityValue() {
		return value;
	}

	public String getPathName() {
		return pathName;
	}

	@Override
	public String asLoggableText() {
		return getEntityValue().getName();
	}

	@Override
	public SemanticPathPart resolvePathPart(
			String name,
			boolean isTerminal,
			SqmCreationState creationState) {
		throw new HqlInterpretationException( "Cannot dereference an entity name" );
	}

	@Override
	public SqmPath<?> resolveIndexedAccess(
			SqmExpression<?> selector,
			boolean isTerminal,
			SqmCreationState creationState) {
		throw new HqlInterpretationException( "Cannot dereference an entity name" );
	}

	@Override
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		hql.append( getEntityValue().getName() );
	}

	@Override
	public boolean equals(Object object) {
		return object instanceof SqmAnyDiscriminatorValue<?> that
			&& Objects.equals( this.value.getName(), that.value.getName() )
			&& Objects.equals( this.pathName, that.pathName );
	}

	@Override
	public int hashCode() {
		return Objects.hash( value.getName(), pathName );
	}
}
