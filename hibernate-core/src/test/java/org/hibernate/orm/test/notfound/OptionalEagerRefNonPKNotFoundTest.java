/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.notfound;

import java.io.Serializable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
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
				OptionalEagerRefNonPKNotFoundTest.PersonManyToOneJoinIgnore.class,
				OptionalEagerRefNonPKNotFoundTest.PersonManyToOneSelectIgnore.class,
				OptionalEagerRefNonPKNotFoundTest.PersonOneToOneJoinIgnore.class,
				OptionalEagerRefNonPKNotFoundTest.PersonOneToOneSelectIgnore.class,
				OptionalEagerRefNonPKNotFoundTest.PersonMapsIdJoinIgnore.class,
				OptionalEagerRefNonPKNotFoundTest.PersonMapsIdSelectIgnore.class,
				OptionalEagerRefNonPKNotFoundTest.PersonPkjcJoinException.class,
				OptionalEagerRefNonPKNotFoundTest.PersonPkjcJoinIgnore.class,
				OptionalEagerRefNonPKNotFoundTest.PersonPkjcSelectException.class,
				OptionalEagerRefNonPKNotFoundTest.PersonPkjcSelectIgnore.class,
				OptionalEagerRefNonPKNotFoundTest.PersonMapsIdColumnJoinIgnore.class,
				OptionalEagerRefNonPKNotFoundTest.PersonMapsIdColumnSelectIgnore.class,
				OptionalEagerRefNonPKNotFoundTest.City.class
		}
)
@SessionFactory
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.SHOW_SQL, value = "true"),
				@Setting(name = AvailableSettings.FORMAT_SQL, value = "true")
		}
)
public class OptionalEagerRefNonPKNotFoundTest {

	@AfterEach
	public void cleanUp(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testOneToOneJoinIgnore(SessionFactoryScope scope) {
		setupTest( PersonOneToOneJoinIgnore.class, 1L, false, scope );
		scope.inTransaction(
				session -> {
					Person pCheck = session.find( PersonOneToOneJoinIgnore.class, 1L );
					checkIgnore( pCheck );
				}
		);
	}

	@Test
	public void testOneToOneSelectIgnore(SessionFactoryScope scope) {
		setupTest( PersonOneToOneSelectIgnore.class, 1L, false, scope );
		scope.inTransaction(
				session -> {
					Person pCheck = session.find( PersonOneToOneSelectIgnore.class, 1L );
					checkIgnore( pCheck );
				}
		);
	}

	@Test
	public void testManyToOneJoinIgnore(SessionFactoryScope scope) {
		setupTest( PersonManyToOneJoinIgnore.class, 1L, false, scope );
		scope.inTransaction(
				session -> {
					Person pCheck = session.find( PersonManyToOneJoinIgnore.class, 1L );
					checkIgnore( pCheck );
				}
		);
	}

	@Test
	public void testManyToOneSelectIgnore(SessionFactoryScope scope) {
		setupTest( PersonManyToOneSelectIgnore.class, 1L, false, scope );
		scope.inTransaction(
				session -> {
					Person pCheck = session.find( PersonManyToOneSelectIgnore.class, 1L );
					checkIgnore( pCheck );
				}
		);
	}

	@Test
	public void testPkjcOneToOneJoinException(SessionFactoryScope scope) {
		setupTest( PersonPkjcJoinException.class, 1L, false, scope );
		scope.inTransaction(
				session -> {
					Person pCheck = session.find( PersonPkjcJoinException.class, 1L );
					// @OneToOne @PrimaryKeyJoinColumn always assumes @NotFound(IGNORE)
					checkIgnore( pCheck );
				}
		);
	}

	@Test
	public void testPkjcOneToOneJoinIgnore(SessionFactoryScope scope) {
		setupTest( PersonPkjcJoinIgnore.class, 1L, false, scope );
		scope.inTransaction(
				session -> {
					Person pCheck = session.find( PersonPkjcJoinIgnore.class, 1L );
					// Person is non-null and association is null.
					checkIgnore( pCheck );
				}
		);
	}

	@Test
	public void testPkjcOneToOneSelectException(SessionFactoryScope scope) {
		setupTest( PersonPkjcSelectException.class, 1L, false, scope );
		scope.inTransaction(
				session -> {
					Person pCheck = session.find( PersonPkjcSelectException.class, 1L );
					// @OneToOne @PrimaryKeyJoinColumn always assumes @NotFound(IGNORE)
					checkIgnore( pCheck );
				}
		);
	}

	@Test
	public void testPkjcOneToOneSelectIgnore(SessionFactoryScope scope) {
		setupTest( PersonPkjcSelectIgnore.class, 1L, false, scope );
		scope.inTransaction(
				session -> {
					Person pCheck = session.find( PersonPkjcSelectIgnore.class, 1L );
					// Person is non-null and association is null.
					checkIgnore( pCheck );
				}
		);
	}

	@Test
	public void testMapsIdOneToOneJoinIgnore(SessionFactoryScope scope) {
		setupTest( PersonMapsIdJoinIgnore.class, 1L, true, scope );
		scope.inTransaction(
				session -> {
					Person pCheck = session.find( PersonMapsIdJoinIgnore.class, 1L );
					// Person is non-null association is null.
					checkIgnore( pCheck );
				}
		);
	}

	@Test
	public void testMapsIdOneToOneSelectIgnore(SessionFactoryScope scope) {
		setupTest( PersonMapsIdSelectIgnore.class, 1L, true, scope );
		scope.inTransaction(
				session -> {
					Person pCheck = session.find( PersonMapsIdSelectIgnore.class, 1L );
					// Person is non-null association is null.
					checkIgnore( pCheck );
				}
		);
	}

	@Test
	public void testMapsIdJoinColumnOneToOneJoinIgnore(SessionFactoryScope scope) {
		setupTest( PersonMapsIdColumnJoinIgnore.class, 1L, true, scope );
		scope.inTransaction(
				session -> {
					Person pCheck = session.find( PersonMapsIdColumnJoinIgnore.class, 1L );
					checkIgnore( pCheck );
				}
		);
	}

	@Test
	public void testMapsIdJoinColumnOneToOneSelectIgnore(SessionFactoryScope scope) {
		setupTest( PersonMapsIdColumnSelectIgnore.class, 1L, true, scope );
		scope.inTransaction(
				session -> {
					Person pCheck = session.find( PersonMapsIdColumnSelectIgnore.class, 1L );
					checkIgnore( pCheck );
				}
		);
	}

	private <T extends Person> void setupTest(Class<T> clazz, long id, boolean isMapsId, SessionFactoryScope scope) {
		persistData( clazz, id, isMapsId, scope );
		scope.inTransaction(
				session -> {
					Person p = session.find( clazz, id );
					assertEquals( "New York", p.getCity().getName() );
				}
		);

		scope.inTransaction(
				session ->
						session.createNativeQuery( "delete from City where id = " + id )
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
					City city = new City();
					city.setId( id );
					city.setName( "New York" );

					if ( !isMapsId ) {
						person.setId( id );
					}
					person.setName( "John Doe" );
					person.setCity( city );
					session.persist( person );
				}
		);
	}

