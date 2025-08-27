/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.notfound;

import java.io.Serializable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

import org.hibernate.Hibernate;
import org.hibernate.ObjectNotFoundException;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Gail Badner
 */
@JiraKey(value = "HHH-12436")
@DomainModel(
		annotatedClasses = {
				OptionalLazyNotFoundTest.PersonManyToOneSelectException.class,
				OptionalLazyNotFoundTest.PersonManyToOneSelectIgnore.class,
				OptionalLazyNotFoundTest.PersonOneToOneSelectException.class,
				OptionalLazyNotFoundTest.PersonOneToOneSelectIgnore.class,
				OptionalLazyNotFoundTest.PersonMapsIdSelectException.class,
				OptionalLazyNotFoundTest.PersonMapsIdSelectIgnore.class,
				OptionalLazyNotFoundTest.PersonPkjcSelectException.class,
				OptionalLazyNotFoundTest.PersonPkjcSelectIgnore.class,
				OptionalLazyNotFoundTest.PersonMapsIdColumnSelectIgnore.class,
				OptionalLazyNotFoundTest.PersonMapsIdColumnSelectException.class,
				OptionalLazyNotFoundTest.City.class
		}
)
@SessionFactory
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.SHOW_SQL, value = "true"),
				@Setting(name = AvailableSettings.FORMAT_SQL, value = "true")
		}
)
public class OptionalLazyNotFoundTest {

