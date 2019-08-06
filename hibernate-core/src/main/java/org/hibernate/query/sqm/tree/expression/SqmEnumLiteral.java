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
import org.hibernate.query.sqm.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.sql.results.spi.DomainResultProducer;
import org.hibernate.type.descriptor.java.EnumJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * Specialized SQM literal defined by an enum reference.  E.g.
 * {@code ".. where p.sex = Sex.MALE"}
 *
 * @author Steve Ebersole
 */
public class SqmEnumLiteral implements SqmExpression<Enum>, SqmExpressable<Enum>, DomainResultProducer<Enum>, SemanticPathPart {
	private final Enum enumValue;
	private final EnumJavaTypeDescriptor<Enum> referencedEnumTypeDescriptor;
	private final String enumValueName;
	private final NodeBuilder nodeBuilder;

	private SqmExpressable<Enum> expressable;

	public SqmEnumLiteral(
			Enum enumValue,
			EnumJavaTypeDescriptor<Enum> referencedEnumTypeDescriptor,
			String enumValueName,
			NodeBuilder nodeBuilder) {
		this.enumValue = enumValue;
		this.referencedEnumTypeDescriptor = referencedEnumTypeDescriptor;
		this.enumValueName = enumValueName;
		this.nodeBuilder = nodeBuilder;

		this.expressable = this;
	}

	public Enum getEnumValue() {
		return enumValue;
	}

	public String getEnumValueName() {
		return enumValueName;
	}

	@Override
	public EnumJavaTypeDescriptor<Enum> getExpressableJavaTypeDescriptor() {
		return referencedEnumTypeDescriptor;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SemanticPathPart

	@Override
	public SemanticPathPart resolvePathPart(
			String name,
			boolean isTerminal,
			SqmCreationState creationState) {
		throw new SemanticException(
				String.format(
						Locale.ROOT,
						"Static enum reference [%s#%s] cannot be de-referenced",
						referencedEnumTypeDescriptor.getJavaType().getName(),
						enumValueName
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
						"Static enum reference [%s#%s] cannot be de-referenced",
						referencedEnumTypeDescriptor.getJavaType().getName(),
						enumValueName
				)
		);
	}

	@Override
	public SqmExpressable<Enum> getNodeType() {
		return expressable;
	}

	@Override
	public void applyInferableType(SqmExpressable<?> type) {
		//noinspection unchecked
		this.expressable = (SqmExpressable) type;
	}

	@Override
	public SqmExpression<Long> asLong() {
		return nodeBuilder().literal( getExpressableJavaTypeDescriptor().toOrdinal( enumValue ).longValue() );
	}

	@Override
	public SqmExpression<Integer> asInteger() {
		return nodeBuilder().literal( getExpressableJavaTypeDescriptor().toOrdinal( enumValue ) );
	}

	@Override
	public SqmExpression<Float> asFloat() {
		return nodeBuilder().literal( getExpressableJavaTypeDescriptor().toOrdinal( enumValue ).floatValue() );
	}

	@Override
	public SqmExpression<Double> asDouble() {
		return nodeBuilder().literal( getExpressableJavaTypeDescriptor().toOrdinal( enumValue ).doubleValue() );
	}

	@Override
	public SqmExpression<BigDecimal> asBigDecimal() {
		throw new UnsupportedOperationException( "Enum literal cannot be cast to BigDecimal" );
	}

	@Override
	public SqmExpression<BigInteger> asBigInteger() {
		throw new UnsupportedOperationException( "Enum literal cannot be cast to BigInteger" );
	}

	@Override
	public SqmExpression<String> asString() {
		return nodeBuilder().literal( getExpressableJavaTypeDescriptor().toName( enumValue ) );
	}

	@Override
	public <X> SqmExpression<X> as(Class<X> type) {
		return nodeBuilder().cast( this, type );
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
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitEnumLiteral( this );
	}

	@Override
	public JavaTypeDescriptor<Enum> getJavaTypeDescriptor() {
		return getExpressableJavaTypeDescriptor();
	}

	@Override
	public NodeBuilder nodeBuilder() {
		return nodeBuilder;
	}

	@Override
	public List<? extends JpaSelection<?>> getSelectionItems() {
		// per-JPA
		throw new IllegalStateException( "Not a compound selection" );
	}

	@Override
	public boolean isCompoundSelection() {
		return false;
	}

	@Override
	public JpaSelection<Enum> alias(String name) {
		return null;
	}

	@Override
	public String getAlias() {
		return null;
	}

	@Override
	public DomainResultProducer<Enum> getDomainResultProducer() {
		return this;
	}
}
