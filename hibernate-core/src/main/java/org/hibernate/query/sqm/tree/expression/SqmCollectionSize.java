/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.expression.domain.SqmPluralAttributeReference;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultCreationContext;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.sql.results.spi.DomainResultProducer;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * Represents the {@code SIZE()} function.
 *
 * @author Steve Ebersole
 * @author Gunnar Morling
 */
public class SqmCollectionSize implements SqmExpression, DomainResultProducer {
	private final SqmPluralAttributeReference pluralAttributeBinding;
	private final BasicValuedExpressableType sizeType;

	public SqmCollectionSize(SqmPluralAttributeReference pluralAttributeBinding, BasicValuedExpressableType sizeType) {
		this.pluralAttributeBinding = pluralAttributeBinding;
		this.sizeType = sizeType;
	}

	public SqmPluralAttributeReference getPluralAttributeBinding() {
		return pluralAttributeBinding;
	}

	@Override
	public BasicValuedExpressableType getExpressableType() {
		return sizeType;
	}

	@Override
	public BasicValuedExpressableType getInferableType() {
		return getExpressableType();
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitPluralAttributeSizeFunction( this );
	}

	@Override
	public String asLoggableText() {
		return "SIZE(" + pluralAttributeBinding.asLoggableText() + ")";
	}

	@Override
	public DomainResult createDomainResult(
			String resultVariable,
			DomainResultCreationState creationState,
			DomainResultCreationContext creationContext) {
		throw new NotYetImplementedFor6Exception(  );
//		return new ScalarQueryResultImpl(
//				resultVariable,
//				creationContext.getSqlExpressionResolver().resolveSqlSelection( expression ),
//				getExpressableType()
//		);
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return getExpressableType().getJavaTypeDescriptor();
	}
}