	@AfterEach
	public void cleanUp(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testOneToOneSelectException(SessionFactoryScope scope) {
		setupTest( PersonOneToOneSelectException.class, 1L, false, scope );
		scope.inTransaction(
				session -> {
					Person pCheck = session.find( PersonOneToOneSelectException.class, 1L );
					assertNotNull( pCheck );
					assertFalse( Hibernate.isInitialized( pCheck.getCity() ) );
					try {
						Hibernate.initialize( pCheck.getCity() );
						fail( "Should have thrown ObjectNotFoundException" );
					}
					catch (ObjectNotFoundException expected) {
						session.getTransaction().setRollbackOnly();
					}
				}
		);
	}

	@Test
	public void testOneToOneSelectIgnore(SessionFactoryScope scope) {
		setupTest( PersonOneToOneSelectIgnore.class, 1L, false, scope );
		scope.inTransaction(
				session -> {
					Person pCheck = session.find( PersonOneToOneSelectIgnore.class, 1L );
					assertNotNull( pCheck );
					assertNull( pCheck.getCity() );
				}
		);
	}

	@Test
	public void testManyToOneSelectException(SessionFactoryScope scope) {
		setupTest( PersonManyToOneSelectException.class, 1L, false, scope );
		scope.inTransaction(
				session -> {
					Person pCheck = session.find( PersonManyToOneSelectException.class, 1L );
					assertNotNull( pCheck );
					assertFalse( Hibernate.isInitialized( pCheck.getCity() ) );
					try {
						Hibernate.initialize( pCheck.getCity() );
						fail( "Should have thrown ObjectNotFoundException" );
					}
					catch (ObjectNotFoundException expected) {
						session.getTransaction().setRollbackOnly();
					}
				}
		);
	}

	@Test
	public void testManyToOneSelectIgnore(SessionFactoryScope scope) {
		setupTest( PersonManyToOneSelectIgnore.class, 1L, false, scope );
		scope.inTransaction(
				session -> {
					Person pCheck = session.find( PersonManyToOneSelectIgnore.class, 1L );
					assertNotNull( pCheck );
					assertNull( pCheck.getCity() );
				}
		);
	}

	@Test
	public void testPkjcOneToOneSelectException(SessionFactoryScope scope) {
		setupTest( PersonPkjcSelectException.class, 1L, false, scope );
		scope.inTransaction(
				session -> {
					Person pCheck = session.find( PersonPkjcSelectException.class, 1L );
					assertNotNull( pCheck );
					// eagerly loaded because @PKJC assumes ignoreNotFound
					assertTrue( Hibernate.isInitialized( pCheck.getCity() ) );
					assertNull( pCheck.getCity() );
					/*
					assertFalse( Hibernate.isInitialized( pCheck.getCity() ) );
					try {
						Hibernate.initialize( pCheck.getCity() );
						fail( "Should have thrown ObjectNotFoundException" );
					}
					catch (ObjectNotFoundException expected) {
						session.getTransaction().setRollbackOnly();
					}
					*/
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
					assertNotNull( pCheck );
					assertNull( pCheck.getCity() );
				}
		);
	}

	@Test
	public void testMapsIdOneToOneSelectException(SessionFactoryScope scope) {
		setupTest( PersonMapsIdSelectException.class, 1L, true, scope );
		scope.inTransaction(
				session -> {
					Person pCheck = session.find( PersonMapsIdSelectException.class, 1L );
					assertNotNull( pCheck );
					assertFalse( Hibernate.isInitialized( pCheck.getCity() ) );
					try {
						Hibernate.initialize( pCheck.getCity() );
						fail( "Should have thrown ObjectNotFoundException" );
					}
					catch (ObjectNotFoundException expected) {
						session.getTransaction().setRollbackOnly();
					}
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
					assertNotNull( pCheck );
					assertNull( pCheck.getCity() );
				}
		);
	}

	@Test
	public void testMapsIdJoinColumnOneToOneSelectException(SessionFactoryScope scope) {
		setupTest( PersonMapsIdColumnSelectException.class, 1L, true, scope );
		scope.inTransaction(
				session -> {
					Person pCheck = session.find( PersonMapsIdColumnSelectException.class, 1L );
					assertNotNull( pCheck );
					assertFalse( Hibernate.isInitialized( pCheck.getCity() ) );
					try {
						Hibernate.initialize( pCheck.getCity() );
						fail( "Should have thrown ObjectNotFoundException" );
					}
					catch (ObjectNotFoundException expected) {
						session.getTransaction().setRollbackOnly();
					}
				}
		);
	}

	@Test
	public void testMapsIdJoinColumnOneToOneSelectIgnore(SessionFactoryScope scope) {
		setupTest( PersonMapsIdColumnSelectIgnore.class, 1L, true, scope );
		scope.inTransaction(
				session -> {
					Person pCheck = session.find( PersonMapsIdColumnSelectIgnore.class, 1L );
					// Person should be non-null;association should be null.
					assertNotNull( pCheck );
					assertNull( pCheck.getCity() );
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
	@Table(name = "PersonOneToOneSelectException")
	public static class PersonOneToOneSelectException extends Person {
		@Id
		private Long id;

		@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
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
	@Table(name = "PersonOneToOneSelectIgnore")
	public static class PersonOneToOneSelectIgnore extends Person {
		@Id
		private Long id;

		@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
		@NotFound(action = NotFoundAction.IGNORE)
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
	@Table(name = "PersonManyToOneSelectException")
	public static class PersonManyToOneSelectException extends Person {
		@Id
		private Long id;

		@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
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
	@Table(name = "PersonManyToOneSelectIgnore")
	public static class PersonManyToOneSelectIgnore extends Person {
		@Id
		private Long id;

		@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
		@NotFound(action = NotFoundAction.IGNORE)
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

		@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
		@PrimaryKeyJoinColumn
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

		@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
		@PrimaryKeyJoinColumn
		@NotFound(action = NotFoundAction.IGNORE)
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
	@Table(name = "PersonMapsIdJoinException")
	public static class PersonMapsIdJoinException extends Person {
		@Id
		private Long id;

		@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
		@MapsId
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

		@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
		@MapsId
		@NotFound(action = NotFoundAction.IGNORE)
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
	@Table(name = "PersonMapsIdSelectException")
	public static class PersonMapsIdSelectException extends Person {
		@Id
		private Long id;

		@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
		@MapsId
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

		@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
		@MapsId
		@NotFound(action = NotFoundAction.IGNORE)
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
	@Table(name = "PersonMapsIdColumnJoinException")
	public static class PersonMapsIdColumnJoinException extends Person {
		@Id
		private Long id;

		@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
		@MapsId
		@JoinColumn(name = "fk", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
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

		@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
		@MapsId
		@NotFound(action = NotFoundAction.IGNORE)
		@JoinColumn(name = "fk", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
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
	@Table(name = "PersonMapsIdColumnSelectExcept")
	public static class PersonMapsIdColumnSelectException extends Person {
		@Id
		private Long id;

		@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
		@MapsId
		@JoinColumn(name = "fk", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
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

		@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
		@MapsId
		@NotFound(action = NotFoundAction.IGNORE)
		@JoinColumn(name = "fk", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
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
