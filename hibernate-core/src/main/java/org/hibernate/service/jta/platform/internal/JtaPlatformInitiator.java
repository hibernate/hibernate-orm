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
package org.hibernate.service.jta.platform.internal;
import java.util.Map;
import org.hibernate.HibernateException;
import org.hibernate.cfg.Environment;
import org.hibernate.internal.util.jndi.JndiHelper;
import org.hibernate.service.classloading.spi.ClassLoaderService;
import org.hibernate.service.jta.platform.spi.JtaPlatform;
import org.hibernate.service.jta.platform.spi.JtaPlatformException;
import org.hibernate.service.spi.ServiceInitiator;
import org.hibernate.service.spi.ServiceRegistry;
import org.hibernate.transaction.TransactionManagerLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Standard initiator for the standard {@link org.hibernate.service.jta.platform.spi.JtaPlatform}
 *
 * @author Steve Ebersole
 */
public class JtaPlatformInitiator implements ServiceInitiator<JtaPlatform> {
	public static final JtaPlatformInitiator INSTANCE = new JtaPlatformInitiator();
	public static final String JTA_PLATFORM = "hibernate.jta.platform";

	private static final Logger log = LoggerFactory.getLogger( JtaPlatformInitiator.class );

	@Override
	public Class<JtaPlatform> getServiceInitiated() {
		return JtaPlatform.class;
	}

	@SuppressWarnings( {"unchecked"})
	@Override
	public JtaPlatform initiateService(Map configVales, ServiceRegistry registry) {
		final Object platform = getConfiguredPlatform( configVales, registry );
		if ( platform == null ) {
			return new NoJtaPlatform();
		}

		if ( JtaPlatform.class.isInstance( platform ) ) {
			return (JtaPlatform) platform;
		}

		final Class<JtaPlatform> jtaPlatformImplClass;

		if ( Class.class.isInstance( platform ) ) {
			jtaPlatformImplClass = (Class<JtaPlatform>) platform;
		}
		else {
			final String platformImplName = platform.toString();
			final ClassLoaderService classLoaderService = registry.getService( ClassLoaderService.class );
			try {
				jtaPlatformImplClass = (Class<JtaPlatform>) classLoaderService.classForName( platformImplName );
			}
			catch ( Exception e ) {
				throw new HibernateException( "Unable to locate specified JtaPlatform class [" + platformImplName + "]", e );
			}
		}

		try {
			return jtaPlatformImplClass.newInstance();
		}
		catch ( Exception e ) {
			throw new HibernateException( "Unable to create specified JtaPlatform class [" + jtaPlatformImplClass.getName() + "]", e );
		}
	}

	private Object getConfiguredPlatform(Map configVales, ServiceRegistry registry) {
		Object platform = configVales.get( JTA_PLATFORM );
		if ( platform == null ) {
			final String transactionManagerLookupImplName = (String) configVales.get( Environment.TRANSACTION_MANAGER_STRATEGY );
			if ( transactionManagerLookupImplName != null ) {
				log.warn(
						"Using deprecated " + TransactionManagerLookup.class.getName() + " strategy [" +
								Environment.TRANSACTION_MANAGER_STRATEGY +
								"], use newer " + JtaPlatform.class.getName() +
								" strategy instead [" + JTA_PLATFORM + "]"
				);
				platform = mapLegacyClasses( transactionManagerLookupImplName, configVales, registry );
				log.debug( "Mapped {} -> {}", transactionManagerLookupImplName, platform );
			}
		}
		return platform;
	}

	private JtaPlatform mapLegacyClasses(
			String transactionManagerLookupImplName,
			Map configVales,
			ServiceRegistry registry) {
		if ( transactionManagerLookupImplName == null ) {
			return null;
		}

		log.info(
				"Encountered legacy TransactionManagerLookup specified; convert to newer " +
						JtaPlatform.class.getName() + " contract specified via " +
						JTA_PLATFORM + "setting"
		);

		if ( "org.hibernate.transaction.BESTransactionManagerLookup".equals( transactionManagerLookupImplName ) ) {
			return new BorlandEnterpriseServerJtaPlatform();
		}

		if ( "org.hibernate.transaction.BTMTransactionManagerLookup".equals( transactionManagerLookupImplName ) ) {
			return new BitronixJtaPlatform();
		}

		if ( "org.hibernate.transaction.JBossTransactionManagerLookup".equals( transactionManagerLookupImplName ) ) {
			return new JBossAppServerPlatform();
		}

		if ( "org.hibernate.transaction.JBossTSStandaloneTransactionManagerLookup".equals( transactionManagerLookupImplName ) ) {
			return new JBossStandAloneJtaPlatform();
		}

		if ( "org.hibernate.transaction.JOnASTransactionManagerLookup".equals( transactionManagerLookupImplName ) ) {
			return new JOnASJtaPlatform();
		}

		if ( "org.hibernate.transaction.JOTMTransactionManagerLookup".equals( transactionManagerLookupImplName ) ) {
			return new JOTMJtaPlatform();
		}

		if ( "org.hibernate.transaction.JRun4TransactionManagerLookup".equals( transactionManagerLookupImplName ) ) {
			return new JRun4JtaPlatform();
		}

		if ( "org.hibernate.transaction.OC4JTransactionManagerLookup".equals( transactionManagerLookupImplName ) ) {
			return new OC4JJtaPlatform();
		}

		if ( "org.hibernate.transaction.OrionTransactionManagerLookup".equals( transactionManagerLookupImplName ) ) {
			return new OrionJtaPlatform();
		}

		if ( "org.hibernate.transaction.ResinTransactionManagerLookup".equals( transactionManagerLookupImplName ) ) {
			return new ResinJtaPlatform();
		}

		if ( "org.hibernate.transaction.SunONETransactionManagerLookup".equals( transactionManagerLookupImplName ) ) {
			return new SunOneJtaPlatform();
		}

		if ( "org.hibernate.transaction.WeblogicTransactionManagerLookup".equals( transactionManagerLookupImplName ) ) {
			return new WeblogicJtaPlatform();
		}

		if ( "org.hibernate.transaction.WebSphereTransactionManagerLookup".equals( transactionManagerLookupImplName ) ) {
			return new WebSphereJtaPlatform();
		}

		if ( "org.hibernate.transaction.WebSphereExtendedJTATransactionLookup".equals( transactionManagerLookupImplName ) ) {
			return new WebSphereExtendedJtaPlatform();
		}

		try {
			TransactionManagerLookup lookup = (TransactionManagerLookup) registry.getService( ClassLoaderService.class )
					.classForName( transactionManagerLookupImplName )
					.newInstance();
			return new TransactionManagerLookupBridge( lookup, JndiHelper.extractJndiProperties( configVales ) );
		}
		catch ( Exception e ) {
			throw new JtaPlatformException(
					"Unable to build " + TransactionManagerLookupBridge.class.getName() + " from specified " +
							TransactionManagerLookup.class.getName() + " implementation: " +
							transactionManagerLookupImplName
			);
		}
	}
}
