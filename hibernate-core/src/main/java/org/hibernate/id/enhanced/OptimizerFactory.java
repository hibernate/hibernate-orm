/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.id.enhanced;

import java.util.Properties;

import static org.hibernate.cfg.MappingSettings.PREFERRED_POOLED_OPTIMIZER;
import static org.hibernate.id.enhanced.OptimizerLogger.OPTIMIZER_MESSAGE_LOGGER;


import static org.hibernate.internal.util.StringHelper.isNotEmpty;

/**
 * Factory for {@link Optimizer} instances.
 *
 * @author Steve Ebersole
 */
public class OptimizerFactory {

	private static final Class<?>[] CTOR_SIG = new Class[] { Class.class, int.class };

	private static Optimizer buildOptimizer(OptimizerDescriptor descriptor, Class<?> returnClass, int incrementSize) {
		final var optimizer = createOptimizer( descriptor, returnClass, incrementSize );
		return optimizer != null ? optimizer : buildFallbackOptimizer( returnClass, incrementSize );
	}

	private static Optimizer createOptimizer(OptimizerDescriptor descriptor, Class<?> returnClass, int incrementSize) {
		final Class<? extends Optimizer> optimizerClass;
		try {
			optimizerClass = descriptor.getOptimizerClass();
		}
		catch ( Throwable ignore ) {
			OPTIMIZER_MESSAGE_LOGGER.unableToLocateCustomOptimizerClass( descriptor.getExternalName() );
			return buildFallbackOptimizer( returnClass, incrementSize );
		}

		try {
			final var ctor = optimizerClass.getConstructor( CTOR_SIG );
			return ctor.newInstance( returnClass, incrementSize );
		}
		catch ( Throwable ignore ) {
			OPTIMIZER_MESSAGE_LOGGER.unableToInstantiateOptimizer( descriptor.getExternalName() );
		}
		return null;
	}

	private static Optimizer buildFallbackOptimizer(Class<?> returnClass, int incrementSize) {
		return new NoopOptimizer( returnClass, incrementSize );
	}

	/**
	 * Builds an optimizer
	 *
	 * @param type The optimizer type, either a shorthand name or the {@link Optimizer} class name.
	 * @param returnClass The generated value java type
	 * @param incrementSize The increment size.
	 * @param explicitInitialValue The user supplied initial-value (-1 indicates the user did not specify).
	 *
	 * @return The built optimizer
	 */
	public static Optimizer buildOptimizer(OptimizerDescriptor type, Class<?> returnClass, int incrementSize, long explicitInitialValue) {
		final var optimizer = buildOptimizer( type, returnClass, incrementSize );
		if ( optimizer instanceof InitialValueAwareOptimizer initialValueAwareOptimizer ) {
			initialValueAwareOptimizer.injectInitialValue( explicitInitialValue );
		}
		return optimizer;
	}

	/**
	 * Determine the optimizer to use when there was not one explicitly specified.
	 */
	public static String determineImplicitOptimizerName(int incrementSize, Properties configSettings) {
		if ( incrementSize <= 1 ) {
			return StandardOptimizerDescriptor.NONE.getExternalName();
		}
		else {
			// see if the user defined a preferred pooled optimizer
			final String preferredPooledOptimizerStrategy =
					configSettings.getProperty( PREFERRED_POOLED_OPTIMIZER );
			return isNotEmpty( preferredPooledOptimizerStrategy )
					? preferredPooledOptimizerStrategy
					// otherwise fall back to the fallback strategy
					: StandardOptimizerDescriptor.POOLED.getExternalName();
		}
	}

	private OptimizerFactory() {
	}
}
