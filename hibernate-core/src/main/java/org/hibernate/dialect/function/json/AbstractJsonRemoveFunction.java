/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function.json;

import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.function.FunctionKind;
import org.hibernate.query.sqm.produce.function.ArgumentTypesValidator;
import org.hibernate.query.sqm.produce.function.FunctionParameterType;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Standard json_remove function.
 */
public abstract class AbstractJsonRemoveFunction extends AbstractSqmSelfRenderingFunctionDescriptor {

	public AbstractJsonRemoveFunction(TypeConfiguration typeConfiguration) {
		super(
				"json_remove",
				FunctionKind.NORMAL,
				new ArgumentTypesValidator(
						StandardArgumentsValidators.exactly( 2 ),
						FunctionParameterType.IMPLICIT_JSON,
						FunctionParameterType.STRING
				),
				StandardFunctionReturnTypeResolvers.invariant(
						typeConfiguration.getBasicTypeRegistry().resolve( String.class, SqlTypes.JSON )
				),
				null
		);
	}

}
