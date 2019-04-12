/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import javax.persistence.criteria.Expression;

import org.hibernate.query.criteria.JpaSelection;
import org.hibernate.query.internal.QueryHelper;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.produce.SqmTreeCreationLogger;
import org.hibernate.query.sqm.tree.jpa.AbstractJpaSelection;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;
import org.hibernate.type.spi.StandardSpiBasicTypes;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSqmExpression<T> extends AbstractJpaSelection<T> implements SqmExpression<T> {
	private ExpressableType<T> type;

	public AbstractSqmExpression(ExpressableType<T> type, NodeBuilder criteriaBuilder) {
		super( type, criteriaBuilder );
		this.type = type;
	}

	@Override
	public ExpressableType<T> getExpressableType() {
		return type;
	}

	@Override
	public final void applyInferableType(ExpressableType<?> type) {
		if ( type == null ) {
			return;
		}

		final ExpressableType newType = QueryHelper.highestPrecedenceType( this.type, type );
		if ( newType != null && newType != this.type ) {
			internalApplyInferableType( newType );
		}
	}

	@SuppressWarnings("unchecked")
	protected void internalApplyInferableType(ExpressableType<?> newType) {
		SqmTreeCreationLogger.LOGGER.debugf(
				"Applying inferable type to SqmExpression [%s] : %s -> %s",
				this,
				this.type,
				newType
		);
		this.type = (ExpressableType<T>) newType;
	}

	@Override
	public JavaTypeDescriptor<T> getJavaTypeDescriptor() {
		final ExpressableType<T> expressableType = getExpressableType();
		return expressableType != null ? expressableType.getJavaTypeDescriptor() : null;
	}

	@Override
	@SuppressWarnings("unchecked")
	public SqmExpression<Long> asLong() {
		applyInferableType( StandardSpiBasicTypes.LONG );
		return (SqmExpression) this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public SqmExpression<Integer> asInteger() {
		applyInferableType( StandardSpiBasicTypes.INTEGER );
		return (SqmExpression) this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public SqmExpression<Float> asFloat() {
		applyInferableType( StandardSpiBasicTypes.FLOAT );
		return (SqmExpression) this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public SqmExpression<Double> asDouble() {
		applyInferableType( StandardSpiBasicTypes.DOUBLE );
		return (SqmExpression) this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public SqmExpression<BigDecimal> asBigDecimal() {
		applyInferableType( StandardSpiBasicTypes.BIG_DECIMAL );
		return (SqmExpression) this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public SqmExpression<BigInteger> asBigInteger() {
		applyInferableType( StandardSpiBasicTypes.BIG_INTEGER );
		return (SqmExpression) this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public SqmExpression<String> asString() {
		applyInferableType( StandardSpiBasicTypes.STRING );
		return (SqmExpression) this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X> SqmExpression<X> as(Class<X> type) {
		applyInferableType(
				nodeBuilder().getDomainModel()
						.getTypeConfiguration()
						.standardExpressableTypeForJavaType( type )
		);

		if ( type.isInstance( this ) ) {
			//noinspection unchecked
			return (SqmExpression<X>) this;
		}

		if ( String.class.equals( type ) ) {
			return (SqmExpression<X>) asString();
		}

		if ( Integer.class.equals( type ) ) {
			return (SqmExpression<X>) asInteger();
		}

		if ( Long.class.equals( type ) ) {
			return (SqmExpression<X>) asLong();
		}

		if ( Double.class.equals( type ) ) {
			return (SqmExpression<X>) asDouble();
		}

		if ( Float.class.equals( type ) ) {
			return (SqmExpression<X>) asFloat();
		}

		if ( BigInteger.class.equals( type ) ) {
			return (SqmExpression<X>) asBigInteger();
		}

		if ( BigDecimal.class.equals( type ) ) {
			return (SqmExpression<X>) asBigDecimal();
		}

		throw new UnsupportedOperationException( "SqmStaticEnumReference cannot be cast as `" + type.getName() + "`" );
	}

	@Override
	public SqmPredicate isNull() {
		return nodeBuilder().isNull( this );
	}

	@Override
	public SqmPredicate isNotNull() {
		return nodeBuilder().isNotNull( this );
	}

	@Override
	public SqmPredicate in(Object... values) {
		return nodeBuilder().in( this, values );
	}

	@Override
	public SqmPredicate in(Expression<?>... values) {
		return nodeBuilder().in( this, values );
	}

	@Override
	public SqmPredicate in(Collection<?> values) {
		return nodeBuilder().in( this, values );
	}

	@Override
	public SqmPredicate in(Expression<Collection<?>> values) {
		return nodeBuilder().in( this, values );
	}

	@Override
	public JpaSelection<T> alias(String name) {
		setAlias( name );
		return this;
	}
}
