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
import java.util.List;
import java.util.Locale;

import javax.persistence.criteria.Expression;

import org.hibernate.query.SemanticException;
import org.hibernate.query.criteria.JpaSelection;
import org.hibernate.query.hql.spi.SemanticPathPart;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmExpressable;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.produce.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class SqmFieldLiteral<T> implements SqmExpression<T>, SqmExpressable<T>, SemanticPathPart {
	private final T value;
	private final JavaTypeDescriptor<T> fieldOwnerJavaTypeDescriptor;
	private final String fieldName;
	private final NodeBuilder nodeBuilder;

	private SqmExpressable<T> expressable;

	public SqmFieldLiteral(
			T value,
			JavaTypeDescriptor<T> fieldOwnerJavaTypeDescriptor,
			String fieldName,
			NodeBuilder nodeBuilder) {
		this.value = value;
		this.fieldOwnerJavaTypeDescriptor = fieldOwnerJavaTypeDescriptor;
		this.fieldName = fieldName;
		this.nodeBuilder = nodeBuilder;

		this.expressable = this;
	}

	public T getValue() {
		return value;
	}

	public JavaTypeDescriptor<T> getFieldOwnerJavaTypeDescriptor() {
		return fieldOwnerJavaTypeDescriptor;
	}

	public String getFieldName() {
		return fieldName;
	}

	public NodeBuilder getNodeBuilder() {
		return nodeBuilder;
	}

	@Override
	public SqmExpressable<T> getNodeType() {
		return expressable;
	}

	@Override
	public void applyInferableType(SqmExpressable<?> type) {
		//noinspection unchecked
		this.expressable = (SqmExpressable) type;
	}

	@Override
	public JavaTypeDescriptor<T> getExpressableJavaTypeDescriptor() {
		return expressable.getExpressableJavaTypeDescriptor();
	}

	@Override
	public JavaTypeDescriptor<T> getJavaTypeDescriptor() {
		return getExpressableJavaTypeDescriptor();
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitFieldLiteral( this );
	}

	@Override
	public NodeBuilder nodeBuilder() {
		return nodeBuilder;
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
	public SqmExpression<Long> asLong() {
		//noinspection unchecked
		return (SqmExpression<Long>) this;
	}

	@Override
	public SqmExpression<Integer> asInteger() {
		//noinspection unchecked
		return (SqmExpression<Integer>) this;
	}

	@Override
	public SqmExpression<Float> asFloat() {
		//noinspection unchecked
		return (SqmExpression<Float>) this;
	}

	@Override
	public SqmExpression<Double> asDouble() {
		//noinspection unchecked
		return (SqmExpression<Double>) this;
	}

	@Override
	public SqmExpression<BigDecimal> asBigDecimal() {
		//noinspection unchecked
		return (SqmExpression<BigDecimal>) this;
	}

	@Override
	public SqmExpression<BigInteger> asBigInteger() {
		//noinspection unchecked
		return (SqmExpression<BigInteger>) this;
	}

	@Override
	public SqmExpression<String> asString() {
		//noinspection unchecked
		return (SqmExpression<String>) this;
	}

	@Override
	public <X> SqmExpression<X> as(Class<X> type) {
		return null;
	}

	@Override
	public SemanticPathPart resolvePathPart(
			String name,
			boolean isTerminal,
			SqmCreationState creationState) {
		throw new SemanticException(
				String.format(
						Locale.ROOT,
						"Static field reference [%s#%s] cannot be de-referenced",
						fieldOwnerJavaTypeDescriptor.getJavaType().getName(),
						fieldName
				)
		);
	}

	@Override
	public SqmPath resolveIndexedAccess(
			SqmExpression selector,
			boolean isTerminal,
			SqmCreationState creationState) {
		throw new SemanticException(
				String.format(
						Locale.ROOT,
						"Static field reference [%s#%s] cannot be de-referenced",
						fieldOwnerJavaTypeDescriptor.getJavaType().getName(),
						fieldName
				)
		);
	}


	@Override
	public boolean isCompoundSelection() {
		return false;
	}

	@Override
	public List<? extends JpaSelection<?>> getSelectionItems() {
		// per-JPA
		throw new IllegalStateException( "Not a compound selection" );
	}

	@Override
	public JpaSelection<T> alias(String name) {
		return null;
	}

	@Override
	public String getAlias() {
		return null;
	}
}
