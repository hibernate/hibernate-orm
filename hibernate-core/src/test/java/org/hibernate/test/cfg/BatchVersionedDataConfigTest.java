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
package org.hibernate.test.cfg;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.Oracle10gDialect;
import org.hibernate.dialect.Oracle12cDialect;
import org.hibernate.dialect.Oracle8iDialect;
import org.hibernate.dialect.Oracle9Dialect;
import org.hibernate.dialect.Oracle9iDialect;
import org.hibernate.dialect.OracleDialect;

import org.junit.Test;

import org.hibernate.testing.junit4.BaseUnitTestCase;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author Andrea Boriero
 */
public class BatchVersionedDataConfigTest extends BaseUnitTestCase {
	public static final String CFG_XML = "org/hibernate/test/cfg/cache/hibernate.cfg.xml";

	@Test
	public void testBatchVersionedData() {
		Configuration cfg = new Configuration().configure( CFG_XML );
		SessionFactory sessionFactory = cfg.buildSessionFactory();
		assertThat( sessionFactory.getSessionFactoryOptions().isJdbcBatchVersionedData(), is( true ) );
	}

	@Test
	public void testBatchVersionedDataForOracle10gDialect() {
		Configuration cfg = new Configuration().configure( CFG_XML )
				.setProperty(
						AvailableSettings.DIALECT,
						Oracle10gDialect.class.getName()
				);
		SessionFactory sessionFactory = cfg.buildSessionFactory();
		assertThat( sessionFactory.getSessionFactoryOptions().isJdbcBatchVersionedData(), is( false ) );
	}

	@Test
	public void testBatchVersionedDataForOracle8iDialect() {
		Configuration cfg = new Configuration().configure( CFG_XML )
				.setProperty(
						AvailableSettings.DIALECT,
						Oracle8iDialect.class.getName()
				);
		SessionFactory sessionFactory = cfg.buildSessionFactory();
		assertThat( sessionFactory.getSessionFactoryOptions().isJdbcBatchVersionedData(), is( false ) );
	}

	@Test
	public void testBatchVersionedDataForOracle9iDialect() {
		Configuration cfg = new Configuration().configure( CFG_XML )
				.setProperty(
						AvailableSettings.DIALECT,
						Oracle9iDialect.class.getName()
				);
		SessionFactory sessionFactory = cfg.buildSessionFactory();
		assertThat( sessionFactory.getSessionFactoryOptions().isJdbcBatchVersionedData(), is( false ) );
	}

	@Test
	public void testBatchVersionedDataForOracle9Dialect() {
		Configuration cfg = new Configuration().configure( CFG_XML )
				.setProperty(
						AvailableSettings.DIALECT,
						Oracle9Dialect.class.getName()
				);
		SessionFactory sessionFactory = cfg.buildSessionFactory();
		assertThat( sessionFactory.getSessionFactoryOptions().isJdbcBatchVersionedData(), is( false ) );
	}

	@Test
	public void testBatchVersionedDataForOracleDialect() {
		Configuration cfg = new Configuration().configure( CFG_XML )
				.setProperty(
						AvailableSettings.DIALECT,
						OracleDialect.class.getName()
				);
		SessionFactory sessionFactory = cfg.buildSessionFactory();
		assertThat( sessionFactory.getSessionFactoryOptions().isJdbcBatchVersionedData(), is( false ) );
	}

	@Test
	public void testBatchVersionedDataForOracle12cDialect() {
		Configuration cfg = new Configuration().configure( CFG_XML )
				.setProperty(
						AvailableSettings.DIALECT,
						Oracle12cDialect.class.getName()
				);
		SessionFactory sessionFactory = cfg.buildSessionFactory();
		assertThat( sessionFactory.getSessionFactoryOptions().isJdbcBatchVersionedData(), is( true ) );
	}
}
