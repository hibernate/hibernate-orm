/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.boot.registry.selector.internal;

import java.util.ArrayList;
import java.util.List;

import org.jboss.logging.Logger;

import org.hibernate.boot.registry.classloading.internal.ClassLoaderServiceImpl;
import org.hibernate.boot.registry.selector.spi.StrategySelectionException;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.dialect.CUBRIDDialect;
import org.hibernate.dialect.Cache71Dialect;
import org.hibernate.dialect.DB2390Dialect;
import org.hibernate.dialect.DB2400Dialect;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.DerbyTenFiveDialect;
import org.hibernate.dialect.DerbyTenSevenDialect;
import org.hibernate.dialect.DerbyTenSixDialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.FirebirdDialect;
import org.hibernate.dialect.FrontBaseDialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.InformixDialect;
import org.hibernate.dialect.Ingres10Dialect;
import org.hibernate.dialect.Ingres9Dialect;
import org.hibernate.dialect.IngresDialect;
import org.hibernate.dialect.InterbaseDialect;
import org.hibernate.dialect.JDataStoreDialect;
import org.hibernate.dialect.MckoiDialect;
import org.hibernate.dialect.MimerSQLDialect;
import org.hibernate.dialect.MySQL5Dialect;
import org.hibernate.dialect.MySQL5InnoDBDialect;
import org.hibernate.dialect.Oracle10gDialect;
import org.hibernate.dialect.Oracle8iDialect;
import org.hibernate.dialect.Oracle9iDialect;
import org.hibernate.dialect.PointbaseDialect;
import org.hibernate.dialect.PostgreSQL81Dialect;
import org.hibernate.dialect.PostgreSQL82Dialect;
import org.hibernate.dialect.PostgresPlusDialect;
import org.hibernate.dialect.ProgressDialect;
import org.hibernate.dialect.SAPDBDialect;
import org.hibernate.dialect.SQLServer2005Dialect;
import org.hibernate.dialect.SQLServer2008Dialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.Sybase11Dialect;
import org.hibernate.dialect.SybaseASE157Dialect;
import org.hibernate.dialect.SybaseASE15Dialect;
import org.hibernate.dialect.SybaseAnywhereDialect;
import org.hibernate.dialect.TeradataDialect;
import org.hibernate.dialect.TimesTenDialect;
import org.hibernate.engine.transaction.internal.jdbc.JdbcTransactionFactory;
import org.hibernate.engine.transaction.internal.jta.CMTTransactionFactory;
import org.hibernate.engine.transaction.internal.jta.JtaTransactionFactory;
import org.hibernate.engine.transaction.jta.platform.internal.BitronixJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.internal.BorlandEnterpriseServerJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.internal.JBossAppServerJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.internal.JBossStandAloneJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.internal.JOTMJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.internal.JOnASJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.internal.JRun4JtaPlatform;
import org.hibernate.engine.transaction.jta.platform.internal.OC4JJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.internal.OrionJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.internal.ResinJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.internal.SunOneJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.internal.WebSphereExtendedJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.internal.WebSphereJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.internal.WeblogicJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.engine.transaction.spi.TransactionFactory;

/**
 * @author Steve Ebersole
 */
public class StrategySelectorBuilder {
	private static final Logger log = Logger.getLogger( StrategySelectorBuilder.class );

	private static class CustomRegistration<T> {
		private final Class<T> strategy;
		private final String name;
		private final Class<? extends T> implementation;

		private CustomRegistration(Class<T> strategy, String name, Class<? extends T> implementation) {
			this.strategy = strategy;
			this.name = name;
			this.implementation = implementation;
		}

		public void registerWith(StrategySelectorImpl strategySelector) {
			strategySelector.registerStrategyImplementor( strategy, name, implementation );
		}
	}

	private final List<CustomRegistration> customRegistrations = new ArrayList<CustomRegistration>();

	@SuppressWarnings("unchecked")
	public <T> void addCustomRegistration(Class<T> strategy, String name, Class<? extends T> implementation) {
		if ( !strategy.isInterface() ) {
			// not good form...
			log.debug( "Registering non-interface strategy implementation : " + strategy.getName()  );
		}

		if ( ! strategy.isAssignableFrom( implementation ) ) {
			throw new StrategySelectionException(
					"Implementation class [" + implementation.getName() + "] does not implement strategy interface ["
							+ strategy.getName() + "]"
			);
		}
		customRegistrations.add( new CustomRegistration( strategy, name, implementation ) );
	}