	private void checkIgnore(Person person) {
		assertNotNull( person );
		assertNull( person.getCity() );
	}

	private void checkException(Person person) {
		assertNull( person );
	}

	@MappedSuperclass
	public abstract static class Person {
		private String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public abstract void setId(Long id);

		public abstract City getCity();

		public abstract void setCity(City city);
	}

	@Entity
	@Table(name = "PersonOneToOneJoinException")
	public static class PersonOneToOneJoinException extends Person {
		@Id
		private Long id;

		@OneToOne(cascade = CascadeType.PERSIST)
		@JoinColumn(
				name = "cityName",
				referencedColumnName = "name",
				foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
		)
		@Fetch(FetchMode.JOIN)
		private City city;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public City getCity() {
			return city;
		}

		@Override
		public void setCity(City city) {
			this.city = city;
		}
	}

	@Entity
	@Table(name = "PersonOneToOneJoinIgnore")
	public static class PersonOneToOneJoinIgnore extends Person {
		@Id
		private Long id;

		@OneToOne(cascade = CascadeType.PERSIST)
		@NotFound(action = NotFoundAction.IGNORE)
		@JoinColumn(
				name = "cityName",
				referencedColumnName = "name",
				foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
		)
		@Fetch(FetchMode.JOIN)
		private City city;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public City getCity() {
			return city;
		}

		@Override
		public void setCity(City city) {
			this.city = city;
		}
	}

	@Entity
	@Table(name = "PersonOneToOneSelectException")
	public static class PersonOneToOneSelectException extends Person {
		@Id
		private Long id;

		@OneToOne(cascade = CascadeType.PERSIST)
		@JoinColumn(
				name = "cityName",
				referencedColumnName = "name",
				foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
		)
		@Fetch(FetchMode.SELECT)
		private City city;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public City getCity() {
			return city;
		}

		@Override
		public void setCity(City city) {
			this.city = city;
		}
	}

	@Entity
	@Table(name = "PersonOneToOneSelectIgnore")
	public static class PersonOneToOneSelectIgnore extends Person {
		@Id
		private Long id;

