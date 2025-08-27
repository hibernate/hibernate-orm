/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.resultmapping;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.ColumnResult;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.SqlResultSetMapping;
import jakarta.persistence.Table;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Nathan Xu
 */
@Jpa(
		annotatedClasses = NamedNativeQueryWithCountColumnTest.Sample.class,
		properties = @Setting(name = AvailableSettings.GLOBALLY_QUOTED_IDENTIFIERS, value = "true")
)
@JiraKey(value = "HHH-15070")
class NamedNativeQueryWithCountColumnTest {

	@BeforeEach
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					for ( int i = 0; i < 3; i++ ) {
						Sample sample = new Sample( i, String.valueOf( i ) );
						entityManager.persist( sample );
					}
				}
		);
	}

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	void testNamedNativeQuery(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager ->
						entityManager.createNamedQuery( "sample.count", Long.class )
		);
	}

	@Test
	@RequiresDialect(H2Dialect.class)
	void testNamedNativeQueryExecution(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Long count = entityManager.createNamedQuery( "sample.count", Long.class ).getSingleResult();
					assertThat( count, is( 3L ) );
				} );
	}

	@SqlResultSetMapping(
			name = "mapping",
			columns = @ColumnResult(name = "cnt")
	)
	@NamedNativeQuery(
			name = "sample.count",
			resultSetMapping = "mapping",
			query = "SELECT count(*) AS cnt FROM SAMPLE_TABLE"
	)
	@Entity(name = "Sample")
	@Table(name = "SAMPLE_TABLE")
	static class Sample {

		@Id
		Integer id;

		String name;

		public Sample() {
		}

		public Sample(Integer id, String name) {
			this.id = id;
			this.name = name;
		}
	}
}
