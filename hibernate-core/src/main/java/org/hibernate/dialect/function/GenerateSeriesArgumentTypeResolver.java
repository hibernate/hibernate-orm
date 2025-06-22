/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.produce.function.internal.AbstractFunctionArgumentTypeResolver;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.type.BasicType;

import java.time.Duration;
import java.util.List;

/**
 * A {@link ArgumentsValidator} that validates the array type is compatible with the element type.
 */
public class GenerateSeriesArgumentTypeResolver extends AbstractFunctionArgumentTypeResolver {

	private final BasicType<Duration> durationType;

	public GenerateSeriesArgumentTypeResolver(BasicType<Duration> durationType) {
		this.durationType = durationType;
	}

	@Override
	public @Nullable MappingModelExpressible<?> resolveFunctionArgumentType(List<? extends SqmTypedNode<?>> arguments, int argumentIndex, SqmToSqlAstConverter converter) {
		if ( argumentIndex == 0 ) {
			final MappingModelExpressible<?> mappingModelExpressible = converter.resolveFunctionImpliedReturnType();
			return mappingModelExpressible != null
					? mappingModelExpressible
					: converter.determineValueMapping( (SqmExpression<?>) arguments.get( 1 ) );
		}
		else if ( argumentIndex == 1 ) {
			final MappingModelExpressible<?> mappingModelExpressible = converter.resolveFunctionImpliedReturnType();
			return mappingModelExpressible != null
					? mappingModelExpressible
					: converter.determineValueMapping( (SqmExpression<?>) arguments.get( 0 ) );
		}
		else {
			assert argumentIndex == 2;
			final MappingModelExpressible<?> implied = converter.resolveFunctionImpliedReturnType();
			final MappingModelExpressible<?> firstType;
			final MappingModelExpressible<?> resultType;
			if ( implied != null ) {
				resultType = implied;
			}
			else if ( (firstType = converter.determineValueMapping( (SqmExpression<?>) arguments.get( 0 ) )) != null ) {
				resultType = firstType;
			}
			else {
				resultType = converter.determineValueMapping( (SqmExpression<?>) arguments.get( 1 ) );
			}

			assert resultType != null;
			if ( resultType.getSingleJdbcMapping().getJdbcType().isTemporal() ) {
				return durationType;
			}
			else {
				return resultType;
			}
		}
	}
}
