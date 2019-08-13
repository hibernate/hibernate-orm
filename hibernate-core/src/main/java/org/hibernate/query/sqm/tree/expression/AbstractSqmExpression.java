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
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmExpressable;
import org.hibernate.query.sqm.SqmTreeCreationLogger;
import org.hibernate.query.sqm.tree.jpa.AbstractJpaSelection;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

import static org.hibernate.query.internal.QueryHelper.highestPrecedenceType;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSqmExpression<T> extends AbstractJpaSelection<T> implements SqmExpression<T> {

	public AbstractSqmExpression(SqmExpressable<T> type, NodeBuilder criteriaBuilder) {
		super( type, criteriaBuilder );
	}

	@Override
	public final void applyInferableType(SqmExpressable<?> type) {
		if ( type == null ) {
			return;
		}

		final SqmExpressable<?> oldType = getNodeType();

		final SqmExpressable<?> newType = highestPrecedenceType( oldType, type );
		if ( newType != null && newType != oldType ) {
			internalApplyInferableType( newType );
		}
	}

	protected void internalApplyInferableType(SqmExpressable<?> newType) {
		SqmTreeCreationLogger.LOGGER.debugf(
				"Applying inferable type to SqmExpression [%s] : %s -> %s",
				this,
				getNodeType(),
				newType
		);

		setExpressableType( highestPrecedenceType( newType, getNodeType() ) );
	}

	@Override
	public SqmExpression<Long> asLong() {
		//noinspection unchecked
		return castAs( StandardBasicTypes.LONG );
	}

	@Override
	public SqmExpression<Integer> asInteger() {
		//noinspection unchecked
		return castAs( StandardBasicTypes.INTEGER );
	}

	@Override
	public SqmExpression<Float> asFloat() {
		//noinspection unchecked
		return castAs( StandardBasicTypes.FLOAT );
	}

	@Override
	public SqmExpression<Double> asDouble() {
		//noinspection unchecked
		return castAs( StandardBasicTypes.DOUBLE );
	}

	@Override
	public SqmExpression<BigDecimal> asBigDecimal() {
		//noinspection unchecked
		return castAs( StandardBasicTypes.BIG_DECIMAL );
	}

	@Override
	public SqmExpression<BigInteger> asBigInteger() {
		//noinspection unchecked
		return castAs( StandardBasicTypes.BIG_INTEGER );
	}

	@Override
	public SqmExpression<String> asString() {
		//noinspection unchecked
		return castAs( StandardBasicTypes.STRING );
	}

	@Override
	public <X> SqmExpression<X> as(Class<X> type) {
		return nodeBuilder().cast(this, type);
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

	@Override
	public JavaTypeDescriptor<T> getJavaTypeDescriptor() {
		return getNodeType().getExpressableJavaTypeDescriptor();
	}
}
