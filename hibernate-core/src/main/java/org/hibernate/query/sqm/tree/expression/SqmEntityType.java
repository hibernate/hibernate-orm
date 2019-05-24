/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmExpressable;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.sql.results.spi.DomainResultProducer;

/**
 * Entity type expression based on a parameter - `TYPE( :someParam )`
 *
 * @author Steve Ebersole
 */
public class SqmEntityType<T> extends AbstractSqmExpression<T> implements DomainResultProducer {
	private final SqmExpression discriminatorSource;

	public SqmEntityType(SqmParameter<T> parameterExpression, NodeBuilder nodeBuilder) {
		super( parameterExpression.getAnticipatedType(), nodeBuilder );
		this.discriminatorSource = parameterExpression;
	}

	public SqmEntityType(SqmPath<T> entityValuedPath, NodeBuilder nodeBuilder) {
		//noinspection unchecked
		super( entityValuedPath.getReferencedPathSource().sqmAs( EntityDomainType.class ), nodeBuilder );
		this.discriminatorSource = entityValuedPath;
	}

	@Override
	public void internalApplyInferableType(SqmExpressable<?> type) {
		setExpressableType( type );

		//noinspection unchecked
		discriminatorSource.applyInferableType( type );
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitParameterizedEntityTypeExpression( this );
	}

	@Override
	public DomainResult createDomainResult(
			String resultVariable,
			DomainResultCreationState creationState) {
		throw new UnsupportedOperationException( "At the moment, selection of an entity's type as a QueryResult is not supported" );
		// todo (6.0) : but could be ^^ - consider adding support for this (returning Class)
	}

}
