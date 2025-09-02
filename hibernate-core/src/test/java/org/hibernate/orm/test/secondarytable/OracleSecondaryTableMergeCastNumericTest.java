/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.secondarytable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcType;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.type.descriptor.jdbc.NumericJdbcType;
import org.junit.jupiter.api.Test;

/**
 * Test case reproducing an Oracle-specific issue that occurs when performing a {@code merge}
 * on an entity mapped with a {@link jakarta.persistence.SecondaryTable}.
 * <p>
 * On Oracle databases, Hibernate may generate SQL that leads to an invalid type cast
 * between {@code NUMERIC} columns when merging entities with secondary tables.
 * <p>
 * This test ensures the problem is detected and helps prevent regressions.
 *
 * @author Vincent Bouthinon
 */
@Jpa(
		annotatedClasses = {OracleSecondaryTableMergeCastNumericTest.Actor.class},
		integrationSettings = {
				@Setting(name = org.hibernate.cfg.AvailableSettings.SHOW_SQL, value = "true"),
		}
)
@JiraKey("HHH-")
class OracleSecondaryTableMergeCastNumericTest {

	@Test
	void testMappingManyToOneMappedByAnyPersistedInSecondaryTable(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Actor actor = new Actor();
					actor.salary = 5000.77d;
					entityManager.persist( actor );
					entityManager.flush();
					entityManager.clear();

					actor = entityManager.find( Actor.class, actor.id );
					actor.salary = 5000.88d;
					entityManager.flush();
				}
		);
	}


	@Entity(name = "actor")
	@Table(name = "PRINCIPAL")
	@SecondaryTable(name = "SECONDARY")
	public static class Actor {

		@Id
		@GeneratedValue
		private Long id;

		@Column(table = "SECONDARY", precision = 6, scale = 2)
		@JdbcType(NumericJdbcType.class)
		private double salary;
	}
}
