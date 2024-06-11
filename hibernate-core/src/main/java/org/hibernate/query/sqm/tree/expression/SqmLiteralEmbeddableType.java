/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.metamodel.model.domain.EmbeddableDomainType;
import org.hibernate.query.hql.HqlInterpretationException;
import org.hibernate.query.hql.spi.SemanticPathPart;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.select.SqmSelectableNode;

import static org.hibernate.persister.entity.DiscriminatorHelper.getDiscriminatorType;

/**
 * Represents a reference to an embeddable type as a literal.
 *
 * @author Marco Belladelli
 */
public class SqmLiteralEmbeddableType<T>
		extends AbstractSqmExpression<T>
		implements SqmSelectableNode<T>, SemanticPathPart {
	final EmbeddableDomainType<T> embeddableDomainType;

	public SqmLiteralEmbeddableType(
			EmbeddableDomainType<T> embeddableDomainType,
			NodeBuilder nodeBuilder) {
		super( getDiscriminatorType( embeddableDomainType, nodeBuilder), nodeBuilder );
		this.embeddableDomainType = embeddableDomainType;
	}

	public EmbeddableDomainType<T> getEmbeddableDomainType() {
		return embeddableDomainType;
	}

	@Override
	public SqmLiteralEmbeddableType<T> copy(SqmCopyContext context) {
		final SqmLiteralEmbeddableType<T> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmLiteralEmbeddableType<T> expression = context.registerCopy(
				this,
				new SqmLiteralEmbeddableType<>(
						embeddableDomainType,
						nodeBuilder()
				)
		);
		copyTo( expression, context );
		return expression;
	}

	@Override
	public void internalApplyInferableType(SqmExpressible<?> type) {
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitEmbeddableTypeLiteralExpression( this );
	}

	@Override
	public String asLoggableText() {
		return "TYPE(" + embeddableDomainType + ")";
	}

	@Override
	public SemanticPathPart resolvePathPart(
			String name,
			boolean isTerminal,
			SqmCreationState creationState) {
		throw new HqlInterpretationException( "Cannot dereference an embeddable name" );
	}

	@Override
	public SqmPath<?> resolveIndexedAccess(
			SqmExpression<?> selector,
			boolean isTerminal,
			SqmCreationState creationState) {
		throw new HqlInterpretationException( "Cannot dereference an embeddable name" );
	}

	@Override
	public void appendHqlString(StringBuilder sb) {
		sb.append( embeddableDomainType.getTypeName() );
	}
}