	public StrategySelector buildSelector(ClassLoaderServiceImpl classLoaderService) {
		StrategySelectorImpl strategySelector = new StrategySelectorImpl( classLoaderService );

		// build the baseline...
		addDialects( strategySelector );
		addJtaPlatforms( strategySelector );
		addTransactionFactories( strategySelector );

		// todo : apply auto-discovered registrations

		// apply customizations
		for ( CustomRegistration customRegistration : customRegistrations ) {
			customRegistration.registerWith( strategySelector );
		}

		return strategySelector;
	}

	private void addDialects(StrategySelectorImpl strategySelector) {
		strategySelector.registerStrategyImplementor( Dialect.class, "Cache71", Cache71Dialect.class );
		strategySelector.registerStrategyImplementor( Dialect.class, "CUBRID", CUBRIDDialect.class );
		strategySelector.registerStrategyImplementor( Dialect.class, "DB2", DB2Dialect.class );
		strategySelector.registerStrategyImplementor( Dialect.class, "DB2-390", DB2390Dialect.class );
		strategySelector.registerStrategyImplementor( Dialect.class, "DB2-400", DB2400Dialect.class );
		strategySelector.registerStrategyImplementor( Dialect.class, "Derby10.5", DerbyTenFiveDialect.class );
		strategySelector.registerStrategyImplementor( Dialect.class, "Derby10.6", DerbyTenSixDialect.class );
		strategySelector.registerStrategyImplementor( Dialect.class, "Derby10.7", DerbyTenSevenDialect.class );
		strategySelector.registerStrategyImplementor( Dialect.class, "Firebird", FirebirdDialect.class );
		strategySelector.registerStrategyImplementor( Dialect.class, "FrontBase", FrontBaseDialect.class );
		strategySelector.registerStrategyImplementor( Dialect.class, "H2", H2Dialect.class );
		strategySelector.registerStrategyImplementor( Dialect.class, "HSQL", HSQLDialect.class );
		strategySelector.registerStrategyImplementor( Dialect.class, "Informix", InformixDialect.class );
		strategySelector.registerStrategyImplementor( Dialect.class, "Ingres", IngresDialect.class );
		strategySelector.registerStrategyImplementor( Dialect.class, "Ingres9", Ingres9Dialect.class );
		strategySelector.registerStrategyImplementor( Dialect.class, "Ingres10", Ingres10Dialect.class );
		strategySelector.registerStrategyImplementor( Dialect.class, "Interbase", InterbaseDialect.class );
		strategySelector.registerStrategyImplementor( Dialect.class, "JDataStore", JDataStoreDialect.class );
		strategySelector.registerStrategyImplementor( Dialect.class, "Mckoi", MckoiDialect.class );
		strategySelector.registerStrategyImplementor( Dialect.class, "MimerSQL", MimerSQLDialect.class );
		strategySelector.registerStrategyImplementor( Dialect.class, "MySQL5", MySQL5Dialect.class );
		strategySelector.registerStrategyImplementor( Dialect.class, "MySQL5-InnoDB", MySQL5InnoDBDialect.class );
		strategySelector.registerStrategyImplementor( Dialect.class, "MySQL", MySQL5Dialect.class );
		strategySelector.registerStrategyImplementor( Dialect.class, "MySQL-InnoDB", MySQL5InnoDBDialect.class );
		strategySelector.registerStrategyImplementor( Dialect.class, "Oracle8i", Oracle8iDialect.class );
		strategySelector.registerStrategyImplementor( Dialect.class, "Oracle9i", Oracle9iDialect.class );
		strategySelector.registerStrategyImplementor( Dialect.class, "Oracle10g", Oracle10gDialect.class );
		strategySelector.registerStrategyImplementor( Dialect.class, "Pointbase", PointbaseDialect.class );
		strategySelector.registerStrategyImplementor( Dialect.class, "PostgresPlus", PostgresPlusDialect.class );
		strategySelector.registerStrategyImplementor( Dialect.class, "PostgreSQL81", PostgreSQL81Dialect.class );
		strategySelector.registerStrategyImplementor( Dialect.class, "PostgreSQL82", PostgreSQL82Dialect.class );
		strategySelector.registerStrategyImplementor( Dialect.class, "Progress", ProgressDialect.class );
		strategySelector.registerStrategyImplementor( Dialect.class, "SAP", SAPDBDialect.class );
		strategySelector.registerStrategyImplementor( Dialect.class, "SQLServer", SQLServerDialect.class );
		strategySelector.registerStrategyImplementor( Dialect.class, "SQLServer2005", SQLServer2005Dialect.class );
		strategySelector.registerStrategyImplementor( Dialect.class, "SQLServer2008", SQLServer2008Dialect.class );
		strategySelector.registerStrategyImplementor( Dialect.class, "Sybase11", Sybase11Dialect.class );
		strategySelector.registerStrategyImplementor( Dialect.class, "SybaseAnywhere", SybaseAnywhereDialect.class );
		strategySelector.registerStrategyImplementor( Dialect.class, "SybaseASE15", SybaseASE15Dialect.class );
		strategySelector.registerStrategyImplementor( Dialect.class, "SybaseASE157", SybaseASE157Dialect.class );
		strategySelector.registerStrategyImplementor( Dialect.class, "Teradata", TeradataDialect.class );
		strategySelector.registerStrategyImplementor( Dialect.class, "TimesTen", TimesTenDialect.class );
	}