		@OneToOne(cascade = CascadeType.PERSIST)
		@NotFound(action = NotFoundAction.IGNORE)
		@JoinColumn(
				name = "cityName",
				referencedColumnName = "name",
				foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
		)
		@Fetch(FetchMode.SELECT)
		private City city;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public City getCity() {
			return city;
		}

		@Override
		public void setCity(City city) {
			this.city = city;
		}
	}

	@Entity
	@Table(name = "PersonManyToOneJoinException")
	public static class PersonManyToOneJoinException extends Person {
		@Id
		private Long id;

		@ManyToOne(cascade = CascadeType.PERSIST)
		@JoinColumn(
				name = "cityName",
				referencedColumnName = "name",
				foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
		)
		@Fetch(FetchMode.JOIN)
		private City city;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public City getCity() {
			return city;
		}

		@Override
		public void setCity(City city) {
			this.city = city;
		}
	}

	@Entity
	@Table(name = "PersonManyToOneJoinIgnore")
	public static class PersonManyToOneJoinIgnore extends Person {
		@Id
		private Long id;

		@ManyToOne(cascade = CascadeType.PERSIST)
		@NotFound(action = NotFoundAction.IGNORE)
		@JoinColumn(
				name = "cityName",
				referencedColumnName = "name",
				foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
		)
		@Fetch(FetchMode.JOIN)
		private City city;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public City getCity() {
			return city;
		}

		@Override
		public void setCity(City city) {
			this.city = city;
		}
	}

	@Entity
	@Table(name = "PersonManyToOneSelectException")
	public static class PersonManyToOneSelectException extends Person {
		@Id
		private Long id;

		@ManyToOne(cascade = CascadeType.PERSIST)
		@JoinColumn(
				name = "cityName",
				referencedColumnName = "name",
				foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
		)
		@Fetch(FetchMode.SELECT)
		private City city;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public City getCity() {
			return city;
		}

		@Override
		public void setCity(City city) {
			this.city = city;
		}
	}

	@Entity
	@Table(name = "PersonManyToOneSelectIgnore")
	public static class PersonManyToOneSelectIgnore extends Person {
		@Id
		private Long id;

		@ManyToOne(cascade = CascadeType.PERSIST)
		@NotFound(action = NotFoundAction.IGNORE)
		@JoinColumn(
				name = "cityName",
				referencedColumnName = "name",
				foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
		)
		@Fetch(FetchMode.SELECT)
		private City city;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public City getCity() {
			return city;
		}

		@Override
		public void setCity(City city) {
			this.city = city;
		}
	}

	@Entity
	@Table(name = "PersonPkjcJoinException")
	public static class PersonPkjcJoinException extends Person {
		@Id
		private Long id;

		@OneToOne(cascade = CascadeType.PERSIST)
		@PrimaryKeyJoinColumn
		@Fetch(FetchMode.JOIN)
		@JoinColumn(
				name = "cityName",
				referencedColumnName = "name",
				foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
		)
		private City city;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public City getCity() {
			return city;
		}

		@Override
		public void setCity(City city) {
			this.city = city;
		}
	}

	@Entity
	@Table(name = "PersonPkjcJoinIgnore")
	public static class PersonPkjcJoinIgnore extends Person {
		@Id
		private Long id;

		@OneToOne(cascade = CascadeType.PERSIST)
		@PrimaryKeyJoinColumn
		@NotFound(action = NotFoundAction.IGNORE)
		@Fetch(FetchMode.JOIN)
		@JoinColumn(
				name = "cityName",
				referencedColumnName = "name",
				foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
		)
		private City city;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public City getCity() {
			return city;
		}

		@Override
		public void setCity(City city) {
			this.city = city;
		}
	}

	@Entity
	@Table(name = "PersonPkjcSelectException")
	public static class PersonPkjcSelectException extends Person {
		@Id
		private Long id;

		@OneToOne(cascade = CascadeType.PERSIST)
		@PrimaryKeyJoinColumn
		@Fetch(FetchMode.SELECT)
		@JoinColumn(
				name = "cityName",
				referencedColumnName = "name",
				foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
		)
		private City city;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public City getCity() {
			return city;
		}

		@Override
		public void setCity(City city) {
			this.city = city;
		}
	}

	@Entity
	@Table(name = "PersonPkjcSelectIgnore")
	public static class PersonPkjcSelectIgnore extends Person {
		@Id
		private Long id;

		@OneToOne(cascade = CascadeType.PERSIST)
		@PrimaryKeyJoinColumn
		@NotFound(action = NotFoundAction.IGNORE)
		@Fetch(FetchMode.SELECT)
		@JoinColumn(
				name = "cityName",
				referencedColumnName = "name",
				foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
		)
		private City city;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public City getCity() {
			return city;
		}

