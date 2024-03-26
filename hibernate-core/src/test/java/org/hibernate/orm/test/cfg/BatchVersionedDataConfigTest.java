/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) {DATE}, Red Hat Inc. or third-party contributors as
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
package org.hibernate.orm.test.cfg;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.community.dialect.OracleLegacyDialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.OracleDialect;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.testing.util.ServiceRegistryUtil;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author Andrea Boriero
 */
public class BatchVersionedDataConfigTest extends BaseUnitTestCase {

	private SessionFactory sessionFactory;
	private Configuration cfg;

	@Before
	public void setUp() {
		cfg = new Configuration();
		ServiceRegistryUtil.applySettings( cfg.getStandardServiceRegistryBuilder() );

		// HHH-10290 ignore environment property hibernate.jdbc.batch_versioned_data
		if (cfg.getProperties().getProperty(AvailableSettings.BATCH_VERSIONED_DATA) != null) {
			cfg.getProperties().remove(AvailableSettings.BATCH_VERSIONED_DATA);
			cfg.getStandardServiceRegistryBuilder().getSettings().remove(AvailableSettings.BATCH_VERSIONED_DATA);
		}
	}

	@After
	public void tearDown() {
		if ( sessionFactory != null ) {
			sessionFactory.close();
		}
	}

	@Test
	public void testBatchVersionedDataForDialectNotSettingBatchVersionedDataProperty() {
		cfg.setProperty( AvailableSettings.DIALECT, H2Dialect.class );
		sessionFactory = cfg.buildSessionFactory();
		assertThat( sessionFactory.getSessionFactoryOptions().isJdbcBatchVersionedData(), is( true ) );
	}

	@Test
	public void testBatchVersionedDataForOracleDialect() {
		cfg.setProperty( AvailableSettings.DIALECT, OracleDialect.class );
		sessionFactory = cfg.buildSessionFactory();

		assertThat( sessionFactory.getSessionFactoryOptions().isJdbcBatchVersionedData(), is( true ) );
	}

	@Test
	public void testBatchVersionedDataForOracle10gDialect() {
		cfg.setProperty( AvailableSettings.DIALECT, OracleLegacyDialect.class );
		cfg.setProperty( AvailableSettings.JAKARTA_HBM2DDL_DB_MAJOR_VERSION, 10 );
		sessionFactory = cfg.buildSessionFactory();

		assertThat( sessionFactory.getSessionFactoryOptions().isJdbcBatchVersionedData(), is( false ) );
	}

	@Test
	public void testBatchVersionedDataForOracle8iDialect() {
		cfg.setProperty( AvailableSettings.DIALECT, OracleLegacyDialect.class );
		cfg.setProperty( AvailableSettings.JAKARTA_HBM2DDL_DB_MAJOR_VERSION, 8 );
		sessionFactory = cfg.buildSessionFactory();

		assertThat( sessionFactory.getSessionFactoryOptions().isJdbcBatchVersionedData(), is( false ) );
	}

	@Test
	public void testBatchVersionedDataForOracle9iDialect() {
		cfg.setProperty( AvailableSettings.DIALECT, OracleLegacyDialect.class );
		cfg.setProperty( AvailableSettings.JAKARTA_HBM2DDL_DB_MAJOR_VERSION, 9 );
		sessionFactory = cfg.buildSessionFactory();

		assertThat( sessionFactory.getSessionFactoryOptions().isJdbcBatchVersionedData(), is( false ) );
	}

	@Test
	public void testBatchVersionedDataForOracle12cDialect() {
		cfg.setProperty( AvailableSettings.DIALECT, OracleLegacyDialect.class );
		cfg.setProperty( AvailableSettings.JAKARTA_HBM2DDL_DB_MAJOR_VERSION, 12 );
		sessionFactory = cfg.buildSessionFactory();

		assertThat( sessionFactory.getSessionFactoryOptions().isJdbcBatchVersionedData(), is( true ) );
	}
}
