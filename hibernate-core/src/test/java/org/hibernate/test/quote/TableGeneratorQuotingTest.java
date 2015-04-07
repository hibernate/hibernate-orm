/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
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
import org.hibernate.test.common.JdbcConnectionAccessImpl;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
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
