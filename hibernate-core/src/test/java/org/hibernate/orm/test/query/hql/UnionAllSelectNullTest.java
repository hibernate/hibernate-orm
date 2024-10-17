/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Tuple;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DomainModel(
		annotatedClasses = {
				UnionAllSelectNullTest.TestEntity.class,
				UnionAllSelectNullTest.AnotherTestEntity.class
		}
)
@SessionFactory
@JiraKey("HHH-18720")
class UnionAllSelectNullTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.persist( new TestEntity( 1L, "a" ) );
					session.persist( new AnotherTestEntity( 2L, "b" ) );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "delete TestEntity" ).executeUpdate();
					session.createQuery( "delete AnotherTestEntity" ).executeUpdate();
				}
		);
	}

	@Test
	void testSelect(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List<Tuple> resultList = session.createQuery(
									"SELECT te.id as id from TestEntity te"
											+ " union all SELECT null as id from AnotherTestEntity ate"
									, Tuple.class )
							.getResultList();
					assertThat( resultList.size() ).isEqualTo( 2 );
					assertResultIsCorrect( resultList, 1L, null );
				}
		);
	}

	@Test
	void testSelect2(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List<Tuple> resultList = session.createQuery(
									"SELECT null as id from TestEntity te"
											+ " union all SELECT ate.id as id from AnotherTestEntity ate"
									, Tuple.class )
							.getResultList();
					assertThat( resultList.size() ).isEqualTo( 2 );
					assertResultIsCorrect( resultList, null, 2L );
				}
		);
	}

	@Test
	void testSelect3(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List<Tuple> resultList = session.createQuery(
									"SELECT te.id as id from TestEntity te"
											+ " union all SELECT ate.id as id from AnotherTestEntity ate"
									, Tuple.class )
							.getResultList();
					assertThat( resultList.size() ).isEqualTo( 2 );
					assertResultIsCorrect( resultList, 1L, 2L );
				}
		);
	}

	private static void assertResultIsCorrect(List<Tuple> resultList, Long id1, Long id2) {
		Set<Long> ids = new HashSet<>( 2 );
		ids.add( (Long) resultList.get( 0 ).get( "id" ) );
		ids.add( (Long) resultList.get( 1 ).get( "id" ) );
		assertThat( ids.contains( id1 ) ).as( "Result does not contain expected value:" + id1 ).isTrue();
		assertThat( ids.contains( id2 ) ).as( "Result does not contain expected value:" + id2 ).isTrue();
	}

	@Entity(name = "TestEntity")
	public static class TestEntity {

		@Id
		private Long id;

		private String name;

		public TestEntity() {
		}

		public TestEntity(Long id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	@Entity(name = "AnotherTestEntity")
	public static class AnotherTestEntity {

		@Id
		private Long id;

		private String name;

		public AnotherTestEntity() {
		}

		public AnotherTestEntity(Long id, String name) {
			this.id = id;
			this.name = name;
		}
	}
}
