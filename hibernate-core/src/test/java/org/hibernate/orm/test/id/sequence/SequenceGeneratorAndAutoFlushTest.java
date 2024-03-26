package org.hibernate.orm.test.id.sequence;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Session;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

/**
 * This template demonstrates how to develop a test case for Hibernate ORM, using the Java Persistence API.
 */
@Jpa(annotatedClasses = {
		SequenceGeneratorAndAutoFlushTest.Account.class,
		SequenceGeneratorAndAutoFlushTest.Person.class,
		SequenceGeneratorAndAutoFlushTest.Ticket.class
})
@JiraKey("HHH-17073")
public class SequenceGeneratorAndAutoFlushTest {

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope){
		scope.inTransaction(
				entityManager -> {
					entityManager.createQuery( "delete from Person" ).executeUpdate();
					entityManager.createQuery( "delete from Account" ).executeUpdate();
				}
		);
	}

	@Test
	public void testCriteriaAutoFlush(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					final Account account = new Account( 1l );
					entityManager.persist( account );

					final Person person = new Person( account );
					account.addChild( person );

					CriteriaBuilder cb = entityManager.getCriteriaBuilder();
					CriteriaQuery<Long> cq = cb.createQuery( Long.class );
					Root<Ticket> ticket = cq.from( Ticket.class );
					cq.select( cb.count( ticket ) ).where( ticket.get( "owner" ).in( List.of( person ) ) );
					entityManager.createQuery( cq ).getSingleResult();

					final Person person2 = new Person( account );
					account.addChild( person2 );

					cb = entityManager.getCriteriaBuilder();
					cq = cb.createQuery( Long.class );
					ticket = cq.from( Ticket.class );
					cq.select( cb.count( ticket ) ).where( ticket.get( "owner" ).in( List.of( person2 ) ) );
					entityManager.createQuery( cq ).getSingleResult();
				}
		);
	}

	@Test
	public void testScrollAutoFlush(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					final Account account = new Account( 1l );
					entityManager.persist( account );

					final Person person = new Person( account );
					account.addChild( person );

					(entityManager.unwrap( Session.class )).createQuery( "select t from Ticket t where t.owner in (:owners)" ).setParameter( "owners", List.of( person ) ).scroll();
				}
		);
	}

	@Test
	public void testStreamAutoFlush(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					final Account account = new Account( 1l );
					entityManager.persist( account );

					final Person person = new Person( account );
					account.addChild( person );

					(entityManager.unwrap( Session.class )).createQuery( "select t from Ticket t where t.owner in (:owners)" ).setParameter( "owners", List.of( person ) ).stream();
				}
		);
	}

	@Entity(name = "Account")
	public static class Account {

		@Id
		private long id;

		@OneToMany(mappedBy = "account", cascade = { CascadeType.PERSIST, CascadeType.MERGE })
		private Set<Person> children = new HashSet<>();

		public Account(long id) {
			this.id = id;
		}

		public Account() {
		}

		public void addChild(final Person child) {
			children.add( child );
		}
	}

	@Entity(name = "Person")
	public static class Person {

		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE)
		private Long id;

		@ManyToOne(fetch = FetchType.LAZY, cascade = { CascadeType.ALL })
		protected Account account;

		public Person() {
		}

		public Person(final Account parent) {
			this.account = parent;
		}
	}

	@Entity(name = "Ticket")
	public static class Ticket {

		@Id
		private long id;

		@ManyToOne(fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.MERGE })
		private Person owner;

		public Ticket() {
		}

		public Ticket(final Person owner) {
			this.owner = owner;
		}
	}
}
