/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function;

import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.type.BindingContext;
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.produce.function.FunctionArgumentException;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.tree.SqmTypedNode;

import java.util.List;
import java.util.Locale;

/**
 * A {@link ArgumentsValidator} that validates the array type is compatible with the element type.
 */
public class GenerateSeriesArgumentValidator implements ArgumentsValidator {

	private final ArgumentsValidator delegate;

	public GenerateSeriesArgumentValidator() {
		this.delegate = StandardArgumentsValidators.between( 2, 3 );
	}

	@Override
	public void validate(
			List<? extends SqmTypedNode<?>> arguments,
			String functionName,
			BindingContext bindingContext) {
		delegate.validate( arguments, functionName, bindingContext );

		final var start = arguments.get( 0 );
		final var stop = arguments.get( 1 );
		final var step = arguments.size() > 2 ? arguments.get( 2 ) : null;

		final var startExpressible = start.getExpressible();
		final var stopExpressible = stop.getExpressible();
		final var stepExpressible = step == null ? null : step.getExpressible();

		final var startType = startExpressible == null ? null : startExpressible.getSqmType();
		final var stopType = stopExpressible == null ? null : stopExpressible.getSqmType();
		final var stepType = stepExpressible == null ? null : stepExpressible.getSqmType();

		if ( startType == null ) {
			throw unknownType( functionName, arguments, 0 );
		}
		if ( stopType == null ) {
			throw unknownType( functionName, arguments, 1 );
		}

		if ( startType != stopType ) {
			throw new FunctionArgumentException(
					String.format(
							"Start and stop parameters of function '%s()' must be of the same type, but found [%s,%s]",
							functionName,
							startType.getTypeName(),
							stopType.getTypeName()
					)
			);
		}
		final var type = (JdbcMapping) startType;
		final var jdbcType = type.getJdbcType();
		if ( jdbcType.isInteger() || jdbcType.isDecimal() ) {
			if ( step != null ) {
				if ( stepType == null ) {
					throw unknownType( functionName, arguments, 2 );
				}
				if ( stepType != startType ) {
					throw new FunctionArgumentException(
							String.format(
									"Step parameter of function '%s()' is of type '%s', but must be of the same type as start and stop [%s,%s]",
									functionName,
									stepType.getTypeName(),
									startType.getTypeName(),
									stopType.getTypeName()
							)
					);
				}
			}
		}
		else if ( jdbcType.isTemporal() ) {
			if ( step == null ) {
				throw new FunctionArgumentException(
						String.format(
								Locale.ROOT,
								"Function %s() requires exactly 3 arguments when invoked with a temporal argument, but %d arguments given",
								functionName,
								arguments.size()
						)
				);
			}
			if ( stepType == null ) {
				throw unknownType( functionName, arguments, 2 );
			}
			final var stepJdbcType = ((JdbcMapping) stepType).getJdbcType();
			if ( !stepJdbcType.isInterval() && !stepJdbcType.isDuration() ) {
				throw new FunctionArgumentException(
						String.format(
								"Step parameter of function '%s()' is of type '%s', but must be of type interval",
								functionName,
								stepType.getTypeName()
						)
				);
			}
		}
		else {
			throw new FunctionArgumentException(
					String.format(
							"Unsupported type '%s' for function '%s()'. Only integral, decimal and timestamp types are supported.",
							startType.getTypeName(),
							functionName
					)
			);
		}
	}

	private FunctionArgumentException unknownType(String functionName, List<? extends SqmTypedNode<?>> arguments, int parameterIndex) {
		return new FunctionArgumentException(
				String.format(
						"Couldn't determine type of parameter %d of function '%s()'. Argument is '%s'",
						parameterIndex,
						functionName,
						arguments.get( parameterIndex )
				)
		);
	}
}
