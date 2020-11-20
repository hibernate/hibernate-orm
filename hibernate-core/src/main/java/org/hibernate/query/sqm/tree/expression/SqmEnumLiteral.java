/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Locale;

import org.hibernate.query.SemanticException;
import org.hibernate.query.hql.spi.SemanticPathPart;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmExpressable;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.type.descriptor.java.EnumJavaTypeDescriptor;

/**
 * Specialized SQM literal defined by an enum reference.  E.g.
 * {@code ".. where p.sex = Sex.MALE"}
 *
 * @author Steve Ebersole
 */
public class SqmEnumLiteral<E extends Enum<E>> extends AbstractSqmExpression<E> implements SqmExpressable<E>, SemanticPathPart {
	private final E enumValue;
	private final EnumJavaTypeDescriptor<E> referencedEnumTypeDescriptor;
	private final String enumValueName;

	public SqmEnumLiteral(
			E enumValue,
			EnumJavaTypeDescriptor<E> referencedEnumTypeDescriptor,
			String enumValueName,
			NodeBuilder nodeBuilder) {
		super( null, nodeBuilder );
		this.enumValue = enumValue;
		this.referencedEnumTypeDescriptor = referencedEnumTypeDescriptor;
		this.enumValueName = enumValueName;
		setExpressableType( this );
	}

	public Enum getEnumValue() {
		return enumValue;
	}

	public String getEnumValueName() {
		return enumValueName;
	}

	@Override
	public EnumJavaTypeDescriptor<E> getExpressableJavaTypeDescriptor() {
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
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitEnumLiteral( this );
	}

}
