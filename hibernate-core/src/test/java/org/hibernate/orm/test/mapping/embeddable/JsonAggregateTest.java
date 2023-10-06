/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.embeddable;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.testing.orm.junit.BaseSessionFactoryFunctionalTest;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;

import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.assertEquals;

//@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJsonAggregate.class)
public class JsonAggregateTest extends BaseSessionFactoryFunctionalTest {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				JsonHolder.class
		};
	}

	@BeforeEach
	public void setUp() {
		inTransaction(
				session -> {
					session.persist( new JsonHolder( 1L, Aggregate.createAggregate2() ) );
				}
		);
	}

	@AfterEach
	protected void cleanupTest() {
		inTransaction(
				session -> {
					session.createMutationQuery( "delete from JsonHolder h" ).executeUpdate();
				}
		);
	}

	@Test
	@JiraKey("HHH-17294")
	public void testDirtyChecking() {
		sessionFactoryScope().inTransaction(
				entityManager -> {
					JsonHolder jsonHolder = entityManager.find( JsonHolder.class, 1L );
					assertEquals("String 'abc'", jsonHolder.getAggregate().getTheString());
					jsonHolder.getAggregate().setTheString( "MyString" );
					entityManager.flush();
					entityManager.clear();
					// Fails, when it should pass
					assertEquals( "String 'MyString'", entityManager.find( JsonHolder.class, 1L ).getAggregate().getTheString() );
				}
		);
	}

	//tag::json-type-mapping-example[]
	@Entity(name = "JsonHolder")
	public static class JsonHolder {

		@Id
		private Long id;
		@JdbcTypeCode(SqlTypes.JSON)
		private Aggregate aggregate;

		//end::json-type-mapping-example[]
		//Getters and setters are omitted for brevity

		public JsonHolder() {
		}

		public JsonHolder(Long id, Aggregate aggregate) {
			this.id = id;
			this.aggregate = aggregate;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Aggregate getAggregate() {
			return aggregate;
		}

		public void setAggregate(Aggregate aggregate) {
			this.aggregate = aggregate;
		}

		//tag::json-type-mapping-example[]
	}

	//end::json-type-mapping-example[]
}

