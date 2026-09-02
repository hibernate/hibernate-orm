/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.array;

import org.hibernate.SPI;
import org.hibernate.internal.log.DeprecationLogger;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.type.spi.TypeConfiguration;

import org.jboss.logging.Logger;

import java.lang.invoke.MethodHandles;
import java.util.Locale;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.USE;

/// Base descriptor for `array_contains`, providing its validation and type
/// resolution while subclasses implement database-specific rendering.
@SPI({ USE, IMPLEMENT })
public abstract class AbstractArrayContainsFunction extends AbstractSqmSelfRenderingFunctionDescriptor {

	private static final DeprecationLogger LOG = Logger.getMessageLogger( MethodHandles.lookup(), DeprecationLogger.class, AbstractArrayContainsFunction.class.getName(), Locale.ROOT );

	protected final boolean nullable;

	@SPI(IMPLEMENT)
	public AbstractArrayContainsFunction(boolean nullable, TypeConfiguration typeConfiguration) {
		super(
				"array_contains" + ( nullable ? "_nullable" : "" ),
				StandardArgumentsValidators.composite(
						StandardArgumentsValidators.exactly( 2 ),
						ArrayContainsArgumentValidator.INSTANCE
				),
				StandardFunctionReturnTypeResolvers.invariant( typeConfiguration.standardBasicTypeForJavaType( Boolean.class ) ),
				ArrayContainsArgumentTypeResolver.INSTANCE
		);
		this.nullable = nullable;
	}

	/// Report use of the deprecated array-valued second argument.
	///
	/// Call this only on the rendering branch which interprets the second
	/// argument as an array.
	///
	/// @since 8.0
	@SPI(USE)
	protected final void warnAboutArrayContainsWithArrayArgument() {
		LOG.deprecatedArrayContainsWithArray();
	}

	@Override
	public String getArgumentListSignature() {
		return "(ARRAY haystackArray, OBJECT needleElementOrArray)";
	}
}
