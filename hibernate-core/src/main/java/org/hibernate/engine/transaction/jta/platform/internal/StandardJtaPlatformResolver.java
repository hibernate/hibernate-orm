/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
			log.tracef( "Located JtaPlatformProvider [%s] provided JtaPlaform : %s", provider, providedPlatform );
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
