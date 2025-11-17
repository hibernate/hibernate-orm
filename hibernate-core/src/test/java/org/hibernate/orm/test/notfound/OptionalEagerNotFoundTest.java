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
				OptionalEagerNotFoundTest.PersonManyToOneJoinIgnore.class,
				OptionalEagerNotFoundTest.PersonManyToOneSelectIgnore.class,
				OptionalEagerNotFoundTest.PersonOneToOneJoinIgnore.class,
				OptionalEagerNotFoundTest.PersonOneToOneSelectIgnore.class,
				OptionalEagerNotFoundTest.PersonMapsIdJoinIgnore.class,
				OptionalEagerNotFoundTest.PersonMapsIdSelectIgnore.class,
				OptionalEagerNotFoundTest.PersonPkjcJoinException.class,
				OptionalEagerNotFoundTest.PersonPkjcJoinIgnore.class,
				OptionalEagerNotFoundTest.PersonPkjcSelectException.class,
				OptionalEagerNotFoundTest.PersonPkjcSelectIgnore.class,
				OptionalEagerNotFoundTest.PersonMapsIdColumnJoinIgnore.class,
				OptionalEagerNotFoundTest.PersonMapsIdColumnSelectIgnore.class,
				OptionalEagerNotFoundTest.City.class
		}
)
@SessionFactory
@ServiceRegistry(settings = {
		@Setting(name = AvailableSettings.SHOW_SQL, value = "true"),
		@Setting(name = AvailableSettings.FORMAT_SQL, value = "true")
})
public class OptionalEagerNotFoundTest {

	@AfterEach
	public void cleanUp(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testOneToOneJoinIgnore(SessionFactoryScope scope) {
		setupTest( PersonOneToOneJoinIgnore.class, 1L, false, scope );
		executeIgnoreTest( PersonOneToOneJoinIgnore.class, 1L, scope );
	}

	@Test
	public void testOneToOneSelectIgnore(SessionFactoryScope scope) {
		setupTest( PersonOneToOneSelectIgnore.class, 1L, false, scope );
		executeIgnoreTest( PersonOneToOneSelectIgnore.class, 1L, scope );
	}

	@Test
	public void testManyToOneJoinIgnore(SessionFactoryScope scope) {
		setupTest( PersonManyToOneJoinIgnore.class, 1L, false, scope );
		executeIgnoreTest( PersonManyToOneJoinIgnore.class, 1L, scope );
	}

	@Test
	public void testManyToOneSelectIgnore(SessionFactoryScope scope) {
		setupTest( PersonManyToOneSelectIgnore.class, 1L, false, scope );
		executeIgnoreTest( PersonManyToOneSelectIgnore.class, 1L, scope );
	}

	@Test
	public void testPkjcOneToOneJoinException(SessionFactoryScope scope) {
		setupTest( PersonPkjcJoinException.class, 1L, false, scope );
		// optional @OneToOne @PKJC implicitly maps @NotFound(IGNORE)
		executeIgnoreTest( PersonPkjcJoinException.class, 1L, scope );
	}

	@Test
	public void testPkjcOneToOneJoinIgnore(SessionFactoryScope scope) {
		setupTest( PersonPkjcJoinIgnore.class, 1L, false, scope );
		executeIgnoreTest( PersonPkjcJoinIgnore.class, 1L, scope );
	}

	@Test
	public void testPkjcOneToOneSelectException(SessionFactoryScope scope) {
		setupTest( PersonPkjcSelectException.class, 1L, false, scope );
		// optional @OneToOne @PKJC implicitly maps @NotFound(IGNORE)
		executeIgnoreTest( PersonPkjcSelectException.class, 1L, scope );
	}

	@Test
	public void testPkjcOneToOneSelectIgnore(SessionFactoryScope scope) {
		setupTest( PersonPkjcSelectIgnore.class, 1L, false, scope );
		executeIgnoreTest( PersonPkjcSelectIgnore.class, 1L, scope );
	}

	@Test
	public void testMapsIdOneToOneJoinIgnore(SessionFactoryScope scope) {
		setupTest( PersonMapsIdJoinIgnore.class, 1L, true, scope );
		executeIgnoreTest( PersonMapsIdJoinIgnore.class, 1L, scope );
	}

	@Test
	public void testMapsIdOneToOneSelectIgnore(SessionFactoryScope scope) {
		setupTest( PersonMapsIdSelectIgnore.class, 1L, true, scope );
		executeIgnoreTest( PersonMapsIdSelectIgnore.class, 1L, scope );
	}

	@Test
	public void testMapsIdJoinColumnOneToOneJoinIgnore(SessionFactoryScope scope) {
		setupTest( PersonMapsIdColumnJoinIgnore.class, 1L, true, scope );
		executeIgnoreTest( PersonMapsIdColumnJoinIgnore.class, 1L, scope );
	}

	@Test
	public void testMapsIdJoinColumnOneToOneSelectIgnore(SessionFactoryScope scope) {
		setupTest( PersonMapsIdColumnSelectIgnore.class, 1L, true, scope );
		executeIgnoreTest( PersonMapsIdColumnSelectIgnore.class, 1L, scope );
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

	private <T extends Person> void executeIgnoreTest(Class<T> clazz, long id, SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Person pCheck = session.find( clazz, id );
					checkResult( pCheck );
					pCheck.setName( "Jane Doe" );
				}
		);
		scope.inTransaction(
				session -> {
					Person pCheck = session.find( clazz, id );
					assertEquals( "Jane Doe", pCheck.getName() );
					checkResult( pCheck );
					pCheck.setCity( null );
				}
		);
		scope.inTransaction(
				session -> {
					Person pCheck = session.find( clazz, id );
					assertEquals( "Jane Doe", pCheck.getName() );
					checkResult( pCheck );
				}
		);
	}

