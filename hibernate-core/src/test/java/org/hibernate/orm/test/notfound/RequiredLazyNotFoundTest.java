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
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Gail Badner
 */
@JiraKey(value = "HHH-12436")
@DomainModel(
		annotatedClasses = {
				RequiredLazyNotFoundTest.PersonManyToOneSelectException.class,
				RequiredLazyNotFoundTest.PersonOneToOneSelectException.class,
				RequiredLazyNotFoundTest.PersonMapsIdSelectException.class,
				RequiredLazyNotFoundTest.PersonPkjcSelectException.class,
				RequiredLazyNotFoundTest.PersonMapsIdColumnSelectException.class,
				RequiredLazyNotFoundTest.City.class
		}
)
@SessionFactory
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.SHOW_SQL, value = "true"),
				@Setting(name = AvailableSettings.FORMAT_SQL, value = "true")
		}
)
public class RequiredLazyNotFoundTest {

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
	public void testPkjcOneToOneSelectException(SessionFactoryScope scope) {
		setupTest( PersonPkjcSelectException.class, 1L, false, scope );
		scope.inTransaction(
				session -> {
					Person pCheck = session.find( PersonPkjcSelectException.class, 1L );
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

		@OneToOne(optional = false, fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
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

		@ManyToOne(optional = false, fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
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

		@OneToOne(optional = false, fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
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
	@Table(name = "PersonMapsIdJoinException")
	public static class PersonMapsIdJoinException extends Person {
		@Id
		private Long id;

		@OneToOne(optional = false, fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
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
	@Table(name = "PersonMapsIdSelectException")
	public static class PersonMapsIdSelectException extends Person {
		@Id
		private Long id;

		@OneToOne(optional = false, fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
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
	@Table(name = "PersonMapsIdColumnJoinException")
	public static class PersonMapsIdColumnJoinException extends Person {
		@Id
		private Long id;

		@OneToOne(optional = false, fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
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
	@Table(name = "PersonMapsIdColumnSelectExcept")
	public static class PersonMapsIdColumnSelectException extends Person {
		@Id
		private Long id;

		@OneToOne(optional = false, fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
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
