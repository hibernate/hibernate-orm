/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql.exec;

import java.io.Serializable;

import org.hibernate.stat.spi.StatisticsImplementor;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@DomainModel(
		annotatedClasses = {
				EntityWithEmbeddedIdTest.TestEntity.class
		}
)
@SessionFactory(generateStatistics = true)
public class EntityWithEmbeddedIdTest {

	private PK entityId;

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		final TestEntity entity = new TestEntity();
		entityId = new PK( 25, "Acme" );
		scope.inTransaction(
				session -> {
					entity.setId( entityId );
					entity.setData( "test" );
					session.persist( entity );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testHqlSelectOnlyTheEmbeddedId(SessionFactoryScope scope) {
		StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();
		scope.inTransaction(
				session -> {
					final PK value = session.createQuery(
							"select e.id FROM TestEntity e",
							PK.class
					).uniqueResult();
					assertThat( value, equalTo( entityId ) );
				}
		);
		assertThat( statistics.getPrepareStatementCount(), is( 1L ) );
	}

	@Entity(name = "TestEntity")
	public static class TestEntity {

		@EmbeddedId
		PK id;

		private String data;

		public PK getId() {
			return id;
		}

		public void setId(PK id) {
			this.id = id;
		}

		public String getData() {
			return data;
		}

		public void setData(String data) {
			this.data = data;
		}
	}

	public static class PK implements Serializable {
		private Integer value1;
		private String value2;

		public PK() {
		}

		public PK(Integer value1, String value2) {
			this.value1 = value1;
			this.value2 = value2;
		}

		public Integer getValue1() {
			return value1;
		}

		public void setValue1(Integer value1) {
			this.value1 = value1;
		}

		public String getValue2() {
			return value2;
		}

		public void setValue2(String value2) {
			this.value2 = value2;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}

			final PK pk = (PK) o;

			if ( value1 != null ? !value1.equals( pk.getValue1() ) : pk.getValue1() != null ) {
				return false;
			}
			return value2 != null ? value2.equals( pk.getValue2() ) : pk.getValue2() == null;
		}

		@Override
		public int hashCode() {
			int result = value1 != null ? value1.hashCode() : 0;
			result = 31 * result + ( value2 != null ? value2.hashCode() : 0 );
			return result;
		}
	}

}
