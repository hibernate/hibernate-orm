/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.ops;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

@JiraKey( value = "HHH-14608")
@Jpa(
		annotatedClasses = {
				MergeJpaComplianceTest.Person.class,
				MergeJpaComplianceTest.Occupation.class,
				MergeJpaComplianceTest.PersonOccupation.class
		},
		integrationSettings = {@Setting(name = AvailableSettings.JPA_PROXY_COMPLIANCE, value = "true")}
)
public class MergeJpaComplianceTest {

	@Test
	public void testMerge(EntityManagerFactoryScope scope) {
		Person person = scope.fromTransaction(
				entityManager -> {
					Person p;
					p = new Person( "1", "Fab" );
					Occupation t = new Occupation( 1l, "Some work" );

					entityManager.persist( p );
					entityManager.persist( t );

					entityManager.flush();

					PersonOccupation participant = new PersonOccupation( p, t );
					entityManager.persist( participant );
					return p;
				}
		);

		scope.inTransaction(
				entityManager -> {
					person.setName( "Fabiana" );
					entityManager.merge( person );
				}
		);
	}

	@Entity(name = "Person")
	public static class Person {
		@Id
		private String id;

		private String name;

		@OneToMany(mappedBy = "pk.person", cascade = CascadeType.ALL, orphanRemoval = true)
		private List<PersonOccupation> occupations;

		public Person() {

		}

		public Person(String id, String name) {
			this.id = id;
			this.name = name;
		}

		protected void setName(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public List<PersonOccupation> getOccupations() {
			return occupations;
		}

		protected void addOccupationPeople(PersonOccupation personOccupation) {
			if ( this.occupations == null ) {
				occupations = new ArrayList<>();
			}
			this.occupations.add( personOccupation );
			personOccupation.getPk().setPerson( this );
		}

		protected void setOccupations(List<PersonOccupation> occupations) {
			this.occupations = occupations;
		}
	}

	@Entity(name = "Occupation")
	public static class Occupation {
		@Id
		private long id;

		private String name;

		@OneToMany(mappedBy = "pk.occupation", cascade = CascadeType.ALL)
		private List<PersonOccupation> personOccupations;

		protected Occupation() {
		}

		public Occupation(long id, String name) {
			this.id = id;
			this.name = name;
		}

		public long getId() {
			return id;
		}

		protected void setId(long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		protected void setName(String name) {
			this.name = name;
		}

		public List<PersonOccupation> getPersonOccupations() {
			return personOccupations;
		}

		protected void addPersonOccupation(PersonOccupation participant) {
			if ( personOccupations == null ) {
				personOccupations = new ArrayList<>();
			}
			personOccupations.add( participant );
			participant.getPk().setOccupation( this );
		}

		protected void setPersonOccupations(List<PersonOccupation> personOccupations) {
			this.personOccupations = personOccupations;
		}
	}

	@Entity(name = "PersonOccupation")
	public static class PersonOccupation {
		@EmbeddedId
		private PersonOccupationPK pk = new PersonOccupationPK();

		protected PersonOccupation() {
		}

		public PersonOccupation(Person person, Occupation occupation) {
			person.addOccupationPeople( this );
			occupation.addPersonOccupation( this );
		}

		public PersonOccupationPK getPk() {
			return pk;
		}

		public void setPk(PersonOccupationPK pk) {
			this.pk = pk;
		}
	}

	@Embeddable
	public static class PersonOccupationPK implements Serializable {

		@ManyToOne(fetch = FetchType.LAZY)
		private Person person;

		@ManyToOne(fetch = FetchType.LAZY)
		private Occupation occupation;

		public Person getPerson() {
			return person;
		}

		public void setPerson(Person person) {
			this.person = person;
		}

		public Occupation getOccupation() {
			return occupation;
		}

		public void setOccupation(Occupation occupation) {
			this.occupation = occupation;
		}
	}

}
