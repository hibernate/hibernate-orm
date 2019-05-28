/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.hql.spi.SemanticPathPart;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.SemanticException;
import org.hibernate.query.sqm.SqmExpressable;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.hql.HqlInterpretationException;
import org.hibernate.query.sqm.produce.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.sql.results.spi.DomainResultProducer;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * Represents an reference to an entity type as a literal.  This is the JPA
 * terminology for cases when we have something like: {@code ... where TYPE(e) = SomeType}.
 * The token {@code SomeType} is an "entity type literal".
 *
 * An entity type expression can be used to restrict query polymorphism. The TYPE operator returns the exact type of the argument.
 *
 * @author Steve Ebersole
 */
public class SqmLiteralEntityType<T>
		extends AbstractSqmExpression<T>
		implements DomainResultProducer, SemanticPathPart {
	private final EntityDomainType<T> entityType;

	public SqmLiteralEntityType(EntityDomainType<T> entityType, NodeBuilder nodeBuilder) {
		super( entityType, nodeBuilder );
		this.entityType = entityType;
	}

	@Override
	public JavaTypeDescriptor<T> getJavaTypeDescriptor() {
		return getNodeType().getExpressableJavaTypeDescriptor();
	}

	@Override
	public EntityDomainType<T> getNodeType() {
		return entityType;
	}

	@Override
	public void internalApplyInferableType(SqmExpressable<?> type) {
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitEntityTypeLiteralExpression( this );
	}

	@Override
	public DomainResult createDomainResult(
			String resultVariable,
			DomainResultCreationState creationState) {
		throw new SemanticException( "Selecting an entity type is not allowed. An entity type expression can be used to restrict query polymorphism ");
		// todo (6.0) : but could be ^^ - consider adding support for this (returning Class)
	}


	@Override
	public String asLoggableText() {
		return "TYPE(" + entityType + ")";
	}

	@Override
	public SemanticPathPart resolvePathPart(
			String name,
			boolean isTerminal,
			SqmCreationState creationState) {
		throw new HqlInterpretationException( "Cannot dereference an entity name" );
	}

	@Override
	public SqmPath resolveIndexedAccess(
			SqmExpression selector,
			boolean isTerminal,
			SqmCreationState creationState) {
		throw new HqlInterpretationException( "Cannot dereference an entity name" );
	}
}
