/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.EnumSet;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaValidator;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Jira("HHH-16094")
@BaseUnitTest
@RequiresDialect(MySQLDialect.class)
public class MySQLLobSchemaValidationTest {
	private StandardServiceRegistry ssr;
	private MetadataImplementor metadata;

	@BeforeEach
	public void setUp() throws Exception {
		ssr = ServiceRegistryUtil.serviceRegistry();
		ConnectionProvider connectionProvider = ssr.getService( ConnectionProvider.class );
		try (Connection connection = connectionProvider.getConnection();) {
			PreparedStatement statement = connection.prepareStatement(
					"create table TestEntity (id integer not null, lobField longtext, primary key (id))" );
			try {
				statement.executeUpdate();
			}
			finally {
				statement.close();
			}
		}

	}

	@AfterEach
	public void tearsDown() {
		dropSchema( TestEntity.class );
		StandardServiceRegistryBuilder.destroy( ssr );
	}

	@Test
	public void testSchemaValidation() {
		final MetadataSources metadataSources = new MetadataSources( ssr );

		metadataSources.addAnnotatedClass( TestEntity.class );
		metadata = (MetadataImplementor) metadataSources.buildMetadata();
		metadata.orderColumns( false );
		metadata.validate();
		new SchemaValidator().validate( metadata );
	}

	private void dropSchema(Class... annotatedClasses) {
		final MetadataSources metadataSources = new MetadataSources( ssr );

		for ( Class c : annotatedClasses ) {
			metadataSources.addAnnotatedClass( c );
		}
		metadata = (MetadataImplementor) metadataSources.buildMetadata();
		metadata.orderColumns( false );
		metadata.validate();
		new SchemaExport()
				.setHaltOnError( false )
				.setFormat( false )
				.drop( EnumSet.of( TargetType.DATABASE ), metadata );
	}

	@Entity
	@Table(name = "TestEntity")
	public static class TestEntity {

		@Id
		private int id;

		@Lob
		private String lobField;

	}
}
