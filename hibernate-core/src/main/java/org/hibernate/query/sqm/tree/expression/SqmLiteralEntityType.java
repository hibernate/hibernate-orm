/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.query.sqm.tree.TreeException;
import org.hibernate.sql.ast.produce.metamodel.spi.EntityValuedExpressableType;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultCreationContext;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.sql.results.spi.DomainResultProducer;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * Represents an reference to an entity type as a literal.  This is the JPA
 * terminology for cases when we have something like: {@code ... where TYPE(e) = SomeType}.
 * The token {@code SomeType} is an "entity type literal".
 *
 * An entity type expression can be used to restrict query polymorphism. The TYPE operator returns the exact type of the argument.
 *
 * @author Steve Ebersole
 */
public class SqmLiteralEntityType implements SqmExpression, DomainResultProducer {
	private final EntityValuedExpressableType entityType;

	public SqmLiteralEntityType(EntityValuedExpressableType entityType) {
		this.entityType = entityType;
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return getExpressableType().getJavaTypeDescriptor();
	}

	@Override
	public EntityValuedExpressableType getExpressableType() {
		return entityType;
	}

	@Override
	public EntityValuedExpressableType getInferableType() {
		return getExpressableType();
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitEntityTypeLiteralExpression( this );
	}

	@Override
	public DomainResult createDomainResult(
			String resultVariable,
			DomainResultCreationState creationState,
			DomainResultCreationContext creationContext) {
		throw new TreeException("Selecting an entity type is not allowed. An entity type expression can be used to restrict query polymorphism ");
		// todo (6.0) : but could be ^^ - consider adding support for this (returning Class)
	}


	@Override
	public String asLoggableText() {
		return "TYPE(" + entityType + ")";
	}
}
