/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.formula;

import java.sql.Timestamp;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.annotations.Formula;
import org.hibernate.dialect.OracleDialect;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@DomainModel( annotatedClasses = FormulaFunctionWithoutParenthesesTest.TestEntity.class )
@SessionFactory
@RequiresDialect(OracleDialect.class)
@JiraKey("HHH-20840")
public class FormulaFunctionWithoutParenthesesTest {

	@Test
	void testFunctionWithoutParentheses(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new TestEntity( 1L ) ) );

		scope.inTransaction( session -> {
			final var entity = session.find( TestEntity.class, 1L );
			assertNotNull( entity.currentTimestamp );
		} );
	}

	@Entity(name = "TestEntity")
	static class TestEntity {
		@Id
		private Long id;

		@Formula("sysdate")
		private Timestamp currentTimestamp;

		TestEntity() {
		}

		TestEntity(Long id) {
			this.id = id;
		}
	}
}