	private void addJtaPlatforms(StrategySelectorImpl strategySelector) {
		addJtaPlatforms(
				strategySelector,
				BorlandEnterpriseServerJtaPlatform.class,
				"Borland",
				"org.hibernate.service.jta.platform.internal.BorlandEnterpriseServerJtaPlatform"
		);

		addJtaPlatforms(
				strategySelector,
				BitronixJtaPlatform.class,
				"Bitronix",
				"org.hibernate.service.jta.platform.internal.BitronixJtaPlatform"
		);

		addJtaPlatforms(
				strategySelector,
				JBossAppServerJtaPlatform.class,
				"JBossAS",
				"org.hibernate.service.jta.platform.internal.JBossAppServerJtaPlatform"
		);

		addJtaPlatforms(
				strategySelector,
				JBossStandAloneJtaPlatform.class,
				"JBossTS",
				"org.hibernate.service.jta.platform.internal.JBossStandAloneJtaPlatform"
		);

		addJtaPlatforms(
				strategySelector,
				JOnASJtaPlatform.class,
				"JOnAS",
				"org.hibernate.service.jta.platform.internal.JOnASJtaPlatform"
		);

		addJtaPlatforms(
				strategySelector,
				JOTMJtaPlatform.class,
				"JOTM",
				"org.hibernate.service.jta.platform.internal.JOTMJtaPlatform"
		);

		addJtaPlatforms(
				strategySelector,
				JRun4JtaPlatform.class,
				"JRun4",
				"org.hibernate.service.jta.platform.internal.JRun4JtaPlatform"
		);

		addJtaPlatforms(
				strategySelector,
				OC4JJtaPlatform.class,
				"OC4J",
				"org.hibernate.service.jta.platform.internal.OC4JJtaPlatform"
		);

		addJtaPlatforms(
				strategySelector,
				OrionJtaPlatform.class,
				"Orion",
				"org.hibernate.service.jta.platform.internal.OrionJtaPlatform"
		);

		addJtaPlatforms(
				strategySelector,
				ResinJtaPlatform.class,
				"Resin",
				"org.hibernate.service.jta.platform.internal.ResinJtaPlatform"
		);

		addJtaPlatforms(
				strategySelector,
				SunOneJtaPlatform.class,
				"SunOne",
				"org.hibernate.service.jta.platform.internal.SunOneJtaPlatform"
		);

		addJtaPlatforms(
				strategySelector,
				WeblogicJtaPlatform.class,
				"Weblogic",
				"org.hibernate.service.jta.platform.internal.WeblogicJtaPlatform"
		);

		addJtaPlatforms(
				strategySelector,
				WebSphereJtaPlatform.class,
				"WebSphere",
				"org.hibernate.service.jta.platform.internal.WebSphereJtaPlatform"
		);

		addJtaPlatforms(
				strategySelector,
				WebSphereExtendedJtaPlatform.class,
				"WebSphereExtended",
				"org.hibernate.service.jta.platform.internal.WebSphereExtendedJtaPlatform"
		);
	}

	private void addJtaPlatforms(StrategySelectorImpl strategySelector, Class<? extends JtaPlatform> impl, String... names) {
		for ( String name : names ) {
			strategySelector.registerStrategyImplementor( JtaPlatform.class, name, impl );
		}
	}

	private void addTransactionFactories(StrategySelectorImpl strategySelector) {
		strategySelector.registerStrategyImplementor( TransactionFactory.class, JdbcTransactionFactory.SHORT_NAME, JdbcTransactionFactory.class );
		strategySelector.registerStrategyImplementor( TransactionFactory.class, "org.hibernate.transaction.JDBCTransactionFactory", JdbcTransactionFactory.class );

		strategySelector.registerStrategyImplementor( TransactionFactory.class, JtaTransactionFactory.SHORT_NAME, JtaTransactionFactory.class );
		strategySelector.registerStrategyImplementor( TransactionFactory.class, "org.hibernate.transaction.JTATransactionFactory", JtaTransactionFactory.class );

		strategySelector.registerStrategyImplementor( TransactionFactory.class, CMTTransactionFactory.SHORT_NAME, CMTTransactionFactory.class );
		strategySelector.registerStrategyImplementor( TransactionFactory.class, "org.hibernate.transaction.CMTTransactionFactory", CMTTransactionFactory.class );
	}
}
