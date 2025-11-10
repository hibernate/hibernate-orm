/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemavalidation;

import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.tool.hbm2ddl.SchemaValidator;

import org.hibernate.testing.orm.junit.JiraKey;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Jan Schatteman
 */
@JiraKey( value = "HHH-13106" )
@RequiresDialect(PostgreSQLDialect.class)
@ServiceRegistry
@DomainModel(annotatedClasses = IdentityGenerationValidationTest.TestEntity.class)
@SessionFactory
public class IdentityGenerationValidationTest {
	@BeforeEach
	void setUp(SessionFactoryScope factoryScope) {
		// force the schema export
		factoryScope.getSessionFactory();
	}

	@Test
	public void testSynonymUsingIndividuallySchemaValidator(DomainModelScope modelScope) {
		new SchemaValidator().validate( modelScope.getDomainModel() );
	}

	@Entity
	@Table(name = "test_entity")
	public static class TestEntity {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;
	}

}
