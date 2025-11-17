/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hhh13670;

import jakarta.persistence.Column;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Tuple;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


@JiraKey(value = "HHH-13670")
@DomainModel(
		annotatedClasses = {
				HHH13670Test.Super.class,
				HHH13670Test.SubA.class,
				HHH13670Test.SubB.class
		}
)
@SessionFactory
public class HHH13670Test {

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			SubA a_1 = new SubA( 1L );
			SubA a_2 = new SubA( 2L );
			SubA a_3 = new SubA( 3L );
			SubA a_14 = em.getReference( SubA.class, 10L );
			SubB b_4 = new SubB( 4L, null );
			SubB b_5 = new SubB( 5L, a_3 );
			SubB b_6 = new SubB( 6L, b_4 );
			SubB b_7 = new SubB( 7L, a_14 );

			em.merge( a_1 );
			em.merge( a_2 );
			em.merge( a_3 );
			em.merge( b_4 );
			em.merge( b_5 );
			em.merge( b_6 );
			em.merge( b_7 );
		} );
	}

	@Test
	public void testDereferenceSuperClassAttributeInWithClause(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createQuery(
					"SELECT subB_0.id FROM SubB subB_0 LEFT JOIN subB_0.other subA_0 ON subA_0.id = subB_0.parent.id",
					Tuple.class ).getResultList();
		} );
	}

	@Test
	public void testRootTypeJoinWithGroupJoins(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			List<Tuple> resultList = session.createQuery(
							"SELECT subB_0.id, subA_0.id, subB_0.id, subA_0.id FROM SubB subB_0 LEFT JOIN Super subA_0 ON subA_0.id = subB_0.parent.id ORDER BY subB_0.id ASC, subA_0.id ASC",
							Tuple.class )
					.getResultList();
			assertThat( resultList )
					.describedAs( "Rows omitted despite optional association should have rendered a left join" )
					.hasSize( 4 );

			assertThat( resultList.get( 0 ).get( 0 ) ).isEqualTo( 4L );
			assertThat( resultList.get( 1 ).get( 0 ) ).isEqualTo( 5L );
			assertThat( resultList.get( 2 ).get( 0 ) ).isEqualTo( 6L );
			assertThat( resultList.get( 3 ).get( 0 ) ).isEqualTo( 7L );

			assertThat( resultList.get( 0 ).get( 1, Long.class ) ).isNull();
			assertThat( resultList.get( 1 ).get( 1, Long.class ) ).isEqualTo( 3L );
			assertThat( resultList.get( 2 ).get( 1, Long.class ) ).isEqualTo( 4L );
			assertThat( resultList.get( 3 ).get( 1, Long.class ) )
					.describedAs( "Missing entry in foreign table should not be returned" )
					.isNull();
		} );
	}

	@Test
	public void testSubTypeJoinWithTableGroupJoins(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			List<Tuple> resultList = session.createQuery(
							"SELECT subB_0.id, subA_0.id, subB_0.id, subA_0.id FROM SubB subB_0 LEFT JOIN SubA subA_0 ON subA_0.id = subB_0.parent.id ORDER BY subB_0.id ASC, subA_0.id ASC",
							Tuple.class )
					.getResultList();

			assertThat( resultList )
					.describedAs( "Rows omitted despite optional association should have rendered a left join" )
					.hasSize( 4 );

			assertThat( resultList.get( 0 ).get( 0 ) ).isEqualTo( 4L );
			assertThat( resultList.get( 1 ).get( 0 ) ).isEqualTo( 5L );
			assertThat( resultList.get( 2 ).get( 0 ) ).isEqualTo( 6L );
			assertThat( resultList.get( 3 ).get( 0 ) ).isEqualTo( 7L );

			assertThat( resultList.get( 0 ).get( 1, Long.class ) ).isNull();
			assertThat( resultList.get( 1 ).get( 1, Long.class ) ).isEqualTo( 3L );
			assertThat( resultList.get( 2 ).get( 1 ) )
					.describedAs( "Another subtype than queried for was returned" )
					.isNull();
			assertThat( resultList.get( 3 ).get( 1, Long.class ) )
					.describedAs( "Missing entry in foreign table should not be returned" )
					.isNull();
		} );
	}

	@Test
	public void testSubTypePropertyReferencedFromEntityJoinInSyntheticSubquery(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			List<Tuple> resultList = session.createQuery(
							"SELECT  subB_0.id, subA_0.id, subB_0.id, subA_0.id FROM SubB subB_0 INNER JOIN SubA subA_0 ON 1=1 WHERE (EXISTS (SELECT 1 FROM subB_0.parent _synth_subquery_0 WHERE subA_0.id = _synth_subquery_0.id)) ORDER BY subB_0.id ASC, subA_0.id ASC",
							Tuple.class )
					.getResultList();

			assertThat( resultList ).hasSize( 1 );
		} );
	}

	@Test
	public void testSubTypePropertyReferencedFromEntityJoinInSyntheticSubquery2(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			List<Tuple> resultList = session.createQuery(
							"SELECT  subB_0.id, subA_0.id, subB_0.id, subA_0.id FROM SubB subB_0 INNER JOIN SubA subA_0 ON 1=1 WHERE (EXISTS (SELECT 1 FROM Super s WHERE subA_0.id = s.parent.id)) ORDER BY subB_0.id ASC, subA_0.id ASC",
							Tuple.class )
					.getResultList();

			assertThat( resultList ).hasSize( 4 );
		} );
	}

	@Test
	public void testSubTypePropertyReferencedFromEntityJoinInSyntheticSubquery3(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			List<Tuple> resultList = session.createQuery(
							"SELECT subB_0.id, subA_0.id, subB_0.id, subA_0.id FROM SubB subB_0 INNER JOIN SubA subA_0 ON 1=1 WHERE (EXISTS (SELECT 1 FROM Super s WHERE s.id = subB_0.parent.id)) ORDER BY subB_0.id ASC, subA_0.id ASC",
							Tuple.class )
					.getResultList();

			assertThat( resultList ).hasSize( 6 );
		} );
	}

	@Test
	public void testSubTypePropertyReferencedFromEntityJoinInSyntheticSubquery4(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			List<Tuple> resultList = session.createQuery(
							"SELECT subB_0.id, subA_0.id, subB_0.id, subA_0.id FROM SubB subB_0 INNER JOIN SubA subA_0 ON 1=1 WHERE (EXISTS (SELECT 1 FROM Super s WHERE s.id = subA_0.parent.id)) ORDER BY subB_0.id ASC, subA_0.id ASC",
							Tuple.class )
					.getResultList();

			assertThat( resultList ).hasSize( 0 );
		} );
	}

	@Test
	public void testSubTypePropertyReferencedFromWhereClause(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			List<Tuple> resultList = session.createQuery(
							"SELECT subB_0.id FROM SubB subB_0 WHERE subB_0.parent.id IS NOT NULL", Tuple.class )
					.getResultList();
		} );
	}

	@Test
	public void testSubTypePropertyReferencedFromGroupByClause(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			List<Tuple> resultList = session.createQuery(
							"SELECT subB_0.id FROM SubB subB_0 GROUP BY subB_0.id , subB_0.parent.id", Tuple.class )
					.getResultList();
		} );
	}

	@Test
	public void testSubTypePropertyReferencedFromOrderByClause(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			List<Tuple> resultList = session.createQuery(
							"SELECT subB_0.id FROM SubB subB_0 ORDER BY subB_0.id , subB_0.parent.id", Tuple.class )
					.getResultList();
		} );
	}

	@Entity(name = "Super")
	@Inheritance(strategy = InheritanceType.JOINED)
	public static class Super<SubType extends Super> {

		@Id
		@Column
		Long id;

		@JoinColumn(foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
		@ManyToOne(targetEntity = Super.class, fetch = FetchType.LAZY)
		SubType parent;

	}

	@Entity(name = "SubA")
	public static class SubA extends Super {

		SubA() {
		}

		SubA(Long id) {
			this.id = id;
		}

	}

	@Entity(name = "SubB")
	public static class SubB extends Super<SubA> {

		@ManyToOne(fetch = FetchType.LAZY)
		Super other;

		SubB() {
		}

		SubB(Long id, Super parent) {
			this.id = id;
			((Super) this).parent = parent;
		}

	}

}
