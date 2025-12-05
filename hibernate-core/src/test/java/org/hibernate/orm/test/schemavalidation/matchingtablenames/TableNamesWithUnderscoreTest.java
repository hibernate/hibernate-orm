/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemavalidation.matchingtablenames;


import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.tool.hbm2ddl.SchemaValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Andrea Boriero
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey(value = "HHH-10718")
@DomainModel(annotatedClasses = {
		TableNamesWithUnderscoreTest.Entity1.class,
		TableNamesWithUnderscoreTest.Entity01.class
})
@SessionFactory
public class TableNamesWithUnderscoreTest {
	@BeforeEach
	public void setUp(SessionFactoryScope factoryScope) {
		// force schema export
		factoryScope.getSessionFactory();
	}

	@Test
	public void testSchemaValidationDoesNotFailDueToAMoreThanOneTableFound(DomainModelScope modelScope) {
		new SchemaValidator().validate( modelScope.getDomainModel() );
	}

	@Entity(name = "Entity1")
	@Table(name = "Entity_1")
	public static class Entity1 {
		@Id
		@GeneratedValue
		private int id;
	}

	@Entity(name = "Entity01")
	@Table(name = "Entity01")
	public static class Entity01 {
		@Id
		@GeneratedValue
		private int id;
	}
}
