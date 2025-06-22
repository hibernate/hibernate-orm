/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.type;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Jan Schatteman
 */
@DomainModel (
		annotatedClasses = {BooleanArrayToStringTest.TestEntity.class}
)
@SessionFactory
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsTypedArrays.class)
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsArrayToString.class)
@SkipForDialect( dialectClass = OracleDialect.class, reason = "External driver fix required")
@Jira( value = "https://hibernate.atlassian.net/browse/HHH-18765" )
public class BooleanArrayToStringTest {

	@BeforeEach
	public void setup(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> session.persist(
						new TestEntity( 1L, new Boolean[] {Boolean.FALSE, Boolean.FALSE, null, Boolean.TRUE} ) )
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testBooleanArrayToStringWithDefault(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final String actual = session.createQuery(
									"select array_to_string(t.theBoolean, ';', 'null') from TestEntity t",
									String.class
							)
							.getSingleResult();
					assertEquals("false;false;null;true", actual);
				}
		);
	}

	@Test
	public void testBooleanArrayToStringWithoutDefault(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final String actual = session.createQuery(
									"select array_to_string(t.theBoolean, ';') from TestEntity t",
									String.class
							)
							.getSingleResult();
					assertEquals("false;false;true", actual);
				}
		);
	}

	@Entity(name = "TestEntity")
	public static class TestEntity {
		@Id
		private Long id;
		private Boolean[] theBoolean;

		public TestEntity() {
		}

		public TestEntity(Long id, Boolean[] theBoolean) {
			this.id = id;
			this.theBoolean = theBoolean;
		}
	}
}