		@Override
		public void setCity(City city) {
			this.city = city;
		}
	}

	@Entity
	@Table(name = "PersonMapsIdJoinException")
	public static class PersonMapsIdJoinException extends Person {
		@Id
		private Long id;

		@OneToOne(cascade = CascadeType.PERSIST)
		@MapsId
		@Fetch(FetchMode.JOIN)
		@JoinColumn(
				name = "cityName",
				referencedColumnName = "name",
				foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
		)
		private City city;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public City getCity() {
			return city;
		}

		@Override
		public void setCity(City city) {
			this.city = city;
		}
	}

	@Entity
	@Table(name = "PersonMapsIdJoinIgnore")
	public static class PersonMapsIdJoinIgnore extends Person {
		@Id
		private Long id;

		@OneToOne(cascade = CascadeType.PERSIST)
		@MapsId
		@NotFound(action = NotFoundAction.IGNORE)
		@Fetch(FetchMode.JOIN)
		@JoinColumn(
				name = "cityName",
				referencedColumnName = "name",
				foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
		)
		private City city;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public City getCity() {
			return city;
		}

		@Override
		public void setCity(City city) {
			this.city = city;
		}
	}

	@Entity
	@Table(name = "PersonMapsIdSelectException")
	public static class PersonMapsIdSelectException extends Person {
		@Id
		private Long id;

		@OneToOne(cascade = CascadeType.PERSIST)
		@MapsId
		@Fetch(FetchMode.SELECT)
		@JoinColumn(
				name = "cityName",
				referencedColumnName = "name",
				foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
		)
		private City city;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public City getCity() {
			return city;
		}

		@Override
		public void setCity(City city) {
			this.city = city;
		}
	}

	@Entity
	@Table(name = "PersonMapsIdSelectIgnore")
	public static class PersonMapsIdSelectIgnore extends Person {
		@Id
		private Long id;

		@OneToOne(cascade = CascadeType.PERSIST)
		@MapsId
		@NotFound(action = NotFoundAction.IGNORE)
		@Fetch(FetchMode.SELECT)
		@JoinColumn(
				name = "cityName",
				referencedColumnName = "name",
				foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
		)
		private City city;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public City getCity() {
			return city;
		}

		@Override
		public void setCity(City city) {
			this.city = city;
		}
	}

	@Entity
	@Table(name = "PersonMapsIdColumnJoinException")
	public static class PersonMapsIdColumnJoinException extends Person {
		@Id
		private Long id;

		@OneToOne(cascade = CascadeType.PERSIST)
		@MapsId
		@JoinColumn(
				name = "cityName",
				referencedColumnName = "name",
				foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
		)
		@Fetch(FetchMode.JOIN)
		private City city;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public City getCity() {
			return city;
		}

		@Override
		public void setCity(City city) {
			this.city = city;
		}
	}

	@Entity
	@Table(name = "PersonMapsIdColumnJoinIgnore")
	public static class PersonMapsIdColumnJoinIgnore extends Person {
		@Id
		private Long id;

		@OneToOne(cascade = CascadeType.PERSIST)
		@MapsId
		@NotFound(action = NotFoundAction.IGNORE)
		@JoinColumn(
				name = "cityName",
				referencedColumnName = "name",
				foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
		)
		@Fetch(FetchMode.JOIN)
		private City city;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public City getCity() {
			return city;
		}

		@Override
		public void setCity(City city) {
			this.city = city;
		}
	}

	@Entity
	@Table(name = "PersonMapsIdColumnSelectException")
	public static class PersonMapsIdColumnSelectException extends Person {
		@Id
		private Long id;

		@OneToOne(cascade = CascadeType.PERSIST)
		@MapsId
		@JoinColumn(
				name = "cityName",
				referencedColumnName = "name",
				foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
		)
		@Fetch(FetchMode.SELECT)
		private City city;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public City getCity() {
			return city;
		}

		@Override
		public void setCity(City city) {
			this.city = city;
		}
	}

	@Entity
	@Table(name = "PersonMapsIdColumnSelectIgnore")
	public static class PersonMapsIdColumnSelectIgnore extends Person {
		@Id
		private Long id;

		@OneToOne(cascade = CascadeType.PERSIST)
		@MapsId
		@NotFound(action = NotFoundAction.IGNORE)
		@JoinColumn(
				name = "cityName",
				referencedColumnName = "name",
				foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
		)
		@Fetch(FetchMode.SELECT)
		private City city;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public City getCity() {
			return city;
		}

		@Override
		public void setCity(City city) {
			this.city = city;
		}
	}

	@Entity
	@Table(name = "City")
	public static class City implements Serializable {

		@Id
		private Long id;

		private String name;

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
	}
}
