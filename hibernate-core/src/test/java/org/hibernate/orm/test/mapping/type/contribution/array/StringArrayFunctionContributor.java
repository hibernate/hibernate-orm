/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.type.contribution.array;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.query.sqm.function.NamedSqmFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

public class StringArrayFunctionContributor implements FunctionContributor {

	@Override
	public void contributeFunctions(FunctionContributions functionContributions) {
		TypeConfiguration typeConfiguration = functionContributions.getTypeConfiguration();
		functionContributions.getFunctionRegistry().register(
				"array_length",
				new NamedSqmFunctionDescriptor(
						"array_length",
						true,
						StandardArgumentsValidators.exactly( 1 ),
						StandardFunctionReturnTypeResolvers.invariant(
								typeConfiguration.getBasicTypeRegistry()
										.resolve( StandardBasicTypes.INTEGER ) )
				)
		);
	}
}
