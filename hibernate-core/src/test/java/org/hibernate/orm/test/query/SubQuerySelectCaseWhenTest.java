/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


@DomainModel(
		annotatedClasses = {
				SubQuerySelectCaseWhenTest.TestEntity.class,
		}
)
@SessionFactory
@JiraKey("HHH-18681")
public class SubQuerySelectCaseWhenTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.persist( new TestEntity( "A" ) );
					session.persist( new TestEntity( "B" ) );
					session.persist( new TestEntity( "C" ) );
				}
		);
	}

	@Test
	public void testSelectCase(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			List<Integer> result = session.createQuery( "select "
							+ "		(select "
							+ "			case "
							+ "				when "
							+ "					t.name = ?1 "
							+ "				then 0 "
							+ "				else 1 "
							+ "			end)"
							+ " from TestEntity t order by t.name", Integer.class )
					.setParameter( 1, "A" )
					.list();
			assertThat( result.size() ).isEqualTo( 3 );
			assertThat( result.get( 0 ) ).isEqualTo( 0 );
			assertThat( result.get( 1 ) ).isEqualTo( 1 );
			assertThat( result.get( 2 ) ).isEqualTo( 1 );
		} );
	}

	@Entity(name = "TestEntity")
	public class TestEntity {

		@Id
		@GeneratedValue
		private Long id;

		private String name;

		public TestEntity() {
		}

		public TestEntity(String name) {
			this.name = name;
		}
	}
}
