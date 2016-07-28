/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.tool.schema;

import java.util.HashMap;
import java.util.Map;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;

import org.hibernate.testing.jta.TestingJtaBootstrap;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.test.resource.transaction.jta.JtaPlatformStandardTestingImpl;
import org.junit.Test;

/**
 * @author Steve Ebersole
 */
public class DropSchemaDuringJtaTxnTest extends BaseUnitTestCase {
	@Test
	public void testDrop() throws Exception {
		final SessionFactory sessionFactory = buildSessionFactory();
		sessionFactory.close();
	}
	@Test
	public void testDropDuringActiveJtaTransaction() throws Exception {
		final SessionFactory sessionFactory = buildSessionFactory();

		JtaPlatformStandardTestingImpl.INSTANCE.transactionManager().begin();
		try {
			sessionFactory.close();
		}
		finally {
			JtaPlatformStandardTestingImpl.INSTANCE.transactionManager().commit();
		}
	}

	private SessionFactory buildSessionFactory() {
		Map settings = new HashMap();
		settings.putAll( Environment.getProperties() );
		TestingJtaBootstrap.prepare( settings );
		settings.put( AvailableSettings.TRANSACTION_COORDINATOR_STRATEGY, "jta" );

		final StandardServiceRegistry ssr = new StandardServiceRegistryBuilder().applySettings( settings ).build();

		return new MetadataSources( ssr )
				.addAnnotatedClass( TestEntity.class )
				.buildMetadata()
				.buildSessionFactory();
	}


	@Entity( name = "TestEntity" )
	@Table( name = "TestEntity" )
	public static class TestEntity {
		@Id
		public Integer id;
		String name;
	}
}
