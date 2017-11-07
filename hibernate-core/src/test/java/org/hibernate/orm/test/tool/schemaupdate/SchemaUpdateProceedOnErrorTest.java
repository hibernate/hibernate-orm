/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.tool.schemaupdate;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;


/**
 * @author Vlad Mihalcea
 */
public class SchemaUpdateProceedOnErrorTest {

	@Test
	public void testProceedOnError() {
		try {
			buildSessionFactory();
		}
		catch (Exception e) {
			fail( "Should not halt on error: " + e.getMessage() );
		}
	}

	private void buildSessionFactory() {
		StandardServiceRegistry ssr = null;
		SessionFactory sessionFactory = null;
		try {
			final StandardServiceRegistryBuilder ssrBuilder = new StandardServiceRegistryBuilder()
					.applySetting( AvailableSettings.HBM2DDL_AUTO, "update" );
			ssr = ssrBuilder.build();

			final MetadataSources metadataSources = new MetadataSources( ssr );
			metadataSources.addAnnotatedClass( SchemaUpdateHaltOnErrorTest.From.class );

			sessionFactory = metadataSources.buildMetadata().buildSessionFactory();

		}
		finally {
			if ( ssr != null ) {
				StandardServiceRegistryBuilder.destroy( ssr );
			}
			if ( sessionFactory != null && !sessionFactory.isClosed() ) {
				sessionFactory.close();
			}
		}
	}

	@Entity(name = "From")
	public class From {

		@Id
		private Integer id;

		private String table;

		private String select;
	}
}
