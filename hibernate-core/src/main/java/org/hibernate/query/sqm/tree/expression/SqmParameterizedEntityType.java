/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultCreationContext;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.sql.results.spi.DomainResultProducer;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * Entity type expression based on a parameter - `TYPE( :someParam )`
 *
 * @author Steve Ebersole
 */
public class SqmParameterizedEntityType implements SqmExpression, DomainResultProducer {
	private final SqmParameter parameterExpression;

	public SqmParameterizedEntityType(SqmParameter parameterExpression) {
		this.parameterExpression = parameterExpression;
	}

	@Override
	public ExpressableType getExpressableType() {
		return parameterExpression.getExpressableType();
	}

	@Override
	public ExpressableType getInferableType() {
		return parameterExpression.getExpressableType();
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitParameterizedEntityTypeExpression( this );
	}

	@Override
	public String asLoggableText() {
		return "TYPE(" + parameterExpression.asLoggableText() + ")";
	}

	@Override
	public DomainResult createDomainResult(
			String resultVariable,
			DomainResultCreationState creationState,
			DomainResultCreationContext creationContext) {
		throw new UnsupportedOperationException( "At the moment, selection of an entity's type as a QueryResult is not supported" );
		// todo (6.0) : but could be ^^ - consider adding support for this (returning Class)
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return getExpressableType().getJavaTypeDescriptor();
	}
}
