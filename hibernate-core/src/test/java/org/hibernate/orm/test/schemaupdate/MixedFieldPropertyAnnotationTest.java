/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.tool.schema.TargetType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.EnumSet;

/**
 * @author Andrea Boriero
 */
@JiraKey(value = "HHH-9849")
@RequiresDialect(MySQLDialect.class)
public class MixedFieldPropertyAnnotationTest {
	protected ServiceRegistry serviceRegistry;
	protected MetadataImplementor metadata;

	@Test
	public void testUpdateSchema() {
		new SchemaUpdate().execute( EnumSet.of( TargetType.STDOUT, TargetType.DATABASE ), metadata );
	}

	@Entity
	@Table(name = "MyEntity")
	class MyEntity {

		@Id
		public int getId() {
			return 0;
		}

		@Column(name = "Ul")
		public int getValue() {
			return 0;
		}

		public void setId(final int _id) {
		}

		public void setValue(int value) {
		}
	}

	@Before
	public void setUp() {
		serviceRegistry = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( Environment.GLOBALLY_QUOTED_IDENTIFIERS, "false" )
				.build();
		metadata = (MetadataImplementor) new MetadataSources( serviceRegistry )
				.addAnnotatedClass( MyEntity.class )
				.buildMetadata();

		System.out.println( "********* Starting SchemaExport for START-UP *************************" );
		new SchemaExport().create( EnumSet.of( TargetType.STDOUT, TargetType.DATABASE ), metadata );
		System.out.println( "********* Completed SchemaExport for START-UP *************************" );
	}

	@After
	public void tearDown() {
		System.out.println( "********* Starting SchemaExport (drop) for TEAR-DOWN *************************" );
		new SchemaExport().drop( EnumSet.of( TargetType.STDOUT, TargetType.DATABASE ), metadata );
		System.out.println( "********* Completed SchemaExport (drop) for TEAR-DOWN *************************" );

		StandardServiceRegistryBuilder.destroy( serviceRegistry );
		serviceRegistry = null;
	}
}
