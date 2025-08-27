/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.type;

import java.time.Duration;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.type.SqlTypes;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@SessionFactory
@DomainModel(annotatedClasses = { PostgresIntervalSecondNoScaleTest.TestEntity.class })
@RequiresDialect(PostgreSQLDialect.class)
@JiraKey("HHH-17520")
public class PostgresIntervalSecondNoScaleTest {

	@Test
	public void testPersistEntity(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					TestEntity entity = new TestEntity( 1L, Duration.ofDays( 3 ), Duration.ofSeconds( 12L ), Duration.ofHours( 3 ) );
					session.persist( entity );
				}
		);
	}

	@Entity(name = "TestEntity")
	public static class TestEntity {
		@Id
		private Long id;

		@Column(length = 0)
		@JdbcTypeCode(SqlTypes.INTERVAL_SECOND)
		Duration interval;

		@Column(length = 1)
		@JdbcTypeCode(SqlTypes.INTERVAL_SECOND)
		Duration interval2;

		@Column
		@JdbcTypeCode(SqlTypes.INTERVAL_SECOND)
		Duration interval3;

		public TestEntity() {
		}

		public TestEntity(Long id, Duration interval, Duration interval2, Duration interval3) {
			this.id = id;
			this.interval = interval;
			this.interval2 = interval2;
			this.interval3 = interval3;
		}
	}
}
