package org.hibernate.jpa.test.ops;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.persistence.CascadeType;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil2.fromTransaction;
import static org.hibernate.testing.transaction.TransactionUtil2.inTransaction;

@TestForIssue( jiraKey = "HHH-14608")
public class MergeJpaComplianceTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Person.class, Occupation.class, PersonOccupation.class
		};
	}

	@Override
	protected void addConfigOptions(Map config) {
		config.put( org.hibernate.cfg.AvailableSettings.JPA_PROXY_COMPLIANCE, true );
	}

	@Test
	public void testMerge() {
		Person person = fromTransaction(
				entityManagerFactory(),
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

		inTransaction(
				entityManagerFactory(),
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

		protected void addOccupationPeoplet(PersonOccupation personOccupation) {
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
			person.addOccupationPeoplet( this );
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
