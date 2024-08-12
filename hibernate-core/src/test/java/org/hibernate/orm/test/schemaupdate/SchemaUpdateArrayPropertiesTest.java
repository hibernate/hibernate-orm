/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.schemaupdate;

import java.math.BigInteger;
import java.util.EnumSet;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.annotations.Array;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.cfg.Environment;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@Jira( "https://hibernate.atlassian.net/browse/HHH-18406" )
public class SchemaUpdateArrayPropertiesTest {
	@Test
	public void testUpdateExisting() {
		final StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( Environment.HBM2DDL_AUTO, "none" )
				.build();
		final Metadata metadata = new MetadataSources( ssr ).addAnnotatedClass( TestEntity.class ).buildMetadata();
		// First create the schema
		new SchemaExport().createOnly( EnumSet.of( TargetType.DATABASE ), metadata );
		// Then update the existing table
		new SchemaUpdate().execute( EnumSet.of( TargetType.DATABASE ), metadata );
		// Verify a query works as expected
		try (final SessionFactory sf = metadata.getSessionFactoryBuilder().build()) {
			try (Session session = sf.openSession()) {
				assertThat( session.find( TestEntity.class, 1 ) ).isNull();
			}
			sf.getSchemaManager().dropMappedObjects( false );
		}
	}

	@Test
	public void testUpdateNew() {
		final StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( Environment.HBM2DDL_AUTO, "none" )
				.build();
		final Metadata metadata = new MetadataSources( ssr ).addAnnotatedClass( TestEntity.class ).buildMetadata();
		// Update should create the schema and all necessary types
		new SchemaUpdate().execute( EnumSet.of( TargetType.DATABASE ), metadata );
		// Verify a query works as expected
		try (final SessionFactory sf = metadata.getSessionFactoryBuilder().build()) {
			try (Session session = sf.openSession()) {
				assertThat( session.find( TestEntity.class, 1 ) ).isNull();
			}
			sf.getSchemaManager().dropMappedObjects( false );
		}
	}

	@Entity( name = "TestEntity" )
	static class TestEntity {
		@Id
		@GeneratedValue
		private Integer id;

		private Integer[] integerArray;

		@Array( length = 3 )
		private String[] stringArrayAnnotated;

		@Array( length = 5 )
		private BigInteger[] bigIntegerArrayAnnotated;
	}
}