	private void checkResult(Person person) {
		assertNotNull( person );
		assertNull( person.getCity() );
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
	@Table(name = "PersonOneToOneJoinIgnore")
	public static class PersonOneToOneJoinIgnore extends Person {
		@Id
		private Long id;

		@OneToOne(cascade = CascadeType.PERSIST)
		@NotFound(action = NotFoundAction.IGNORE)
		@JoinColumn(foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
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
	@Table(name = "PersonOneToOneSelectIgnore")
	public static class PersonOneToOneSelectIgnore extends Person {
		@Id
		private Long id;

		@OneToOne(cascade = CascadeType.PERSIST)
		@NotFound(action = NotFoundAction.IGNORE)
		@JoinColumn(foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
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
	@Table(name = "PersonManyToOneJoinIgnore")
	public static class PersonManyToOneJoinIgnore extends Person {
		@Id
		private Long id;

		@ManyToOne(cascade = CascadeType.PERSIST)
		@NotFound(action = NotFoundAction.IGNORE)
		@JoinColumn(foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
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
	@Table(name = "PersonManyToOneSelectIgnore")
	public static class PersonManyToOneSelectIgnore extends Person {
		@Id
		private Long id;

		@ManyToOne(cascade = CascadeType.PERSIST)
		@NotFound(action = NotFoundAction.IGNORE)
		@JoinColumn(foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
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
		@JoinColumn(foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
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
		@JoinColumn(foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
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
		@JoinColumn(foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
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
		@JoinColumn(foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
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
		@JoinColumn(foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
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
		@JoinColumn(foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
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
		@JoinColumn(name = "fk", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
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
	@Table(name = "PersonMapsIdColumnSelectIgnore")
	public static class PersonMapsIdColumnSelectIgnore extends Person {
		@Id
		private Long id;

		@OneToOne(cascade = CascadeType.PERSIST)
		@MapsId
		@NotFound(action = NotFoundAction.IGNORE)
		@JoinColumn(name = "fk", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
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
