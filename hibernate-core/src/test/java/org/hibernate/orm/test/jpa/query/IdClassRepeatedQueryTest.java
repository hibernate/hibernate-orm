/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.query;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Jpa(
		annotatedClasses = {
				IdClassRepeatedQueryTest.Corporation.class,
				IdClassRepeatedQueryTest.CorporationUser.class,
				IdClassRepeatedQueryTest.Document.class,
				IdClassRepeatedQueryTest.Person.class,
		}
)
@JiraKey( "HHH-19031" )
public class IdClassRepeatedQueryTest {

	@BeforeAll
	public void setup(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Person person = new Person("1", "Andrew");
					Corporation corporation = new Corporation("2", "ibm");
					Document document = new Document(3,"registration", person, corporation);
					entityManager.persist( person );
					entityManager.persist( corporation );
					entityManager.persist( document );
				}
		);
	}

	@Test
	public void testRepeatedQuery(EntityManagerFactoryScope scope){
		scope.inTransaction(
				entityManager -> {
					String query = "SELECT d FROM Document d";

					List<Document> resultList = entityManager.createQuery( query, Document.class ).getResultList();
					assertThat(resultList.size()).isEqualTo( 1 );
					Document document = resultList.get( 0 );

					resultList = entityManager.createQuery( query, Document.class ).getResultList();
					assertThat(resultList.size()).isEqualTo( 1 );
					assertThat( resultList.get( 0 ) ).isSameAs( document );
				}
		);
	}

	@Entity(name = "Document")
	@Table(name = "documents")
	public static class Document {
		@Id
		private Integer id;

		@Embedded
		private Owner owner;

		private String name;

		public Document() {
		}

		public Document(Integer id, String name, Person person, Corporation corporation) {
			this.id = id;
			this.name = name;
			owner = new Owner(person, corporation);
		}

		public Integer getId() {
			return id;
		}

		public Owner getOwner() {
			return owner;
		}
	}

	@Embeddable
	public static class Owner {
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "owner")
		private Person person;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "owner_corporation")
		private Corporation corporation;

		@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
		@JoinColumns({
				@JoinColumn(name = "owner_corporation", referencedColumnName = "id_corporation", insertable = false, updatable = false),
				@JoinColumn(name = "owner", referencedColumnName = "id_person", insertable = false, updatable = false),
		})
		private CorporationUser corporationUser;

		public Owner() {
		}

		public Owner(Person person, Corporation corporation) {
			this.person = person;
			this.corporation = corporation;
			this.corporationUser = new CorporationUser(person, corporation) ;
		}

		public Person getPerson() {
			return person;
		}

		public Corporation getCorporation() {
			return corporation;
		}

		public CorporationUser getCorporationUser() {
			return corporationUser;
		}
	}

	@Entity(name = "Corporation")
	@Table(name = "corporation")
	public static class Corporation  {
		@Id
		private String id;

		private String corporateName;

		public Corporation() {
		}

		public Corporation(String id, String corporateName) {
			this.id = id;
			this.corporateName = corporateName;
		}

		public String getId() {
			return id;
		}

		public String getCorporateName() {
			return corporateName;
		}
	}

	@Entity(name = "CorporationUser")
	@Table(name = "corporation_person_xref")
	@IdClass(CorporationUser.CorporationUserPK.class)
	public static class CorporationUser {
		@Id
		@ManyToOne
		@JoinColumn(name = "id_person", updatable = false)
		private Person person;

		@Id
		@ManyToOne
		@JoinColumn(name = "id_corporation", updatable = false)
		private Corporation corporation;

		public CorporationUser() {
		}

		public CorporationUser(Person person, Corporation corporation) {
			this.person = person;
			this.corporation = corporation;
		}

		public Person getPerson() {
			return person;
		}

		public Corporation getCorporation() {
			return corporation;
		}

		public static class CorporationUserPK {
			public String person;

			public String corporation;
		}
	}

	@Entity(name = "Person")
	@Table(name = "person")
	public static class Person {
		@Id
		private String id;

		private String surname;

		public Person() {
		}

		public Person(String id, String surname) {
			this.id = id;
			this.surname = surname;
		}

		public String getId() {
			return id;
		}

		public String getSurname() {
			return surname;
		}
	}
}
