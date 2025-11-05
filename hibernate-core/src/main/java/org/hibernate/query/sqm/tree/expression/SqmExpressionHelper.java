/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.metamodel.model.domain.internal.EmbeddedSqmPathSource;
import org.hibernate.type.BindableType;
import org.hibernate.type.BindingContext;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.BinaryArithmeticOperator;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmBindableType;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.type.descriptor.java.JavaTypeHelper;
import org.hibernate.usertype.internal.AbstractTimeZoneStorageCompositeUserType;
import org.hibernate.usertype.internal.OffsetTimeCompositeUserType;

import java.time.Duration;
import java.time.OffsetTime;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;

/**
 * @author Steve Ebersole
 */
public class SqmExpressionHelper {
	public static <T> @Nullable SqmExpressible<T> toSqmType(@Nullable BindableType<T> parameterType, SqmCreationState creationState) {
		return toSqmType( parameterType, creationState.getCreationContext() );
	}

	public static <T> @Nullable SqmBindableType<T> toSqmType(
			@Nullable BindableType<T> anticipatedType, BindingContext bindingContext) {
		if ( anticipatedType == null ) {
			return null;
		}
		else {
			final SqmBindableType<T> sqmExpressible = bindingContext.resolveExpressible( anticipatedType );
			assert sqmExpressible != null;
			return sqmExpressible;
		}
	}

//	public static SqmLiteral<Timestamp> timestampLiteralFrom(String literalText, SqmCreationState creationState) {
//		final Timestamp literal = Timestamp.valueOf(
//				LocalDateTime.from( JdbcTimestampJavaType.LITERAL_FORMATTER.parse( literalText ) )
//		);
//
//		final SqmCreationContext creationContext = creationState.getCreationContext();
//		return new SqmLiteral<>(
//				literal,
//				creationContext.getTypeConfiguration().standardBasicTypeForJavaType( Timestamp.class ),
//				creationContext.getNodeBuilder()
//		);
//	}

//	public static SqmLiteral<Integer> integerLiteral(String literalText, SqmCreationState creationState) {
//		return integerLiteral( literalText, creationState.getCreationContext().getQueryEngine() );
//	}

//	public static SqmLiteral<Integer> integerLiteral(String literalText, QueryEngine queryEngine) {
//		return integerLiteral( Integer.parseInt( literalText ), queryEngine );
//	}

//	public static SqmLiteral<Integer> integerLiteral(int value, QueryEngine queryEngine) {
//		final NodeBuilder nodeBuilder = queryEngine.getCriteriaBuilder();
//		return new SqmLiteral<>( value, nodeBuilder.getIntegerType(), nodeBuilder );
//	}

//	public static SqmLiteral<Date> dateLiteralFrom(String literalText, SqmCreationState creationState) {
//		final LocalDate localDate = LocalDate.from( JdbcDateJavaType.LITERAL_FORMATTER.parse( literalText ) );
//		final Date literal = new Date( localDate.toEpochDay() );
//
//		final SqmCreationContext creationContext = creationState.getCreationContext();
//		return new SqmLiteral<>(
//				literal,
//				creationContext.getTypeConfiguration().standardBasicTypeForJavaType( Date.class ),
//				creationContext.getNodeBuilder()
//		);
//	}

//	public static SqmLiteral<Time> timeLiteralFrom(String literalText, SqmCreationState creationState) {
//		final LocalTime localTime = LocalTime.from( JdbcTimeJavaType.LITERAL_FORMATTER.parse( literalText ) );
//		final Time literal = Time.valueOf( localTime );
//
//		final SqmCreationContext creationContext = creationState.getCreationContext();
//		return new SqmLiteral<>(
//				literal,
//				creationContext.getTypeConfiguration().standardBasicTypeForJavaType( Time.class ),
//				creationContext.getNodeBuilder()
//		);
//	}

	public static boolean isCompositeTemporal(SqmExpression<?> expression) {
		// When TimeZoneStorageStrategy.COLUMN is used, that implies using a composite user type
		return expression instanceof SqmPath<?> path
			&& path.getReferencedPathSource() instanceof EmbeddedSqmPathSource
			&& JavaTypeHelper.isTemporal( expression.getJavaTypeDescriptor() );
	}

	public static SqmExpression<?> getActualExpression(SqmExpression<?> expression) {
		if ( isCompositeTemporal( expression ) ) {
			final SqmPath<?> path = (SqmPath<?>) expression;
			return castNonNull( expression.getJavaTypeDescriptor() ).getJavaTypeClass() == OffsetTime.class
					? path.get( OffsetTimeCompositeUserType.LOCAL_TIME_NAME )
					: path.get( AbstractTimeZoneStorageCompositeUserType.INSTANT_NAME );
		}
		else {
			return expression;
		}
	}

	public static SqmExpression<?> getOffsetAdjustedExpression(SqmExpression<?> expression) {
		if ( isCompositeTemporal( expression ) ) {
			final SqmPath<?> compositePath = (SqmPath<?>) expression;
			final SqmPath<Object> temporalPath =
					castNonNull( expression.getJavaTypeDescriptor() ).getJavaTypeClass() == OffsetTime.class
							? compositePath.get( OffsetTimeCompositeUserType.LOCAL_TIME_NAME )
							: compositePath.get( AbstractTimeZoneStorageCompositeUserType.INSTANT_NAME );
			final NodeBuilder nodeBuilder = temporalPath.nodeBuilder();
			return new SqmBinaryArithmetic<>(
					BinaryArithmeticOperator.ADD,
					temporalPath,
					new SqmToDuration<>(
							compositePath.get( AbstractTimeZoneStorageCompositeUserType.ZONE_OFFSET_NAME ),
							new SqmDurationUnit<>( TemporalUnit.SECOND, nodeBuilder.getIntegerType(), nodeBuilder ),
							castNonNull( nodeBuilder.getTypeConfiguration().getBasicTypeForJavaType( Duration.class ) ),
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

//	public static SqmPath<?> findPath(SqmExpression<?> expression, SqmExpressible<?> nodeType) {
//		if ( nodeType != expression.getNodeType() ) {
//			return null;
//		}
//		else if ( expression instanceof SqmPath<?> sqmPath ) {
//			return sqmPath;
//		}
//		else if ( expression instanceof SqmBinaryArithmetic<?> binaryArithmetic ) {
//			final SqmPath<?> lhs = findPath( binaryArithmetic.getLeftHandOperand(), nodeType );
//			if ( lhs != null ) {
//				return lhs;
//			}
//			final SqmPath<?> rhs = findPath( binaryArithmetic.getRightHandOperand(), nodeType );
//			return rhs;
//		}
//		return null;
//	}
}
