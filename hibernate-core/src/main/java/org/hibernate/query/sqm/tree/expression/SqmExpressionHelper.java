/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetTime;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.model.domain.internal.EmbeddedSqmPathSource;
import org.hibernate.query.BindableType;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.BinaryArithmeticOperator;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.type.descriptor.java.JavaTypeHelper;
import org.hibernate.type.descriptor.java.JdbcDateJavaType;
import org.hibernate.type.descriptor.java.JdbcTimeJavaType;
import org.hibernate.type.descriptor.java.JdbcTimestampJavaType;
import org.hibernate.usertype.internal.AbstractTimeZoneStorageCompositeUserType;
import org.hibernate.usertype.internal.OffsetTimeCompositeUserType;

/**
 * @author Steve Ebersole
 */
public class SqmExpressionHelper {
	public static <T> SqmExpressible<T> toSqmType(BindableType<T> parameterType, SqmCreationState creationState) {
		return toSqmType( parameterType, creationState.getCreationContext().getNodeBuilder().getSessionFactory() );
	}

	public static <T> SqmExpressible<T> toSqmType(BindableType<T> anticipatedType, NodeBuilder nodeBuilder) {
		return toSqmType( anticipatedType, nodeBuilder.getSessionFactory() );
	}

//	public static <T> SqmExpressible<T> toSqmType(BindableType<T> anticipatedType, TypeConfiguration typeConfiguration) {
//		return toSqmType( anticipatedType, typeConfiguration.getSessionFactory() );
//	}

	public static <T> SqmExpressible<T> toSqmType(BindableType<T> anticipatedType, SessionFactoryImplementor sessionFactory) {
		if ( anticipatedType == null ) {
			return null;
		}
		final SqmExpressible<T> sqmExpressible = anticipatedType.resolveExpressible( sessionFactory );
		assert sqmExpressible != null;

		return sqmExpressible;

	}

	public static SqmLiteral<Timestamp> timestampLiteralFrom(String literalText, SqmCreationState creationState) {
		final Timestamp literal = Timestamp.valueOf(
				LocalDateTime.from( JdbcTimestampJavaType.LITERAL_FORMATTER.parse( literalText ) )
		);

		return new SqmLiteral<>(
				literal,
				creationState.getCreationContext().getTypeConfiguration().standardBasicTypeForJavaType( Timestamp.class ),
				creationState.getCreationContext().getNodeBuilder()
		);
	}

	public static SqmLiteral<Integer> integerLiteral(String literalText, SqmCreationState creationState) {
		return integerLiteral( literalText, creationState.getCreationContext().getQueryEngine() );
	}

	public static SqmLiteral<Integer> integerLiteral(String literalText, QueryEngine queryEngine) {
		return integerLiteral( Integer.parseInt( literalText ), queryEngine );
	}

	public static SqmLiteral<Integer> integerLiteral(int value, QueryEngine queryEngine) {
		return new SqmLiteral<>(
				value,
				queryEngine.getCriteriaBuilder().getIntegerType(),
				queryEngine.getCriteriaBuilder()
		);
	}

	public static SqmLiteral<Date> dateLiteralFrom(String literalText, SqmCreationState creationState) {
		final LocalDate localDate = LocalDate.from( JdbcDateJavaType.LITERAL_FORMATTER.parse( literalText ) );
		final Date literal = new Date( localDate.toEpochDay() );

		return new SqmLiteral<>(
				literal,
				creationState.getCreationContext().getTypeConfiguration().standardBasicTypeForJavaType( Date.class ),
				creationState.getCreationContext().getNodeBuilder()
		);
	}

	public static SqmLiteral<Time> timeLiteralFrom(String literalText, SqmCreationState creationState) {
		final LocalTime localTime = LocalTime.from( JdbcTimeJavaType.LITERAL_FORMATTER.parse( literalText ) );
		final Time literal = Time.valueOf( localTime );

		return new SqmLiteral<>(
				literal,
				creationState.getCreationContext().getTypeConfiguration().standardBasicTypeForJavaType( Time.class ),
				creationState.getCreationContext().getNodeBuilder()
		);
	}

	public static boolean isCompositeTemporal(SqmExpression<?> expression) {
		// When TimeZoneStorageStrategy.COLUMN is used, that implies using a composite user type
		return expression instanceof SqmPath<?> && expression.getNodeType() instanceof EmbeddedSqmPathSource<?>
				&& JavaTypeHelper.isTemporal( expression.getJavaTypeDescriptor() );
	}

	public static SqmExpression<?> getActualExpression(SqmExpression<?> expression) {
		if ( isCompositeTemporal( expression ) ) {
			if ( expression.getJavaTypeDescriptor().getJavaTypeClass() == OffsetTime.class ) {
				return ( (SqmPath<?>) expression ).get( OffsetTimeCompositeUserType.LOCAL_TIME_NAME );
			}
			else {
				return ( (SqmPath<?>) expression ).get( AbstractTimeZoneStorageCompositeUserType.INSTANT_NAME );
			}
		}
		else {
			return expression;
		}
	}

	public static SqmExpression<?> getOffsetAdjustedExpression(SqmExpression<?> expression) {
		if ( isCompositeTemporal( expression ) ) {
			final SqmPath<?> compositePath = (SqmPath<?>) expression;
			final SqmPath<Object> temporalPath;
			if ( expression.getJavaTypeDescriptor().getJavaTypeClass() == OffsetTime.class ) {
				temporalPath = compositePath.get( OffsetTimeCompositeUserType.LOCAL_TIME_NAME );
			}
			else {
				temporalPath = compositePath.get( AbstractTimeZoneStorageCompositeUserType.INSTANT_NAME );
			}
			final NodeBuilder nodeBuilder = temporalPath.nodeBuilder();
			return new SqmBinaryArithmetic<>(
					BinaryArithmeticOperator.ADD,
					temporalPath,
					new SqmToDuration<>(
							compositePath.get( AbstractTimeZoneStorageCompositeUserType.ZONE_OFFSET_NAME ),
							new SqmDurationUnit<>( TemporalUnit.SECOND, nodeBuilder.getIntegerType(), nodeBuilder ),
							nodeBuilder.getTypeConfiguration().getBasicTypeForJavaType( Duration.class ),
							nodeBuilder
					),
					temporalPath.getNodeType(),
					nodeBuilder
			);
		}
		else {
			return expression;
		}
	}

	public static SqmPath<?> findPath(SqmExpression<?> expression, SqmExpressible<?> nodeType) {
		if ( nodeType != expression.getNodeType() ) {
			return null;
		}
		if ( expression instanceof SqmPath<?> ) {
			return (SqmPath<?>) expression;
		}
		else if ( expression instanceof SqmBinaryArithmetic<?> ) {
			final SqmBinaryArithmetic<?> binaryArithmetic = (SqmBinaryArithmetic<?>) expression;
			final SqmPath<?> lhs = findPath( binaryArithmetic.getLeftHandOperand(), nodeType );
			if ( lhs != null ) {
				return lhs;
			}
			final SqmPath<?> rhs = findPath( binaryArithmetic.getRightHandOperand(), nodeType );
			return rhs;
		}
		return null;
	}
}
