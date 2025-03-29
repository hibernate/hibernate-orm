/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Locale;

import org.hibernate.HibernateException;
import org.hibernate.internal.util.NullnessUtil;
import org.hibernate.metamodel.model.domain.BasicDomainType;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * Used to model numeric literals found in HQL queries.
 * <p/>
 * Used instead of {@link SqmLiteral} which would require parsing the
 * literal value to the specified number type to avoid loss of precision
 * due to Float and Double being non-exact types.
 *
 * @apiNote Only used for HQL literals because we do not have this problem
 * with criteria queries where the value given us by user would already be
 * typed.
 *
 * @author Steve Ebersole
 */
public class SqmHqlNumericLiteral<N extends Number> extends SqmLiteral<N> {
	private final String literalValue;
	private final NumericTypeCategory typeCategory;

	public SqmHqlNumericLiteral(
			String literalValue,
			BasicDomainType<N> type,
			NodeBuilder criteriaBuilder) {
		this( literalValue, interpretCategory( literalValue, type ), type, criteriaBuilder );
	}

	public SqmHqlNumericLiteral(
			String literalValue,
			NumericTypeCategory typeCategory,
			BasicDomainType<N> type,
			NodeBuilder criteriaBuilder) {
		super( type, criteriaBuilder );
		this.literalValue = literalValue;
		this.typeCategory = typeCategory;
	}

	public String getUnparsedLiteralValue() {
		return literalValue;
	}

	@Override
	public N getLiteralValue() {
		return typeCategory.parseLiteralValue( literalValue );
	}

	public NumericTypeCategory getTypeCategory() {
		return typeCategory;
	}

	@Override
	public BasicDomainType<N> getNodeType() {
		return (BasicDomainType<N>) NullnessUtil.castNonNull( super.getNodeType() );
	}

	@Override
	public BasicDomainType<N> getExpressible() {
		return getNodeType();
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitHqlNumericLiteral( this );
	}

	@Override
	public void appendHqlString(StringBuilder hql) {
		hql.append( literalValue );

		switch ( typeCategory ) {
			case BIG_DECIMAL: {
				hql.append( "bd" );
				break;
			}
			case FLOAT: {
				hql.append( "f" );
				break;
			}
			case BIG_INTEGER: {
				hql.append( "bi" );
				break;
			}
			case LONG: {
				hql.append( "l" );
				break;
			}
			default: {
				// nothing to do for double/integer
			}
		}
	}

	@Override
	public String asLoggableText() {
		final StringBuilder stringBuilder = new StringBuilder();
		appendHqlString( stringBuilder );
		return stringBuilder.toString();
	}

	@Override
	public SqmHqlNumericLiteral<N> copy(SqmCopyContext context) {
		return new SqmHqlNumericLiteral<>( literalValue, typeCategory, getExpressible(), nodeBuilder() );
	}

	private static <N extends Number> NumericTypeCategory interpretCategory(String literalValue, SqmExpressible<N> type) {
		assert type != null;

		final JavaType<N> javaTypeDescriptor = type.getExpressibleJavaType();
		assert javaTypeDescriptor != null;

		final Class<N> javaTypeClass = javaTypeDescriptor.getJavaTypeClass();

		if ( BigDecimal.class.equals( javaTypeClass ) ) {
			return NumericTypeCategory.BIG_DECIMAL;
		}

		if ( Double.class.equals( javaTypeClass ) ) {
			return NumericTypeCategory.DOUBLE;
		}

		if ( Float.class.equals( javaTypeClass ) ) {
			return NumericTypeCategory.FLOAT;
		}

		if ( BigInteger.class.equals( javaTypeClass ) ) {
			return NumericTypeCategory.BIG_INTEGER;
		}

		if ( Long.class.equals( javaTypeClass ) ) {
			return NumericTypeCategory.LONG;
		}

		if ( Short.class.equals( javaTypeClass )
				|| Integer.class.equals( javaTypeClass ) ) {
			return NumericTypeCategory.INTEGER;
		}

		throw new TypeException( literalValue, javaTypeClass );
	}

	public static class TypeException extends HibernateException {
		public TypeException(String literalValue, Class<?> javaType) {
			super(
					String.format(
							Locale.ROOT,
							"Unexpected Java type [%s] for numeric literal - %s",
							javaType.getTypeName(),
							literalValue
					)
			);
		}
	}
}
