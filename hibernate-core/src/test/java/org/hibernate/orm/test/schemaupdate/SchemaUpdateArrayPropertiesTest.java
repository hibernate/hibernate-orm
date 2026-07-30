/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate;

import java.math.BigInteger;
import java.util.EnumSet;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.annotations.Array;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.pipeline.internal.source.MappingSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.cfg.Environment;
import org.hibernate.orm.test.boot.MetadataBuildingTestHelper;
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
		final Metadata metadata = buildMetadata( ssr );
		// First create the schema
		new SchemaExport().createOnly( EnumSet.of( TargetType.DATABASE ), metadata );
		// Then update the existing table
		new SchemaUpdate().execute( EnumSet.of( TargetType.DATABASE ), metadata );
		// Verify a query works as expected
		try (final SessionFactory sf = org.hibernate.testing.orm.junit.SessionFactoryUtil.buildSessionFactory( metadata )) {
			try (Session session = sf.openSession()) {
				assertThat( session.find( TestEntity.class, 1 ) ).isNull();
			}
			sf.getSchemaManager().drop( false );
		}
	}

	@Test
	public void testUpdateNew() {
		final StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( Environment.HBM2DDL_AUTO, "none" )
				.build();
		final Metadata metadata = buildMetadata( ssr );
		// Update should create the schema and all necessary types
		new SchemaUpdate().execute( EnumSet.of( TargetType.DATABASE ), metadata );
		// Verify a query works as expected
		try (final SessionFactory sf = org.hibernate.testing.orm.junit.SessionFactoryUtil.buildSessionFactory( metadata )) {
			try (Session session = sf.openSession()) {
				assertThat( session.find( TestEntity.class, 1 ) ).isNull();
			}
			sf.getSchemaManager().drop( false );
		}
	}

	private Metadata buildMetadata(StandardServiceRegistry ssr) {
		return MetadataBuildingTestHelper.buildMetadata(
				ssr,
				new MappingSources().addManagedClass( TestEntity.class )
		);
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
