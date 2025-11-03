/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.query.hql;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Christian Beikov
 */
@DomainModel(annotatedClasses = {
		ImplicitNestedJoinTest.RootEntity.class,
		ImplicitNestedJoinTest.FirstLevelReferencedEntity.class,
		ImplicitNestedJoinTest.SecondLevelReferencedEntityA.class,
		ImplicitNestedJoinTest.SecondLevelReferencedEntityB.class
})
@SessionFactory
@Jira("https://hibernate.atlassian.net/browse/HHH-19905")
public class ImplicitNestedJoinTest {

	@Test
	public void testInnerAndLeftJoin(SessionFactoryScope scope) {
		scope.inSession( session -> {
			final var resultList = session.createQuery(
					"select r.id from RootEntity r"
					+ " join r.firstLevelReference.secondLevelReferenceA sa "
					+ " left join r.firstLevelReference.secondLevelReferenceB sb",
					Long.class
			).getResultList();
			assertThat( resultList ).hasSize( 2 ).containsExactlyInAnyOrder( 1L, 2L );
		} );
	}

	@Test
	public void testLeftAndInnerJoin(SessionFactoryScope scope) {
		scope.inSession( session -> {
			final var resultList = session.createQuery(
					"select r.id from RootEntity r"
					+ " left join r.firstLevelReference.secondLevelReferenceA sa "
					+ " join r.firstLevelReference.secondLevelReferenceB sb",
					Long.class
			).getResultList();
			assertThat( resultList ).hasSize( 1 ).containsExactly( 1L );
		} );
	}

	@Test
	public void testBothInnerJoins(SessionFactoryScope scope) {
		scope.inSession( session -> {
			final var resultList = session.createQuery(
					"select r.id from RootEntity r"
					+ " join r.firstLevelReference.secondLevelReferenceA sa "
					+ " join r.firstLevelReference.secondLevelReferenceB sb",
					Long.class
			).getResultList();
			assertThat( resultList ).hasSize( 1 ).containsExactly( 1L );
		} );
	}

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			// create some test data : one first level reference with both second level references, and
			// another first level reference with only one second level reference
			SecondLevelReferencedEntityA secondLevelA = new SecondLevelReferencedEntityA();
			secondLevelA.id = 1L;
			secondLevelA.name = "Second Level A";
			session.persist( secondLevelA );
			SecondLevelReferencedEntityB secondLevelB = new SecondLevelReferencedEntityB();
			secondLevelB.id = 1L;
			session.persist( secondLevelB );
			FirstLevelReferencedEntity firstLevel1 = new FirstLevelReferencedEntity();
			firstLevel1.id = 1L;
			firstLevel1.secondLevelReferenceA = secondLevelA;
			firstLevel1.secondLevelReferenceB = secondLevelB;
			session.persist( firstLevel1 );
			RootEntity root1 = new RootEntity();
			root1.id = 1L;
			root1.firstLevelReference = firstLevel1;
			session.persist( root1 );
			FirstLevelReferencedEntity firstLevel2 = new FirstLevelReferencedEntity();
			firstLevel2.id = 2L;
			firstLevel2.secondLevelReferenceA = secondLevelA;
			session.persist( firstLevel2 );
			RootEntity root2 = new RootEntity();
			root2.id = 2L;
			root2.firstLevelReference = firstLevel2;
			session.persist( root2 );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Entity(name = "RootEntity")
	public static class RootEntity {
		@Id
		private Long id;

		@ManyToOne
		private FirstLevelReferencedEntity firstLevelReference;
	}

	@Entity(name = "FirstLevelReferencedEntity")
	public static class FirstLevelReferencedEntity {
		@Id
		private Long id;
		@ManyToOne
		private SecondLevelReferencedEntityA secondLevelReferenceA;
		@ManyToOne
		private SecondLevelReferencedEntityB secondLevelReferenceB;

	}

	@Entity(name = "SecondLevelReferencedEntityA")
	public static class SecondLevelReferencedEntityA {
		@Id
		private Long id;
		private String name;
	}

	@Entity(name = "SecondLevelReferencedEntityB")
	public static class SecondLevelReferencedEntityB {
		@Id
		private Long id;
	}
}
