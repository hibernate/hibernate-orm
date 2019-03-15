/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import java.util.function.Supplier;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;
import org.hibernate.sql.results.spi.DomainResult;
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
	private final SqmPath pluralPath;
	private final BasicValuedExpressableType sizeType;

	public SqmCollectionSize(SqmPath pluralPath, BasicValuedExpressableType sizeType) {
		this.pluralPath = pluralPath;
		this.sizeType = sizeType;
	}

	public SqmPath getPluralPath() {
		return pluralPath;
	}

	@Override
	public BasicValuedExpressableType getExpressableType() {
		return sizeType;
	}

	@Override
	public Supplier<? extends BasicValuedExpressableType> getInferableType() {
		return this::getExpressableType;
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitPluralAttributeSizeFunction( this );
	}

	@Override
	public String asLoggableText() {
		return "SIZE(" + pluralPath.asLoggableText() + ")";
	}

	@Override
	public DomainResult createDomainResult(
			String resultVariable,
			DomainResultCreationState creationState) {
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
