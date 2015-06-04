/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.quote;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.hibernate.HibernateException;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.tool.hbm2ddl.SchemaValidator;
import org.hibernate.tool.schema.internal.TargetDatabaseImpl;
import org.hibernate.tool.schema.spi.SchemaManagementTool;
import org.hibernate.tool.schema.spi.Target;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.testing.boot.JdbcConnectionAccessImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.fail;

/**
 * @author Steve Ebersole
 */
public class TableGeneratorQuotingTest extends BaseUnitTestCase {
	private StandardServiceRegistry serviceRegistry;

	@Before
	public void setUp() {
		serviceRegistry = new StandardServiceRegistryBuilder()
				.applySetting( AvailableSettings.GLOBALLY_QUOTED_IDENTIFIERS, "true" )
				.build();
	}

	@After
	public void tearDown() {
		if ( serviceRegistry != null ) {
			StandardServiceRegistryBuilder.destroy( serviceRegistry );
		}
	}

	@Test
	@TestForIssue(jiraKey = "HHH-7927")
	public void testTableGeneratorQuoting() {
		final Metadata metadata = new MetadataSources( serviceRegistry ).addAnnotatedClass( TestEntity.class ).buildMetadata();

		final ConnectionProvider connectionProvider = serviceRegistry.getService( ConnectionProvider.class );
		final Target target = new TargetDatabaseImpl( new JdbcConnectionAccessImpl( connectionProvider ) );
		final SchemaManagementTool tool = serviceRegistry.getService( SchemaManagementTool.class );

		tool.getSchemaDropper( null ).doDrop( metadata, false, target );
		tool.getSchemaCreator( null ).doCreation( metadata, false, target );

		try {
			new SchemaValidator( serviceRegistry, (MetadataImplementor) metadata ).validate();
		}
		catch (HibernateException e) {
			fail( "The identifier generator table should have validated.  " + e.getMessage() );
		}
		finally {
			tool.getSchemaDropper( null ).doDrop( metadata, false, target );
		}
	}

	@Entity
	private static class TestEntity {
		@Id
		@GeneratedValue(strategy = GenerationType.TABLE)
		private int id;
	}
}
