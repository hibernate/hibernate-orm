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
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.type.descriptor.jdbc.NumericJdbcType;
import org.junit.jupiter.api.Test;

@Jpa(
		annotatedClasses = {MergeCastNumericTest.Actor.class},
		integrationSettings = {
				@Setting(name = org.hibernate.cfg.AvailableSettings.SHOW_SQL, value = "true"),
		}
)
@JiraKey("HHH-19749")
@RequiresDialect(OracleDialect.class)
@RequiresDialect(SQLServerDialect.class)
class MergeCastNumericTest {

	@Test
	void test(EntityManagerFactoryScope scope) {
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
				entityManager.clear();
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
