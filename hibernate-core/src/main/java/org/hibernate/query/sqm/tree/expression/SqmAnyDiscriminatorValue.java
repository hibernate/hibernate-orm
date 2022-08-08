/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.internal.AnyDiscriminatorDomainTypeImpl;
import org.hibernate.query.hql.HqlInterpretationException;
import org.hibernate.query.hql.spi.SemanticPathPart;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.select.SqmSelectableNode;

public class SqmAnyDiscriminatorValue<T> extends AbstractSqmExpression<T>
		implements SqmSelectableNode<T>, SemanticPathPart {

	private final EntityDomainType value;
	private final AnyDiscriminatorDomainTypeImpl domainType;
	private final String pathName;

	public SqmAnyDiscriminatorValue(
			EntityDomainType<T> entityWithDiscriminator,
			String pathName,
			EntityDomainType entityValue,
			AnyDiscriminatorDomainTypeImpl domainType,
			NodeBuilder nodeBuilder) {
		super( entityWithDiscriminator, nodeBuilder );
		this.value = entityValue;
		this.pathName = pathName;
		this.domainType = domainType;
	}

	public AnyDiscriminatorDomainTypeImpl getDomainType(){
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
						(EntityDomainType) getNodeType(),
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
	public void appendHqlString(StringBuilder sb) {
		sb.append( getEntityValue().getName() );
	}
}
