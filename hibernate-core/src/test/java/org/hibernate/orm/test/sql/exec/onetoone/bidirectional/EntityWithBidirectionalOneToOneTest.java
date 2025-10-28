/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql.exec.onetoone.bidirectional;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import org.hibernate.Hibernate;
import org.hibernate.validator.internal.util.Contracts;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.orm.test.sql.exec.onetoone.bidirectional.EntityWithBidirectionalOneToOneTest.AdoptedChild;
import static org.hibernate.orm.test.sql.exec.onetoone.bidirectional.EntityWithBidirectionalOneToOneTest.Child;
import static org.hibernate.orm.test.sql.exec.onetoone.bidirectional.EntityWithBidirectionalOneToOneTest.Mother;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Chris Cranford
 */
@DomainModel(
		annotatedClasses = {
				Mother.class,
				AdoptedChild.class,

				Child.class,
		}
)
@ServiceRegistry
@SessionFactory(useCollectingStatementInspector = true)
public class EntityWithBidirectionalOneToOneTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Mother mother = new Mother( 1, "Giulia" );

			Child child = new Child( 2, "Luis", mother );

			AdoptedChild adoptedChild = new AdoptedChild( 3, "Fab", mother );

			session.persist( mother );
			session.persist( child );
			session.persist( adoptedChild );
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testGetMother(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction( session -> {
			final Mother mother = session.get( Mother.class, 1 );

			Child child = mother.getBiologicalChild();
			assertThat( child, notNullValue() );
			assertTrue(
					Hibernate.isInitialized( child ),
					"The child eager OneToOne association is not initialized"
			);
			assertThat( child.getName(), is( "Luis" ) );
			assertSame( child.getMother(), mother );

			AdoptedChild adoptedChild = mother.getAdopted();
			assertThat( adoptedChild.getName(), is( "Fab" ) );
			assertTrue(
					Hibernate.isInitialized( adoptedChild ),
					"The adoptedChild eager OneToOne association is not initialized"
			);
			assertThat( adoptedChild.getName(), equalTo( "Fab" ) );

			assertSame( adoptedChild.getStepMother(), mother );
			assertThat( adoptedChild.getBiologicalMother(), is( nullValue() ) );

			/*
				fetchablePath: Mother.biologicalChild --- NO circular --- first join created
				fetchablePath: Mother.biologicalChild.mother --- Circular ---
				fetchablePath: Mother.adopted --- NO circular --- second join created
				fetchablePath: Mother.adopted.biologicalMother --- NO circular --- third join created
				fetchablePath: Mother.adopted.biologicalMother.biologicalChild --- NO circular --- fourth join created
				fetchablePath: Mother.adopted.biologicalMother.biologicalChild.mother --- Circular ---
				fetchablePath: Mother.adopted.biologicalMother.adopted --- NO circular --- fifth join created
				fetchablePath: Mother.adopted.biologicalMother.adopted.biologicalMother --- NO circular --- sixth join created
				fetchablePath: Mother.adopted.biologicalMother.adopted.biologicalMother.biologicalChild --- NO circular --- seventh join created
				fetchablePath: Mother.adopted.biologicalMother.adopted.biologicalMother.biologicalChild.mother --- Circular ---
				fetchablePath: Mother.adopted.biologicalMother.adopted.biologicalMother.adopted --- NO circular --- eighth join created
				fetchablePath: Mother.adopted.biologicalMother.adopted.biologicalMother.adopted.biologicalMother --- NO circular ---  max fetch depth reached no join created
				fetchablePath: Mother.adopted.biologicalMother.adopted.biologicalMother.adopted.stepMother --- Circular ---
				fetchablePath: Mother.adopted.biologicalMother.adopted.stepMother --- Circular ---
				fetchablePath org.hibernate.orm.test.sql.exec.onetoone.bidirectional.EntityWithBidirectionalOneToOneTest$Mother.adopted.stepMother --- Circular ---
			 */
			statementInspector.assertExecutedCount( 1 );
			statementInspector.assertNumberOfOccurrenceInQuery( 0, "join", 4 );

		} );
	}

	@Test
	public void testUnrealCaseWhenMotherIsAlsoStepMother(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Mother mother = new Mother( 4, "Jiny" );

			Child child = new Child( 5, "Carlo", mother );

			AdoptedChild adoptedChild = new AdoptedChild( 6, "Andrea", mother );

			adoptedChild.setBiologicalMother( mother );

			session.persist( mother );
			session.persist( child );
			session.persist( adoptedChild );
		} );

		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction( session -> {
			final Mother mother = session.get( Mother.class, 4 );

			Child child = mother.getBiologicalChild();
			assertThat( child, notNullValue() );
			assertTrue(
					Hibernate.isInitialized( child ),
					"The child eager OneToOne association is not initialized"
			);
			assertThat( child.getName(), is( "Carlo" ) );
			assertSame( child.getMother(), mother );

			AdoptedChild adoptedChild = mother.getAdopted();
			assertThat( adoptedChild, notNullValue() );
			assertTrue(
					Hibernate.isInitialized( adoptedChild ),
					"The adoptedChild eager OneToOne association is not initialized"
			);
			assertThat( adoptedChild.getName(), equalTo( "Andrea" ) );

			assertSame( adoptedChild.getStepMother(), mother );
			assertSame( adoptedChild.getBiologicalMother(), mother );

			statementInspector.assertExecutedCount( 2 );
			statementInspector.assertNumberOfOccurrenceInQuery( 0, "join", 4 );
			statementInspector.assertNumberOfOccurrenceInQuery( 1, "join", 5 );
		} );
	}

	@Test
	public void testGetMother3(SessionFactoryScope scope) {
		scope.inTransaction( session -> {

			Mother mother = new Mother( 4, "Catia" );

			Child child = new Child( 5, "Stefano", mother );

			AdoptedChild adoptedChild = new AdoptedChild( 7, "Luisa", mother );

			Mother biologicalMother = new Mother( 6, "Rebecca" );
			adoptedChild.setBiologicalMother( biologicalMother );

			Child anotherChild = new Child( 8, "Igor", biologicalMother );

			session.persist( mother );
			session.persist( biologicalMother );
			session.persist( child );
			session.persist( adoptedChild );
			session.persist( anotherChild );
		} );

		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction( session -> {
			final Mother mother = session.get( Mother.class, 4 );

			assertThat( mother.getName(), equalTo( "Catia" ) );

			Child procreatedChild = mother.getBiologicalChild();
			assertThat( procreatedChild, notNullValue() );
			assertTrue(
					Hibernate.isInitialized( procreatedChild ),
					"The procreatedChild eager OneToOne association is not initialized"
			);
			assertThat( procreatedChild.getName(), equalTo( "Stefano" ) );
			assertSame( procreatedChild.getMother(), mother );

			AdoptedChild adoptedChild = mother.getAdopted();
			assertThat( adoptedChild, notNullValue() );
			assertTrue(
					Hibernate.isInitialized( adoptedChild ),
					"The adoptedChild eager OneToOne association is not initialized"
			);
			assertThat( adoptedChild.getName(), equalTo( "Luisa" ) );
			assertSame( adoptedChild.getStepMother(), mother );

			Mother biologicalMother = adoptedChild.getBiologicalMother();
			assertThat( biologicalMother.getId(), equalTo( 6 ) );
			assertThat( biologicalMother.getAdopted(), nullValue() );

			Child anotherChild = biologicalMother.getBiologicalChild();
			assertThat( anotherChild.getId(), equalTo( 8 ) );
			assertThat( anotherChild.getName(), equalTo( "Igor" ) );
			assertSame(  biologicalMother, anotherChild.getMother() );

			statementInspector.assertExecutedCount( 2 );
			statementInspector.assertNumberOfOccurrenceInQuery( 0, "join", 4);
			statementInspector.assertNumberOfOccurrenceInQuery( 1, "join", 5);
		} );
	}

	@Test
	public void testGetChild(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction( session -> {
			final Child child = session.get( Child.class, 2 );

			Mother mother = child.getMother();
			assertTrue(
					Hibernate.isInitialized( mother ),
					"The mother eager OneToOne association is not initialized"
			);
			assertThat( mother, notNullValue() );
			assertThat( mother.getName(), is( "Giulia" ) );

			Child biologicalChild = mother.getBiologicalChild();
			assertSame( biologicalChild, child );
			assertTrue(
					Hibernate.isInitialized( biologicalChild ),
					"The child eager OneToOne association is not initialized"
			);

			AdoptedChild adoptedChild = mother.getAdopted();
			assertThat( adoptedChild, notNullValue() );
			assertTrue(
					Hibernate.isInitialized( adoptedChild ),
					"The adoptedChild eager OneToOne association is not initialized"
			);
			assertSame( adoptedChild.getStepMother(), mother );
			assertThat( adoptedChild.getBiologicalMother(), nullValue() );

			statementInspector.assertExecutedCount( 1 );
			/*
				fetchablePath: Child.mother --- NO circular --- first join created
				fetchablePath: Child.mother.biologicalChild --- Circular ---
				fetchablePath: Child.mother.adopted --- NO circular --- second join created
				fetchablePath: Child.mother.adopted.biologicalMother --- NO circular --- third join created
				fetchablePath: Child.mother.adopted.biologicalMother.biologicalChild --- NO circular --- fourth join created
				fetchablePath: Child.mother.adopted.biologicalMother.biologicalChild.mother --- Circular ---
				fetchablePath: Child.mother.adopted.biologicalMother.adopted --- NO circular --- fifth join created
				fetchablePath: Child.mother.adopted.biologicalMother.adopted.biologicalMother --- NO circular --- sixth join created
				fetchablePath: Child.mother.adopted.biologicalMother.adopted.biologicalMother.biologicalChild --- NO circular ---  max fetch depth reached no join created
				fetchablePath Child.mother.adopted.biologicalMother.adopted.biologicalMother.adopted --- NO circular --- max fetch depth reached no join created
				fetchablePath Child.mother.adopted.biologicalMother.adopted.stepMother --- Circular ---
				fetchablePath Child.mother.adopted.stepMother --- Circular --
			 */
			statementInspector.assertNumberOfOccurrenceInQuery( 0, "join", 3 );
		} );
	}

	@Test
	public void testGetChild3(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Mother mother = new Mother( 10, "Strange mom" );
					session.persist( mother );
					session.get( AdoptedChild.class, 3 ).setBiologicalMother( mother );
				}
		);
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction( session -> {
			final Child child = session.get( Child.class, 2 );

			Mother mother = child.getMother();
			assertTrue(
					Hibernate.isInitialized( mother ),
					"The mother eager OneToOne association is not initialized"
			);
			assertThat( mother, notNullValue() );
			assertThat( mother.getName(), is( "Giulia" ) );

			Child biologicalChild = mother.getBiologicalChild();
			assertSame( biologicalChild, child );
			assertTrue(
					Hibernate.isInitialized( biologicalChild ),
					"The child eager OneToOne association is not initialized"
			);

			AdoptedChild adoptedChild = mother.getAdopted();
			assertThat( adoptedChild, notNullValue() );
			assertTrue(
					Hibernate.isInitialized( adoptedChild ),
					"The adoptedChild eager OneToOne association is not initialized"
			);
			assertSame( adoptedChild.getStepMother(), mother );
			assertThat( adoptedChild.getBiologicalMother(), notNullValue() );
			assertTrue(
					Hibernate.isInitialized( adoptedChild.getBiologicalMother() ),
					"The biologicalMother eager OneToOne association is not initialized"
			);
			assertThat( adoptedChild.getBiologicalMother().getName(), is( "Strange mom" ) );

			statementInspector.assertExecutedCount( 2 );
			statementInspector.assertNumberOfOccurrenceInQuery( 0, "join", 3 );
			statementInspector.assertNumberOfOccurrenceInQuery( 1, "join", 5 );
		} );
	}

	@Test
	public void testGetAdoptedChild(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction( session -> {
			final AdoptedChild adoptedChild = session.get( AdoptedChild.class, 3 );

			Mother stepMother = adoptedChild.getStepMother();
			assertTrue(
					Hibernate.isInitialized( stepMother ),
					"The stepMother eager OneToOne association is not initialized"
			);
			assertThat( stepMother, notNullValue() );
			assertThat( stepMother.getName(), is( "Giulia" ) );

			Child biologicalChild = stepMother.getBiologicalChild();
			assertThat( biologicalChild, notNullValue() );
			assertThat( biologicalChild.getId(), is(2) );
			assertTrue(
					Hibernate.isInitialized( biologicalChild ),
					"The biological eager OneToOne association is not initialized"
			);

			assertSame( adoptedChild, stepMother.getAdopted() );
			assertTrue(
					Hibernate.isInitialized( adoptedChild ),
					"The adoptedChild eager OneToOne association is not initialized"
			);
			assertThat( adoptedChild.getBiologicalMother(), nullValue() );

			statementInspector.assertExecutedCount( 1 );
			/*
				fetchablePath: AdoptedChild.biologicalMother --- NO circular ---
				fetchablePath: AdoptedChild.biologicalMother.biologicalChild --- NO circular ---
				fetchablePath: AdoptedChild.biologicalMother.biologicalChild.mother --- Circular ---
				fetchablePath: AdoptedChild.biologicalMother.adopted --- NO circular ---
				fetchablePath: AdoptedChild.biologicalMother.adopted.biologicalMother --- NO circular ---
				fetchablePath: AdoptedChild.biologicalMother.adopted.biologicalMother.biologicalChild --- NO circular --- [N.b is not circular because adopted is an instance of AdoptedChild while biologicalChild is an instance of Child]
				fetchablePath: AdoptedChild.biologicalMother.adopted.biologicalMother.adopted --- NO circular ---
				fetchablePath: AdoptedChild.biologicalMother.adopted.biologicalMother.adopted.biologicalMother --- NO circular ---
				fetchablePath: AdoptedChild.biologicalMother.adopted.biologicalMother.adopted.biologicalMother.biologicalChild --- NO circular ---
				fetchablePath: AdoptedChild.biologicalMother.adopted.biologicalMother.adopted.biologicalMother.adopted --- NO circular ---
				fetchablePath: AdoptedChild.biologicalMother.adopted.biologicalMother.adopted.stepMother --- Circular ---
				fetchablePath: AdoptedChild.biologicalMother.adopted.stepMother --- Circular ---
				fetchablePath: AdoptedChild.stepMother --- NO circular ---
				fetchablePath: AdoptedChild.stepMother.biologicalChild --- NO circular ---
				fetchablePath: AdoptedChild.stepMother.biologicalChild.mother --- Circular ---
				fetchablePath: AdoptedChild.stepMother.adopted --- Circular ---
			 */
			statementInspector.assertNumberOfOccurrenceInQuery( 0, "join", 5 );
		} );
	}

	@Test
	public void testGetChild2(SessionFactoryScope scope) {
		scope.inTransaction( session -> {

			Mother mother = new Mother( 4, "Giulia" );
			Child child = new Child( 5, "Stefano", mother );

			AdoptedChild child2 = new AdoptedChild( 7, "Fab2", mother );

			Mother biologicalMother = new Mother( 6, "Hibernate OGM" );
			child2.setBiologicalMother( biologicalMother );

			Child child3 = new Child( 8, "Carla", biologicalMother );

			session.persist( mother );
			session.persist( biologicalMother );
			session.persist( child );
			session.persist( child2 );
			session.persist( child3 );
		} );

		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction( session -> {
			final Child child = session.get( Child.class, 5 );

			Mother mother = child.getMother();
			Contracts.assertTrue(
					Hibernate.isInitialized( mother ),
					"The mother eager OneToOne association is not initialized"
			);
			assertThat( mother, notNullValue() );
			assertThat( mother.getName(), is( "Giulia" ) );

			Child child1 = mother.getBiologicalChild();
			assertSame( child1, child );
			Contracts.assertTrue(
					Hibernate.isInitialized( child1 ),
					"The child eager OneToOne association is not initialized"
			);

			AdoptedChild adoptedChild = mother.getAdopted();
			assertThat( adoptedChild, notNullValue() );
			Contracts.assertTrue(
					Hibernate.isInitialized( adoptedChild ),
					"The adoptedChild eager OneToOne association is not initialized"
			);

			Assert.assertSame( adoptedChild.getStepMother(), mother );

			Mother biologicalMother = adoptedChild.getBiologicalMother();
			assertThat( biologicalMother, notNullValue() );
			assertThat( biologicalMother.getId(), equalTo( 6 ) );

			Child anotherChild = biologicalMother.getBiologicalChild();
			assertThat( anotherChild, notNullValue() );
			assertThat( anotherChild.getId(), equalTo( 8 ) );

			Assert.assertSame( anotherChild.getMother(), biologicalMother );

			assertThat( biologicalMother.getAdopted(), nullValue() );

			statementInspector.assertExecutedCount( 3 );
			statementInspector.assertNumberOfOccurrenceInQuery( 0, "join", 3 );
			statementInspector.assertNumberOfOccurrenceInQuery( 1, "join", 5 );
			statementInspector.assertNumberOfOccurrenceInQuery( 2, "join", 3 );
		} );
	}

	@Test
	public void testHqlSelectMother(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				session -> {
					final Mother mother = session.createQuery(
							"SELECT m FROM Mother m JOIN m.biologicalChild WHERE m.id = :id",
							Mother.class
					)
							.setParameter( "id", 1 )
							.getSingleResult();

					Child child = mother.getBiologicalChild();
					assertThat( child, notNullValue() );
					assertThat( child.getName(), is( "Luis" ) );
					statementInspector.assertExecutedCount( 2 );
					statementInspector.assertNumberOfOccurrenceInQuery( 0, "join", 1 );
					// Mother.biologicalChild
					statementInspector.assertNumberOfOccurrenceInQuery( 1, "join", 5 );
				}
		);
	}

	@Test
	public void testHqlSelectChild(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				session -> {
					final String queryString = "SELECT c FROM Child c JOIN c.mother d WHERE d.id = :id";
					final Child child = session.createQuery( queryString, Child.class )
							.setParameter( "id", 1 )
							.getSingleResult();

					Mother mother = child.getMother();
					assertThat( mother, notNullValue() );

					assertThat( mother.getName(), is( "Giulia" ) );
					statementInspector.assertExecutedCount( 2 );
					statementInspector.assertNumberOfOccurrenceInQuery( 0, "join", 1 );
					statementInspector.assertNumberOfOccurrenceInQuery( 1, "join", 4 );
				}
		);
	}

	@Entity(name = "Mother")
	public static class Mother {

		@OneToOne(mappedBy = "stepMother")
		private AdoptedChild adopted;

		@OneToOne
		private Child biologicalChild;

		@Id
		private Integer id;

		private String name;

		public Mother(){

		}

		public Mother(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		Mother(Integer id) {
			this.id = id;
		}

		public AdoptedChild getAdopted() {
			return adopted;
		}

		public void setAdopted(AdoptedChild adopted) {
			this.adopted = adopted;
		}

		public Child getBiologicalChild() {
			return biologicalChild;
		}

		public void setBiologicalChild(Child biologicalChild) {
			this.biologicalChild = biologicalChild;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity(name = "Child")
	public static class Child {
		@Id
		private Integer id;

		@OneToOne(mappedBy = "biologicalChild")
		private Mother mother;

		private String name;


		Child() {

		}

		Child(Integer id, String name, Mother mother) {
			this.id = id;
			this.name = name;
			this.mother = mother;
			this.mother.setBiologicalChild( this );
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Mother getMother() {
			return mother;
		}

		public void setMother(Mother mother) {
			this.mother = mother;
		}
	}

	@Entity(name = "AdoptedChild")
	@Table(name = "ADOPTED_CHILD")
	public static class AdoptedChild {
		@Id
		private Integer id;

		@OneToOne
		private Mother biologicalMother;

		private String name;


		@OneToOne
		private Mother stepMother;

		AdoptedChild() {
		}

		AdoptedChild(Integer id, String name, Mother stepMother) {
			this.id = id;
			this.name = name;
			this.stepMother = stepMother;
			this.stepMother.setAdopted( this );
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Mother getBiologicalMother() {
			return biologicalMother;
		}

		public void setBiologicalMother(Mother biologicalMother) {
			this.biologicalMother = biologicalMother;
		}

		public Mother getStepMother() {
			return stepMother;
		}

		public void setStepMother(Mother stepMother) {
			this.stepMother = stepMother;
		}

		@Override
		public String toString() {
			return "AdoptedChild{" +
					"id=" + id +
					", name='" + name + '\'' +
					'}';
		}
	}
}
