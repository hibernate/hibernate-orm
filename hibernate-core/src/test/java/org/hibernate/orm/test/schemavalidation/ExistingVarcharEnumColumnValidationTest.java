/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemavalidation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.boot.MetadataSources;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.testing.jdbc.JdbcUtils;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.hibernate.tool.hbm2ddl.SchemaValidator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.sql.Statement;


@JiraKey("HHH-17908")
@RequiresDialect( H2Dialect.class )
@RequiresDialect( MySQLDialect.class )
public class ExistingVarcharEnumColumnValidationTest {

	@BeforeEach
	public void setUp() {
		try (var registry = ServiceRegistryUtil.serviceRegistryBuilder().build()) {
			JdbcUtils.withConnection( registry, (connection) -> {
				try (var statement = connection.createStatement()) {
					try {
						dropSchema( statement );
					}
					catch (Exception ignore) {
					}
					createSchema( statement );
				}
			} );
		}
	}

	private void dropSchema(Statement statement) throws SQLException {
		statement.execute( "drop table en cascade" );
	}

	private void createSchema(Statement statement) throws SQLException {
		statement.execute(
				"""
					create table en (
						id integer not null,
						sign_position varchar(255)
						check (sign_position in (
							'AFTER_NO_SPACE',
							'AFTER_WITH_SPACE',
							'BEFORE_NO_SPACE',
							'BEFORE_WITH_SPACE')
						),
						primary key (id)
					)
					"""
		);
	}

	@AfterEach
	public void tearDown() {
		try (var registry = ServiceRegistryUtil.serviceRegistryBuilder().build()) {
			final var connections = registry.requireService( ConnectionProvider.class );
			JdbcUtils.withConnection( connections, (connection) -> {
				try (var statement = connection.createStatement()) {
					dropSchema( statement );
				}
			} );
		}
	}

	@Test
	public void testEnumDataTypeSchemaValidator() {
		try (var ssr = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( AvailableSettings.HBM2DDL_AUTO, "validate" )
				.build()) {
			final MetadataSources metadataSources = new MetadataSources( ssr );
			metadataSources.addAnnotatedClass( EntityE.class );

			new SchemaValidator().validate( metadataSources.buildMetadata() );
		}
	}


	@Entity(name = "en")
	@Table(name = "en")
	public static class EntityE {
		@Id
		@Column(name = "id", nullable = false, updatable = false)
		private Integer id;

		@Enumerated(EnumType.STRING)
		@Column(name = "sign_position")
		private SignPosition signPosition;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public SignPosition getSignPosition() {
			return signPosition;
		}

		public void setSignPosition(SignPosition signPosition) {
			this.signPosition = signPosition;
		}
	}

	public enum SignPosition {
		AFTER_NO_SPACE, AFTER_WITH_SPACE, BEFORE_NO_SPACE, BEFORE_WITH_SPACE
	}
}
