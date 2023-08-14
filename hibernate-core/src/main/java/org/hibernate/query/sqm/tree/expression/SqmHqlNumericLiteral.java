/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.query.sqm.tree.expression;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Locale;

import org.hibernate.HibernateException;
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
public class SqmHqlNumericLiteral<N extends Number> extends AbstractSqmExpression<N> {
	private final String literalValue;
	private final TypeCategory typeCategory;

	public SqmHqlNumericLiteral(
			String literalValue,
			TypeCategory typeCategory,
			BasicDomainType<N> type,
			NodeBuilder criteriaBuilder) {
		super( type, criteriaBuilder );
		this.literalValue = literalValue;
		this.typeCategory = typeCategory;
	}

	public SqmHqlNumericLiteral(
			String literalValue,
			BasicDomainType<N> type,
			NodeBuilder criteriaBuilder) {
		this( literalValue, interpretCategory( literalValue, type ), type, criteriaBuilder );
	}

	public String getLiteralValue() {
		return literalValue;
	}

	public TypeCategory getTypeCategory() {
		return typeCategory;
	}

	@Override
	public BasicDomainType<N> getNodeType() {
		return (BasicDomainType<N>) super.getNodeType();
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
	public void appendHqlString(StringBuilder sb) {
		sb.append( literalValue );

		switch ( typeCategory ) {
			case BIG_DECIMAL: {
				sb.append( "bd" );
				break;
			}
			case FLOAT: {
				sb.append( "f" );
				break;
			}
			case BIG_INTEGER: {
				sb.append( "bi" );
				break;
			}
			case LONG: {
				sb.append( "l" );
				break;
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

	public enum TypeCategory {
		INTEGER,
		LONG,
		BIG_INTEGER,
		DOUBLE,
		FLOAT,
		BIG_DECIMAL
	}

	private static <N extends Number> TypeCategory interpretCategory(String literalValue, SqmExpressible<N> type) {
		assert type != null;

		final JavaType<N> javaTypeDescriptor = type.getExpressibleJavaType();
		assert javaTypeDescriptor != null;

		final Class<N> javaTypeClass = javaTypeDescriptor.getJavaTypeClass();

		if ( BigDecimal.class.equals( javaTypeClass ) ) {
			return TypeCategory.BIG_DECIMAL;
		}

		if ( Double.class.equals( javaTypeClass ) ) {
			return TypeCategory.DOUBLE;
		}

		if ( Float.class.equals( javaTypeClass ) ) {
			return TypeCategory.FLOAT;
		}

		if ( BigInteger.class.equals( javaTypeClass ) ) {
			return TypeCategory.BIG_INTEGER;
		}

		if ( Long.class.equals( javaTypeClass ) ) {
			return TypeCategory.LONG;
		}

		if ( Short.class.equals( javaTypeClass )
				|| Integer.class.equals( javaTypeClass ) ) {
			return TypeCategory.INTEGER;
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
