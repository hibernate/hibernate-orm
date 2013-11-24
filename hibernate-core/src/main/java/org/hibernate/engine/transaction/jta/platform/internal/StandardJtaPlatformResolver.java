/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.engine.transaction.jta.platform.internal;

import java.util.Map;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatformProvider;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatformResolver;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class StandardJtaPlatformResolver implements JtaPlatformResolver {
	public static final StandardJtaPlatformResolver INSTANCE = new StandardJtaPlatformResolver();

	private static final Logger log = Logger.getLogger( StandardJtaPlatformResolver.class );

	@Override
	public JtaPlatform resolveJtaPlatform(Map configurationValues, ServiceRegistryImplementor registry) {
		final ClassLoaderService classLoaderService = registry.getService( ClassLoaderService.class );

		// Initially look for a JtaPlatformProvider
		for ( JtaPlatformProvider provider : classLoaderService.loadJavaServices( JtaPlatformProvider.class ) ) {
			final JtaPlatform providedPlatform = provider.getProvidedJtaPlatform();
			if ( providedPlatform!= null ) {
				return providedPlatform;
			}
		}


		// look for classes on the ClassLoader (via service) that we know indicate certain JTA impls or
		// indicate running in certain environments with known JTA impls.
		//
		// IMPL NOTE : essentially we attempt Class lookups and use the exceptions from the class(es) not
		// being found as the indicator


		// JBoss ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		try {
			classLoaderService.classForName( JBossStandAloneJtaPlatform.JBOSS_TM_CLASS_NAME );
			classLoaderService.classForName( JBossStandAloneJtaPlatform.JBOSS_UT_CLASS_NAME );

			// we know that the JBoss TS classes are available
			// if neither of these look-ups resulted in an error (no such class), then JBossTM is available on
			// the classpath
			//
			// todo : we cannot really distinguish between the need for JBossStandAloneJtaPlatform versus JBossApServerJtaPlatform
			// but discussions with David led to the JtaPlatformProvider solution above, so inside JBoss AS we
			// should be relying on that
			return new JBossStandAloneJtaPlatform();
		}
		catch (ClassLoadingException ignore) {
		}

		// Bitronix ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		try {
			classLoaderService.classForName( BitronixJtaPlatform.TM_CLASS_NAME );
			return new BitronixJtaPlatform();
		}
		catch (ClassLoadingException ignore) {
		}

		// JOnAS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		try {
			classLoaderService.classForName( JOnASJtaPlatform.TM_CLASS_NAME );
			return new JOnASJtaPlatform();
		}
		catch (ClassLoadingException ignore) {
		}

		// JOTM ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		try {
			classLoaderService.classForName( JOTMJtaPlatform.TM_CLASS_NAME );
			return new JOTMJtaPlatform();
		}
		catch (ClassLoadingException ignore) {
		}

		// WebSphere ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		for ( WebSphereJtaPlatform.WebSphereEnvironment webSphereEnvironment
				: WebSphereJtaPlatform.WebSphereEnvironment.values() ) {
			try {
				Class accessClass = classLoaderService.classForName( webSphereEnvironment.getTmAccessClassName() );
				return new WebSphereJtaPlatform( accessClass, webSphereEnvironment );
			}
			catch (ClassLoadingException ignore) {
			}
		}

		// Finally, return the default...
		log.debugf( "Could not resolve JtaPlatform, using default [%s]", NoJtaPlatform.class.getName() );
		return NoJtaPlatform.INSTANCE;
	}
}
