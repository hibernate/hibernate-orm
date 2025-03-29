/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.collectionelement;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DomainModel(
		annotatedClasses = {
				OrphanRemovalOfAnEntityWithElementCollectionTest.Credit.class,
				OrphanRemovalOfAnEntityWithElementCollectionTest.Person.class
		}
)
@SessionFactory
@JiraKey(value = "HHH-15159")
public class OrphanRemovalOfAnEntityWithElementCollectionTest {

	@Test
	public void testDeleteCredit(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Credit credit = new Credit();
					credit.setReasons( new HashSet<>( Arrays.asList( "one", "two" ) ) );
					session.persist( credit );
				}
		);

		scope.inTransaction(
				session -> {
					Credit credit = session.createQuery( "from Credit", Credit.class ).list().get( 0 );
					session.remove( credit );
				}
		);
	}

	@Test
	public void testRemoveCreditFromPerson(SessionFactoryScope scope) {
		final Long firstPersonKey = scope.fromTransaction(
				session -> {
					Credit credit = new Credit();
					credit.setReasons( new HashSet<>( Arrays.asList( "one", "two" ) ) );

					Person person = new Person();
					person.setName( "William" );
					person.addCredit( credit );

					session.persist( person );

					return person.getId();
				}
		);

		scope.inTransaction(
				session -> {
					Person person = session.find( Person.class, firstPersonKey );
					person.removeCredit();

					session.persist( person );
				}
		);

		scope.inTransaction(
				session -> {
					List<Credit> credits = session.createQuery( "from Credit", Credit.class ).list();
					assertThat( credits.size() ).isEqualTo(0);
				}
		);
	}

	@Entity(name = "Credit")
	public static class Credit {
		@Id
		@GeneratedValue
		private Long id;

		@OneToOne(fetch = FetchType.LAZY)
		private Person person;

		@ElementCollection
		private Set<String> reasons;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Person getPerson() {
			return person;
		}

		public void setPerson(Person person) {
			this.person = person;
		}

		public Set<String> getReasons() {
			return reasons;
		}

		public void setReasons(Set<String> reasons) {
			this.reasons = reasons;
		}
	}

	@Entity(name = "Person")
	public static class Person {
		@Id
		@GeneratedValue
		private Long id;

		private String name;

		@OneToOne(mappedBy = "person", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
		private Credit credit;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Credit getCredit() {
			return credit;
		}

		public void setCredit(Credit credit) {
			this.credit = credit;
		}

		public void addCredit(Credit credit) {
			credit.setPerson( this );
			this.credit = credit;
		}

		public void removeCredit() {
			this.credit.setPerson( null );
			this.credit = null;
		}
	}
}
