package org.hibernate.test.notfound;

import java.io.Serializable;
import javax.persistence.CascadeType;
import javax.persistence.ConstraintMode;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.MapsId;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

import org.hibernate.Hibernate;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Gail Badner
 */
@TestForIssue( jiraKey = "HHH-12436")
public class RequiredLazyNotFoundTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				PersonManyToOneSelectException.class,
				PersonOneToOneSelectException.class,
				PersonMapsIdSelectException.class,
				PersonPkjcSelectException.class,
				PersonMapsIdColumnSelectException.class,
				City.class
		};
	}

	@Override
	protected void configure(Configuration configuration) {
		super.configure( configuration );

		configuration.setProperty( AvailableSettings.SHOW_SQL, Boolean.TRUE.toString() );
		configuration.setProperty( AvailableSettings.FORMAT_SQL, Boolean.TRUE.toString() );
	}

	@Test
	public void testOneToOneSelectException() {
		setupTest( PersonOneToOneSelectException.class, 1L, false );
		doInHibernate(
				this::sessionFactory, session -> {
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
	public void testManyToOneSelectException() {
		setupTest( PersonManyToOneSelectException.class, 1L, false );
		doInHibernate(
				this::sessionFactory, session -> {
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
	public void testPkjcOneToOneSelectException() {
		setupTest( PersonPkjcSelectException.class, 1L, false );
		doInHibernate(
				this::sessionFactory, session -> {
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
	public void testMapsIdOneToOneSelectException() {
		setupTest( PersonMapsIdSelectException.class, 1L, true );
		doInHibernate(
				this::sessionFactory, session -> {
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
	public void testMapsIdJoinColumnOneToOneSelectException() {
		setupTest( PersonMapsIdColumnSelectException.class, 1L, true );
		doInHibernate(
				this::sessionFactory, session -> {
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

	private <T extends Person> void setupTest(Class<T> clazz, long id, boolean isMapsId ) {
		persistData( clazz, id, isMapsId );
		doInHibernate(
				this::sessionFactory, session -> {
					Person p = session.find( clazz, id );
					assertEquals( "New York", p.getCity().getName() );
				}
		);

		doInHibernate(
				this::sessionFactory, session -> {
					session.createNativeQuery( "delete from City where id = " + id )
							.executeUpdate();
				}
		);
	}

	private <T extends Person> void persistData(Class<T> clazz, long id, boolean isMapsId) {
		final Person person;
		try {
			person = clazz.newInstance();
		}
		catch (Exception ex) {
			throw new RuntimeException( ex );
		}

		doInHibernate(
				this::sessionFactory, session -> {
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
	@Table( name = "PersonOneToOneSelectException" )
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
	@Table( name = "PersonManyToOneSelectException" )
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
	@Table( name = "PersonPkjcSelectException" )
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
	@Table( name = "PersonMapsIdJoinException" )
	public static class PersonMapsIdJoinException extends Person {
		@Id
		private Long id;

		@OneToOne(optional = false, fetch = FetchType.LAZY)
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
	@Table( name = "PersonMapsIdSelectException" )
	public static class PersonMapsIdSelectException extends Person {
		@Id
		private Long id;

		@OneToOne(optional = false, fetch = FetchType.LAZY)
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
	@Table( name = "PersonMapsIdColumnJoinException" )
	public static class PersonMapsIdColumnJoinException extends Person {
		@Id
		private Long id;

		@OneToOne(optional = false, fetch = FetchType.LAZY)
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
	@Table( name = "PersonMapsIdColumnSelectExcept" )
	public static class PersonMapsIdColumnSelectException extends Person {
		@Id
		private Long id;

		@OneToOne(optional = false, fetch = FetchType.LAZY)
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
	@Table( name = "City" )
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