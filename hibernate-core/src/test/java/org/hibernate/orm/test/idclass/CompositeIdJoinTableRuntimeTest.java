package org.hibernate.orm.test.idclass;

import java.io.Serializable;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.OneToOne;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DomainModel(
		annotatedClasses = {
				CompositeIdJoinTableRuntimeTest.Person.class,
				CompositeIdJoinTableRuntimeTest.Passport.class
		}
)
@SessionFactory
@Jira("https://hibernate.atlassian.net/browse/HHH-20961")
public class CompositeIdJoinTableRuntimeTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createMutationQuery("delete from Person").executeUpdate();
					session.createMutationQuery("delete from Passport").executeUpdate();
				}
		);
	}

	@Test
	public void testCompositeIdJoinTableWithData(SessionFactoryScope scope) {
		Long personId1 = 1L;
		Long personId2 = 2L;
		Long passportId1 = 100L;
		Long passportId2 = 200L;

		scope.inTransaction(
				session -> {
					Passport passport = new Passport();
					passport.pa_id = passportId1;
					passport.pa_id2 = passportId2;
					passport.number = "ABC123";
					session.persist(passport);

					Person person = new Person();
					person.idclass_id = personId1;
					person.idclass_id2 = personId2;
					person.name = "John Doe";
					person.passport = passport;
					session.persist(person);
				}
		);

		scope.inTransaction(
				session -> {
					Ids personKey = new Ids();
					personKey.idclass_id = personId1;
					personKey.idclass_id2 = personId2;
					Person person = session.find(Person.class, personKey);
					assertNotNull(person);
					assertEquals("John Doe", person.name);
					assertNotNull(person.passport);
					assertEquals("ABC123", person.passport.number);
					assertEquals(passportId1, person.passport.pa_id);
					assertEquals(passportId2, person.passport.pa_id2);
				}
		);
	}

	public static class Ids implements Serializable {
		public Long idclass_id;
		public Long idclass_id2;
	}

	@Entity(name = "Person")
	@IdClass(Ids.class)
	public static class Person {
		@Id
		public Long idclass_id;
		@Id
		public Long idclass_id2;
		public String name;
		@OneToOne
		@JoinTable(joinColumns = {
				@JoinColumn(referencedColumnName = "idclass_id"),
				@JoinColumn(referencedColumnName = "idclass_id2")
		}, inverseJoinColumns = {
				@JoinColumn(referencedColumnName = "pa_id"),
				@JoinColumn(referencedColumnName = "pa_id2")
		})
		public Passport passport;
	}

	public static class PaIds implements Serializable {
		public Long pa_id;
		public Long pa_id2;
	}

	@Entity(name = "Passport")
	@IdClass(PaIds.class)
	public static class Passport {
		@Id
		public Long pa_id;
		@Id
		public Long pa_id2;
		public String number;
	}
}
