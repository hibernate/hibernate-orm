/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id.enhanced;

import java.lang.reflect.Constructor;
import java.util.Properties;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.config.ConfigurationHelper;

import org.jboss.logging.Logger;

/**
 * Factory for {@link Optimizer} instances.
 *
 * @author Steve Ebersole
 */
public class OptimizerFactory {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			OptimizerFactory.class.getName()
	);

	/**
	 * Does the given optimizer name represent a pooled strategy?
	 *
	 * @param optimizerName The name of the optimizer
	 *
	 * @return {@code true} indicates the optimizer is a pooled strategy.
	 */
	public static boolean isPooledOptimizer(String optimizerName) {
		final StandardOptimizerDescriptor standardDescriptor = StandardOptimizerDescriptor.fromExternalName( optimizerName );
		return standardDescriptor != null && standardDescriptor.isPooled();
	}

	private static final Class[] CTOR_SIG = new Class[] { Class.class, int.class };

	private static Optimizer buildOptimizer(String type, Class returnClass, int incrementSize) {
		final Class<? extends Optimizer> optimizerClass;

		final StandardOptimizerDescriptor standardDescriptor = StandardOptimizerDescriptor.fromExternalName( type );
		if ( standardDescriptor != null ) {
			optimizerClass = standardDescriptor.getOptimizerClass();
		}
		else {
			try {
				optimizerClass = ReflectHelper.classForName( type );
			}
			catch( Throwable ignore ) {
				LOG.unableToLocateCustomOptimizerClass( type );
				return buildFallbackOptimizer( returnClass, incrementSize );
			}
		}

		try {
			final Constructor ctor = optimizerClass.getConstructor( CTOR_SIG );
			return (Optimizer) ctor.newInstance( returnClass, incrementSize );
		}
		catch( Throwable ignore ) {
			LOG.unableToInstantiateOptimizer( type );
		}

		return buildFallbackOptimizer( returnClass, incrementSize );
	}

	private static Optimizer buildFallbackOptimizer(Class returnClass, int incrementSize) {
		return new NoopOptimizer( returnClass, incrementSize );
	}

	/**
	 * Builds an optimizer
	 *
	 * @param type The optimizer type, either a short-hand name or the {@link Optimizer} class name.
	 * @param returnClass The generated value java type
	 * @param incrementSize The increment size.
	 * @param explicitInitialValue The user supplied initial-value (-1 indicates the user did not specify).
	 *
	 * @return The built optimizer
	 */
	public static Optimizer buildOptimizer(String type, Class returnClass, int incrementSize, long explicitInitialValue) {
		final Optimizer optimizer = buildOptimizer( type, returnClass, incrementSize );
		if ( InitialValueAwareOptimizer.class.isInstance( optimizer ) ) {
			( (InitialValueAwareOptimizer) optimizer ).injectInitialValue( explicitInitialValue );
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

		// see if the user defined a preferred pooled optimizer...
		final String preferredPooledOptimizerStrategy = configSettings.getProperty( AvailableSettings.PREFERRED_POOLED_OPTIMIZER );
		if ( StringHelper.isNotEmpty( preferredPooledOptimizerStrategy ) ) {
			return preferredPooledOptimizerStrategy;
		}

		// otherwise fallback to the fallback strategy
		return StandardOptimizerDescriptor.POOLED.getExternalName();
	}

	private OptimizerFactory() {
	}
}
