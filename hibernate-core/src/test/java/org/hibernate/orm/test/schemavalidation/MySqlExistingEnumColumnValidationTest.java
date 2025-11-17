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
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.testing.jdbc.JdbcUtils;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.tool.hbm2ddl.SchemaValidator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static jakarta.persistence.GenerationType.IDENTITY;

@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey("HHH-16498")
@RequiresDialect(MySQLDialect.class)
@ServiceRegistry
public class MySqlExistingEnumColumnValidationTest {
	@BeforeEach
	void setUp(ServiceRegistryScope registryScope) {
		JdbcUtils.withConnection( registryScope.getRegistry(), connection -> {
			try ( var statement = connection.createStatement() ) {
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

	@AfterEach
	void tearDown(ServiceRegistryScope registryScope) {
		JdbcUtils.withConnection( registryScope.getRegistry(), connection -> {
			try ( var statement = connection.createStatement() ) {
				statement.execute( "DROP TABLE en CASCADE" );
			}
		} );
	}

	@Test
	public void testSynonymUsingGroupedSchemaValidator(ServiceRegistryScope registryScope) {
		final MetadataSources metadataSources = new MetadataSources( registryScope.getRegistry() );
		metadataSources.addAnnotatedClass( EntityE.class );

		new SchemaValidator().validate( metadataSources.buildMetadata() );
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

	public static enum SignPosition {
		AFTER_NO_SPACE, AFTER_WITH_SPACE, BEFORE_NO_SPACE, BEFORE_WITH_SPACE
	}
}
