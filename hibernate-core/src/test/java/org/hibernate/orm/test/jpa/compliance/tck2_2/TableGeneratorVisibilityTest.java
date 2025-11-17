/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.compliance.tck2_2;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.TableGenerator;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

/**
 * @author Andrea Boriero
 */
@JiraKey(value = "HHH-12157")
@DomainModel(annotatedClasses = {
		TableGeneratorVisibilityTest.TestEntity1.class,
		TableGeneratorVisibilityTest.TestEntity2.class,
		TableGeneratorVisibilityTest.TestEntity3.class
})
@SessionFactory
@ServiceRegistry(settings = {@Setting(name = AvailableSettings.JPA_ID_GENERATOR_GLOBAL_SCOPE_COMPLIANCE, value = "true")})
public class TableGeneratorVisibilityTest {

	@Test
	public void testGeneratorIsVisible(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new TestEntity1() ) );

		scope.inTransaction( session -> session.persist( new TestEntity2() ) );

		scope.inTransaction( session -> session.persist( new TestEntity3() ) );
	}

	@Entity(name = "TestEntity1")

	public static class TestEntity1 {
		@Id
		@GeneratedValue(strategy = GenerationType.TABLE, generator = "table-generator-2")
		public long id;
	}

	@Entity(name = "TestEntity2")
	public static class TestEntity2 {
		@Id
		@GeneratedValue(strategy = GenerationType.TABLE, generator = "table-generator")
		public long id;
	}

	@Entity(name = "TestEntity3")
	@TableGenerator(
			name = "table-generator",
			table = "table_identifier",
			pkColumnName = "identifier",
			valueColumnName = "val",
			allocationSize = 5
	)
	public static class TestEntity3 {
		@Id
		@TableGenerator(
				name = "table-generator-2",
				table = "table_identifier_2",
				pkColumnName = "identifier",
				valueColumnName = "val",
				allocationSize = 5
		)
		@GeneratedValue(strategy = GenerationType.TABLE, generator = "table-generator")
		public long id;
	}
}
