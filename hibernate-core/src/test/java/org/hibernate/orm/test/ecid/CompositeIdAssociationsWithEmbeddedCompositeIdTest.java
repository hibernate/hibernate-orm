/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.ecid;

import java.io.Serializable;
import java.util.Iterator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import org.hibernate.annotations.processing.Exclude;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Gail Badner
 */
@DomainModel(
		annotatedClasses = {
				CompositeIdAssociationsWithEmbeddedCompositeIdTest.Parent.class,
				CompositeIdAssociationsWithEmbeddedCompositeIdTest.Person.class
		}
)
@SessionFactory
@Exclude
public class CompositeIdAssociationsWithEmbeddedCompositeIdTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope){
		scope.inTransaction(
				session -> {
					session.createQuery( "delete from Person" ).executeUpdate();
					session.createQuery( "delete from Parent" ).executeUpdate();
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-13114")
	public void testQueries(SessionFactoryScope scope) {
		Parent parent1 = new Parent( "Jane", 0 );
		Parent parent2 = new Parent( "Jim", 1 );
		Person person = scope.fromTransaction(
				session -> {
					Person p = new Person();
					p.setParent1( parent1 );
					p.setParent2( parent2 );
					p.setBirthOrder( 0 );
					session.persist( parent1 );
					session.persist( parent2 );
					session.persist( p );
					return p;
				} );

		scope.inTransaction(
				session ->
						checkResult( session.get( Person.class, person ) )
		);


		scope.inTransaction(
				session ->
						checkResult( session.createQuery( "from Person p", Person.class ).getSingleResult() )
		);

		scope.inTransaction(
				session -> {
					Iterator<Person> iterator = session.createQuery( "from Person p", Person.class ).list().iterator();
					assertTrue( iterator.hasNext() );
					Person p = iterator.next();
					checkResult( p );
					assertFalse( iterator.hasNext() );
				} );
	}

	private void checkResult(Person p) {
		assertEquals( "Jane", p.getParent1().name );
		assertEquals( 0, p.getParent1().index );
		assertEquals( "Jim", p.getParent2().name );
		assertEquals( 1, p.getParent2().index );
	}

	@Entity(name = "Person")
	public static class Person implements Serializable {
		@Id
		@JoinColumn(name = "p1Name")
		@JoinColumn(name = "p1Index")
		@ManyToOne
		private Parent parent1;

		@Id
		@JoinColumn(name = "p2Name")
		@JoinColumn(name = "p2Index")
		@ManyToOne
		private Parent parent2;

		@Id
		private int birthOrder;

		private String name;

		public Person() {
		}

		public Person(String name, Parent parent1, Parent parent2) {
			this();
			setName( name );
			this.parent1 = parent1;
			this.parent2 = parent2;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Parent getParent1() {
			return parent1;
		}

		public void setParent1(Parent parent1) {
			this.parent1 = parent1;
		}

		public Parent getParent2() {
			return parent2;
		}

		public void setParent2(Parent parent2) {
			this.parent2 = parent2;
		}

		public int getBirthOrder() {
			return birthOrder;
		}

		public void setBirthOrder(int birthOrder) {
			this.birthOrder = birthOrder;
		}
	}

	@Entity(name = "Parent")
	public static class Parent implements Serializable {
		@Id
		private String name;

		@Id
		@Column(name = "ind")
		private int index;

		public Parent() {
		}

		public Parent(String name, int index) {
			this.name = name;
			this.index = index;
		}
	}
}
