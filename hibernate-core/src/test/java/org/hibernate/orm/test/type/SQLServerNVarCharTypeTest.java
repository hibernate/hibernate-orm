/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.type;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.EnumSet;

import org.hibernate.annotations.Nationalized;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Andrea Boriero
 */
@JiraKey(value = "HHH-10529")
@RequiresDialect(value = SQLServerDialect.class)
public class SQLServerNVarCharTypeTest {
	private StandardServiceRegistry ssr;
	private MetadataImplementor metadata;
	private SchemaExport schemaExport;

	@BeforeEach
	public void setUp() {
		ssr = ServiceRegistryUtil.serviceRegistry();
		schemaExport = createSchemaExport( new Class[] {MyEntity.class} );
	}

	@AfterEach
	public void tearDown() {
		schemaExport.drop( EnumSet.of( TargetType.DATABASE ), metadata );
		StandardServiceRegistryBuilder.destroy( ssr );
	}

	@Test
	public void testSchemaIsCreatedWithoutExceptions() {
		schemaExport.createOnly( EnumSet.of( TargetType.DATABASE ), metadata );
	}

	private SchemaExport createSchemaExport(Class[] annotatedClasses) {
		final MetadataSources metadataSources = new MetadataSources( ssr );

		for ( Class c : annotatedClasses ) {
			metadataSources.addAnnotatedClass( c );
		}
		metadata = (MetadataImplementor) metadataSources.buildMetadata();
		metadata.orderColumns( false );
		metadata.validate();
		SchemaExport schemaExport = new SchemaExport();
		schemaExport.setHaltOnError( true )
				.setFormat( false );

		return schemaExport;
	}

	@Entity(name = "MyEntity")
	@Table(name = "MY_ENTITY")
	public static class MyEntity {
		@Id
		long id;

		@Nationalized
		@Column(length = 4001)
		String name;
	}
}
