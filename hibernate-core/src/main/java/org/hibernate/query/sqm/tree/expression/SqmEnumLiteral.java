/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Locale;

import org.hibernate.query.hql.spi.SemanticPathPart;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmBindableType;
import org.hibernate.query.sqm.UnknownPathException;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmRenderContext;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.domain.SqmDomainType;
import org.hibernate.type.descriptor.java.EnumJavaType;

import static jakarta.persistence.metamodel.Type.PersistenceType.BASIC;

/**
 * Specialized SQM literal defined by an enum reference.  E.g.
 * {@code ".. where p.sex = Sex.MALE"}
 *
 * @author Steve Ebersole
 */
public class SqmEnumLiteral<E extends Enum<E>> extends SqmLiteral<E> implements SqmBindableType<E>, SemanticPathPart {
	private final E enumValue;
	private final EnumJavaType<E> referencedEnumTypeDescriptor;
	private final String enumValueName;

	public SqmEnumLiteral(
			E enumValue,
			EnumJavaType<E> referencedEnumTypeDescriptor,
			String enumValueName,
			NodeBuilder nodeBuilder) {
		super( null, nodeBuilder );
		this.enumValue = enumValue;
		this.referencedEnumTypeDescriptor = referencedEnumTypeDescriptor;
		this.enumValueName = enumValueName;
		setExpressibleType( this );
	}

	@Override
	public SqmEnumLiteral<E> copy(SqmCopyContext context) {
		final SqmEnumLiteral<E> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmEnumLiteral<E> expression = context.registerCopy(
				this,
				new SqmEnumLiteral<>(
						enumValue,
						referencedEnumTypeDescriptor,
						enumValueName,
						nodeBuilder()
				)
		);
		copyTo( expression, context );
		return expression;
	}

	@Override
	public SqmBindableType<E> getExpressible() {
		return this;
	}

	@Override
	public PersistenceType getPersistenceType() {
		return BASIC;
	}

	@Override
	public SqmDomainType<E> getSqmType() {
		return null;
	}

	public E getEnumValue() {
		return enumValue;
	}

	@Override
	public EnumJavaType<E> getExpressibleJavaType() {
		return referencedEnumTypeDescriptor;
	}

	@Override
	public Class<E> getJavaType() {
		return getJavaTypeDescriptor().getJavaTypeClass();
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SemanticPathPart

	@Override
	public SemanticPathPart resolvePathPart(
			String name,
			boolean isTerminal,
			SqmCreationState creationState) {
		throw new UnknownPathException(
				String.format(
						Locale.ROOT,
						"Static enum reference [%s#%s] cannot be de-referenced",
						referencedEnumTypeDescriptor.getTypeName(),
						enumValueName
				)
		);
	}

	@Override
	public SqmPath<?> resolveIndexedAccess(
			SqmExpression<?> selector,
			boolean isTerminal,
			SqmCreationState creationState) {
		throw new UnknownPathException(
				String.format(
						Locale.ROOT,
						"Static enum reference [%s#%s] cannot be de-referenced",
						referencedEnumTypeDescriptor.getTypeName(),
						enumValueName
				)
		);
	}

	private Integer ordinalValue() {
		return getExpressibleJavaType().toOrdinal( enumValue );
	}

	@Override
	public SqmExpression<Long> asLong() {
		return nodeBuilder().literal( ordinalValue().longValue() );
	}

	@Override
	public SqmExpression<Integer> asInteger() {
		return nodeBuilder().literal( ordinalValue() );
	}

	@Override
	public SqmExpression<Float> asFloat() {
		return nodeBuilder().literal( ordinalValue().floatValue() );
	}

	@Override
	public SqmExpression<Double> asDouble() {
		return nodeBuilder().literal( ordinalValue().doubleValue() );
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
		return nodeBuilder().literal( getExpressibleJavaType().toName( enumValue ) );
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitEnumLiteral( this );
	}

	@Override
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		hql.append( enumValue.getDeclaringClass().getTypeName() );
		hql.append( '.' );
		hql.append( enumValueName );
	}
}
