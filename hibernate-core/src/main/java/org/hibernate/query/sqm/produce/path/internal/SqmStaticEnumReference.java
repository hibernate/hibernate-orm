/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.path.internal;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import javax.persistence.criteria.Expression;

import org.hibernate.metamodel.model.mapping.spi.BasicValuedNavigable;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.produce.path.spi.SemanticPathPart;
import org.hibernate.query.sqm.produce.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.expression.AbstractSqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmLiteral;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.type.descriptor.java.spi.EnumJavaDescriptor;
import org.hibernate.type.spi.StandardSpiBasicTypes;

/**
 * @author Steve Ebersole
 */
public class SqmStaticEnumReference<T extends Enum<T>>
		extends AbstractSqmExpression<T>
		implements SemanticPathPart, SqmExpression<T> {
	private final Enum referencedEnum;

	public SqmStaticEnumReference(Enum referencedEnum, EnumJavaDescriptor<T> jtd, NodeBuilder nodeBuilder) {
		super( null, nodeBuilder );
		this.referencedEnum = referencedEnum;

		setExpressableType( nodeBuilder.getTypeConfiguration().standardExpressableTypeForJavaType( jtd ) );
	}

	@Override
	public EnumJavaDescriptor<T> getJavaTypeDescriptor() {
		return (EnumJavaDescriptor<T>) super.getJavaTypeDescriptor();
	}

	@Override
	public SemanticPathPart resolvePathPart(
			String name,
			String currentContextKey,
			boolean isTerminal,
			SqmCreationState creationState) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public SqmPath resolveIndexedAccess(
			SqmExpression selector,
			String currentContextKey,
			boolean isTerminal,
			SqmCreationState creationState) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	protected void internalApplyInferableType(ExpressableType<?> newType) {
		//noinspection StatementWithEmptyBody
		if ( newType instanceof BasicValuedNavigable ) {
			final BasicValuedNavigable basicValuedNavigable = (BasicValuedNavigable) newType;
			setExpressableType( basicValuedNavigable );
		}
		else {
			// nothing to do
		}
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitFullyQualifiedEnum( referencedEnum );
	}

	@Override
	public Class<T> getJavaType() {
		return getJavaTypeDescriptor().getJavaType();
	}

	private SqmLiteral<T> forName(BasicValuedExpressableType<T> type) {
		//noinspection unchecked
		return new SqmLiteral(
				referencedEnum.name(),
				type,
				nodeBuilder()
		);
	}

	private SqmLiteral<T> forOrdinal(BasicValuedExpressableType<T> type) {
		//noinspection unchecked
		return new SqmLiteral(
				referencedEnum.ordinal(),
				type,
				nodeBuilder()
		);
	}

	@Override
	public SqmExpression<Long> asLong() {
		//noinspection unchecked
		return forOrdinal( (BasicValuedExpressableType) StandardSpiBasicTypes.LONG );
	}

	@Override
	public SqmExpression<Integer> asInteger() {
		//noinspection unchecked
		return forOrdinal( (BasicValuedExpressableType) StandardSpiBasicTypes.INTEGER );
	}

	@Override
	public SqmExpression<Float> asFloat() {
		//noinspection unchecked
		return forOrdinal( (BasicValuedExpressableType) StandardSpiBasicTypes.FLOAT );
	}

	@Override
	public SqmExpression<Double> asDouble() {
		//noinspection unchecked
		return forOrdinal( (BasicValuedExpressableType) StandardSpiBasicTypes.DOUBLE );
	}

	@Override
	public SqmExpression<BigDecimal> asBigDecimal() {
		//noinspection unchecked
		return forOrdinal( (BasicValuedExpressableType) StandardSpiBasicTypes.BIG_DECIMAL );
	}

	@Override
	public SqmExpression<BigInteger> asBigInteger() {
		//noinspection unchecked
		return forOrdinal( (BasicValuedExpressableType) StandardSpiBasicTypes.BIG_INTEGER );
	}

	@Override
	public SqmExpression<String> asString() {
		//noinspection unchecked
		return forName( (BasicValuedExpressableType) StandardSpiBasicTypes.STRING );
	}

	@Override
	public SqmPredicate isNull() {
		return null;
	}

	@Override
	public SqmPredicate isNotNull() {
		return null;
	}

	@Override
	public SqmPredicate in(Object... values) {
		return null;
	}

	@Override
	public SqmPredicate in(Expression<?>... values) {
		return null;
	}

	@Override
	public SqmPredicate in(Collection<?> values) {
		return null;
	}

	@Override
	public SqmPredicate in(Expression<Collection<?>> values) {
		return null;
	}
}
