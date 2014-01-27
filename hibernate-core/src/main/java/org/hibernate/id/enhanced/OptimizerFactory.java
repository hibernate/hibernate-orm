/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.id.enhanced;

import java.lang.reflect.Constructor;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.internal.CoreMessageLogger;
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

	/**
	 * Builds an optimizer
	 *
	 * @param type The optimizer type, either a short-hand name or the {@link Optimizer} class name.
	 * @param returnClass The generated value java type
	 * @param incrementSize The increment size.
	 * @param classLoaderService ClassLoaderService
	 *
	 * @return The built optimizer
	 *
	 * @deprecated Use {@link #buildOptimizer(String, Class, int, long)} instead
	 */
	@Deprecated
	public static Optimizer buildOptimizer(String type, Class returnClass, int incrementSize,
			ClassLoaderService classLoaderService) {
		final Class<? extends Optimizer> optimizerClass;

		final StandardOptimizerDescriptor standardDescriptor = StandardOptimizerDescriptor.fromExternalName( type );
		if ( standardDescriptor != null ) {
			optimizerClass = standardDescriptor.getOptimizerClass();
		}
		else {
			try {
				optimizerClass = classLoaderService.classForName( type );
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
	 * @param classLoaderService ClassLoaderService
	 *
	 * @return The built optimizer
	 */
	public static Optimizer buildOptimizer(String type, Class returnClass, int incrementSize, long explicitInitialValue, ClassLoaderService classLoaderService) {
		final Optimizer optimizer = buildOptimizer( type, returnClass, incrementSize, classLoaderService );
		if ( InitialValueAwareOptimizer.class.isInstance( optimizer ) ) {
			( (InitialValueAwareOptimizer) optimizer ).injectInitialValue( explicitInitialValue );
		}
		return optimizer;
	}

	/**
	 * Deprecated!
	 *
	 * @deprecated Use {@link StandardOptimizerDescriptor#getExternalName()} via {@link StandardOptimizerDescriptor#NONE}
	 */
	@Deprecated
	@SuppressWarnings( {"UnusedDeclaration"})
	public static final String NONE = StandardOptimizerDescriptor.NONE.getExternalName();

	/**
	 * Deprecated!
	 *
	 * @deprecated Use {@link StandardOptimizerDescriptor#getExternalName()} via {@link StandardOptimizerDescriptor#HILO}
	 */
	@Deprecated
	@SuppressWarnings( {"UnusedDeclaration"})
	public static final String HILO = StandardOptimizerDescriptor.HILO.getExternalName();

	/**
	 * Deprecated!
	 *
	 * @deprecated Use {@link StandardOptimizerDescriptor#getExternalName()} via {@link StandardOptimizerDescriptor#LEGACY_HILO}
	 */
	@Deprecated
	@SuppressWarnings( {"UnusedDeclaration"})
	public static final String LEGACY_HILO = "legacy-hilo";

	/**
	 * Deprecated!
	 *
	 * @deprecated Use {@link StandardOptimizerDescriptor#getExternalName()} via {@link StandardOptimizerDescriptor#POOLED}
	 */
	@Deprecated
	@SuppressWarnings( {"UnusedDeclaration"})
	public static final String POOL = "pooled";

	/**
	 * Deprecated!
	 *
	 * @deprecated Use {@link StandardOptimizerDescriptor#getExternalName()} via {@link StandardOptimizerDescriptor#POOLED_LO}
	 */
	@Deprecated
	@SuppressWarnings( {"UnusedDeclaration"})
	public static final String POOL_LO = "pooled-lo";

	private OptimizerFactory() {
	}
}
