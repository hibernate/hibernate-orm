/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.notfound;

import java.io.Serializable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Gail Badner
 */
@JiraKey(value = "HHH-12436")
@DomainModel(
		annotatedClasses = {
				OptionalEagerMappedByNotFoundTest.PersonOneToOneJoinException.class,
				OptionalEagerMappedByNotFoundTest.PersonOneToOneJoinIgnore.class,
				OptionalEagerMappedByNotFoundTest.PersonOneToOneSelectException.class,
				OptionalEagerMappedByNotFoundTest.PersonOneToOneSelectIgnore.class,
				OptionalEagerMappedByNotFoundTest.Employment.class
		}
)
@SessionFactory
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.SHOW_SQL, value = "true"),
				@Setting(name = AvailableSettings.FORMAT_SQL, value = "true")
		}
)
public class OptionalEagerMappedByNotFoundTest {

	@AfterEach
	public void deleteData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testOneToOneJoinException(SessionFactoryScope scope) {
		setupTest( PersonOneToOneJoinException.class, 1L, false, scope );
		scope.inTransaction(
				session -> {
					Person pCheck = session.find( PersonOneToOneJoinException.class, 1L );
					checkResult( pCheck );
				}
		);
	}

	@Test
	public void testOneToOneJoinIgnore(SessionFactoryScope scope) {
		setupTest( PersonOneToOneJoinIgnore.class, 1L, false, scope );
		scope.inTransaction(
				session -> {
					Person pCheck = session.find( PersonOneToOneJoinIgnore.class, 1L );
					checkResult( pCheck );
				}
		);
	}

	@Test
	public void testOneToOneSelectException(SessionFactoryScope scope) {
		setupTest( PersonOneToOneSelectException.class, 1L, false, scope );
		scope.inTransaction(
				session -> {
					Person pCheck = session.find( PersonOneToOneSelectException.class, 1L );
					checkResult( pCheck );
				}
		);
	}

	@Test
	public void testOneToOneSelectIgnore(SessionFactoryScope scope) {
		setupTest( PersonOneToOneSelectIgnore.class, 1L, false, scope );
		scope.inTransaction(
				session -> {
					Person pCheck = session.find( PersonOneToOneSelectIgnore.class, 1L );
					checkResult( pCheck );
				}
		);
	}

	private <T extends Person> void setupTest(Class<T> clazz, long id, boolean isMapsId, SessionFactoryScope scope) {
		persistData( clazz, id, isMapsId, scope );
		scope.inTransaction(
				session -> {
					Person p = session.find( clazz, id );
					assertEquals( "New York", p.getEmployment().getName() );
				}
		);

		scope.inTransaction(
				session ->
						session.createNativeQuery( "delete from Employment where id = " + id )
								.executeUpdate()
		);
	}

	private <T extends Person> void persistData(Class<T> clazz, long id, boolean isMapsId, SessionFactoryScope scope) {
		final Person person;
		try {
			person = clazz.newInstance();
		}
		catch (Exception ex) {
			throw new RuntimeException( ex );
		}

		scope.inTransaction(
				session -> {
					Employment employment = new Employment();
					employment.setId( id );
					employment.setName( "New York" );

					if ( !isMapsId ) {
						person.setId( id );
					}
					person.setName( "John Doe" );
					person.setEmployment( employment );
					employment.setPerson( person );
					session.persist( person );
				}
		);
	}

	private void checkResult(Person person) {
		assertNotNull( person );
		assertNotNull( person.getId() );
		assertNull( person.getEmployment() );
	}

	@Entity(name = "Person")
	@Inheritance(strategy = InheritanceType.JOINED)
	public abstract static class Person {
		@Id
		private Long id;

		private String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public abstract Employment getEmployment();

		public abstract void setEmployment(Employment employment);
	}

	@Entity
	@Table(name = "PersonOneToOneJoinException")
	public static class PersonOneToOneJoinException extends Person {
		@OneToOne(mappedBy = "person", cascade = CascadeType.PERSIST)
		@NotFound(action = NotFoundAction.EXCEPTION)
		@Fetch(FetchMode.JOIN)
		private Employment employment;

		public Employment getEmployment() {
			return employment;
		}

		@Override
		public void setEmployment(Employment employment) {
			this.employment = employment;
		}
	}

	@Entity
	@Table(name = "PersonOneToOneJoinIgnore")
	public static class PersonOneToOneJoinIgnore extends Person {
		@Id
		private Long id;

		@OneToOne(mappedBy = "person", cascade = CascadeType.PERSIST)
		@NotFound(action = NotFoundAction.IGNORE)
		@Fetch(FetchMode.JOIN)
		private Employment employment;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Employment getEmployment() {
			return employment;
		}

		@Override
		public void setEmployment(Employment employment) {
			this.employment = employment;
		}
	}

	@Entity
	@Table(name = "PersonOneToOneSelectException")
	public static class PersonOneToOneSelectException extends Person {
		@Id
		private Long id;

		@OneToOne(mappedBy = "person", cascade = CascadeType.PERSIST)
		@NotFound(action = NotFoundAction.EXCEPTION)
		@Fetch(FetchMode.SELECT)
		private Employment employment;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Employment getEmployment() {
			return employment;
		}

		@Override
		public void setEmployment(Employment employment) {
			this.employment = employment;
		}
	}

	@Entity
	@Table(name = "PersonOneToOneSelectIgnore")
	public static class PersonOneToOneSelectIgnore extends Person {
		@Id
		private Long id;

		@OneToOne(mappedBy = "person", cascade = CascadeType.PERSIST)
		@NotFound(action = NotFoundAction.IGNORE)
		@Fetch(FetchMode.SELECT)
		private Employment employment;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Employment getEmployment() {
			return employment;
		}

		@Override
		public void setEmployment(Employment employment) {
			this.employment = employment;
		}
	}

	@Entity(name = "Employment")
	public static class Employment implements Serializable {

		@Id
		private Long id;

		private String name;

		@OneToOne
		//@JoinColumn(foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
		private Person person;

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

		public Person getPerson() {
			return person;
		}

		public void setPerson(Person person) {
			this.person = person;
		}
	}
}
