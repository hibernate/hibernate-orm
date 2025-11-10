/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemavalidation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.boot.MetadataSources;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.jdbc.JdbcUtils;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.hibernate.tool.hbm2ddl.SchemaValidator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static jakarta.persistence.GenerationType.IDENTITY;

@JiraKey("HHH-17675")
@RequiresDialect(H2Dialect.class)
public class H2ExistingEnumColumnValidationTest {

	@BeforeEach
	public void setUp() {
		try (var registry = ServiceRegistryUtil.serviceRegistryBuilder().build()) {
			JdbcUtils.withConnection( registry, (connection) -> {
				try (var statement = connection.createStatement()) {
					statement.execute( "DROP TABLE IF EXISTS en CASCADE" );
					statement.execute(
							"""
								CREATE TABLE en (
									id INTEGER NOT NULL AUTO_INCREMENT,
									sign_position enum (
										'AFTER_NO_SPACE',
										'AFTER_WITH_SPACE',
										'BEFORE_NO_SPACE',
										'BEFORE_WITH_SPACE'
									),
									PRIMARY KEY (id)
								)
								"""
					);
				}
			} );
		}
	}

	@AfterEach
	public void tearDown() {
		try (var registry = ServiceRegistryUtil.serviceRegistryBuilder().build()) {
			JdbcUtils.withConnection( registry, (connection) -> {
				try (var statement = connection.createStatement()) {
					statement.execute( "DROP TABLE en CASCADE" );
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
		@GeneratedValue(strategy = IDENTITY)
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
