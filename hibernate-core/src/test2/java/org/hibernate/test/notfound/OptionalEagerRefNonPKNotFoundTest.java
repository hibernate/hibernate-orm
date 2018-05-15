package org.hibernate.test.notfound;

import java.io.Serializable;
import javax.persistence.CascadeType;
import javax.persistence.ConstraintMode;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.MapsId;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Gail Badner
 */
@TestForIssue( jiraKey = "HHH-12436")
public class OptionalEagerRefNonPKNotFoundTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				PersonManyToOneJoinIgnore.class,
				PersonManyToOneSelectIgnore.class,
				PersonOneToOneJoinIgnore.class,
				PersonOneToOneSelectIgnore.class,
				PersonMapsIdJoinIgnore.class,
				PersonMapsIdSelectIgnore.class,
				PersonPkjcJoinException.class,
				PersonPkjcJoinIgnore.class,
				PersonPkjcSelectException.class,
				PersonPkjcSelectIgnore.class,
				PersonMapsIdColumnJoinIgnore.class,
				PersonMapsIdColumnSelectIgnore.class,
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
	public void testOneToOneJoinIgnore() {
		setupTest( PersonOneToOneJoinIgnore.class, 1L, false );
		doInHibernate(
				this::sessionFactory, session -> {
					Person pCheck = session.find( PersonOneToOneJoinIgnore.class, 1L );
					checkIgnore( pCheck );
				}
		);
	}

	@Test
	public void testOneToOneSelectIgnore() {
		setupTest( PersonOneToOneSelectIgnore.class, 1L, false );
		doInHibernate(
				this::sessionFactory, session -> {
					Person pCheck = session.find( PersonOneToOneSelectIgnore.class, 1L );
					checkIgnore( pCheck );
				}
		);
	}

	@Test
	public void testManyToOneJoinIgnore() {
		setupTest( PersonManyToOneJoinIgnore.class, 1L, false );
		doInHibernate(
				this::sessionFactory, session -> {
					Person pCheck = session.find( PersonManyToOneJoinIgnore.class, 1L );
					checkIgnore( pCheck );
				}
		);
	}

	@Test
	public void testManyToOneSelectIgnore() {
		setupTest( PersonManyToOneSelectIgnore.class, 1L, false );
		doInHibernate(
				this::sessionFactory, session -> {
					Person pCheck = session.find( PersonManyToOneSelectIgnore.class, 1L );
					checkIgnore( pCheck );
				}
		);
	}

	@Test
	public void testPkjcOneToOneJoinException() {
		setupTest( PersonPkjcJoinException.class, 1L, false );
		doInHibernate(
				this::sessionFactory, session -> {
					Person pCheck = session.find( PersonPkjcJoinException.class, 1L );
					// @OneToOne @PrimaryKeyJoinColumn always assumes @NotFound(IGNORE)
					checkIgnore( pCheck );
				}
		);
	}

	@Test
	public void testPkjcOneToOneJoinIgnore() {
		setupTest( PersonPkjcJoinIgnore.class, 1L, false );
		doInHibernate(
				this::sessionFactory, session -> {
					Person pCheck = session.find( PersonPkjcJoinIgnore.class, 1L );
					// Person is non-null and association is null.
					checkIgnore( pCheck );
				}
		);
	}

	@Test
	public void testPkjcOneToOneSelectException() {
		setupTest( PersonPkjcSelectException.class, 1L, false );
		doInHibernate(
				this::sessionFactory, session -> {
					Person pCheck = session.find( PersonPkjcSelectException.class, 1L );
					// @OneToOne @PrimaryKeyJoinColumn always assumes @NotFound(IGNORE)
					checkIgnore( pCheck );
				}
		);
	}

	@Test
	public void testPkjcOneToOneSelectIgnore() {
		setupTest( PersonPkjcSelectIgnore.class, 1L, false );
		doInHibernate(
				this::sessionFactory, session -> {
					Person pCheck = session.find( PersonPkjcSelectIgnore.class, 1L );
					// Person is non-null and association is null.
					checkIgnore( pCheck );
				}
		);
	}

	@Test
	public void testMapsIdOneToOneJoinIgnore() {
		setupTest( PersonMapsIdJoinIgnore.class, 1L, true );
		doInHibernate(
				this::sessionFactory, session -> {
					Person pCheck = session.find( PersonMapsIdJoinIgnore.class, 1L );
					// Person is non-null association is null.
					checkIgnore( pCheck );
				}
		);
	}

	@Test
	public void testMapsIdOneToOneSelectIgnore() {
		setupTest( PersonMapsIdSelectIgnore.class, 1L, true );
		doInHibernate(
				this::sessionFactory, session -> {
					Person pCheck = session.find( PersonMapsIdSelectIgnore.class, 1L );
					// Person is non-null association is null.
					checkIgnore( pCheck );
				}
		);
	}

	@Test
	public void testMapsIdJoinColumnOneToOneJoinIgnore() {
		setupTest( PersonMapsIdColumnJoinIgnore.class, 1L, true );
		doInHibernate(
				this::sessionFactory, session -> {
					Person pCheck = session.find( PersonMapsIdColumnJoinIgnore.class, 1L );
					checkIgnore( pCheck );
				}
		);
	}

	@Test
	public void testMapsIdJoinColumnOneToOneSelectIgnore() {
		setupTest( PersonMapsIdColumnSelectIgnore.class, 1L, true );
		doInHibernate(
				this::sessionFactory, session -> {
					Person pCheck = session.find( PersonMapsIdColumnSelectIgnore.class, 1L );
					checkIgnore( pCheck );
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
	@Table( name = "PersonOneToOneJoinException" )
	public static class PersonOneToOneJoinException extends Person {
		@Id
		private Long id;

		@OneToOne(cascade = CascadeType.PERSIST)
		@JoinColumn(
				name = "cityName",
				referencedColumnName = "name",
				foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
		)
		@Fetch( FetchMode.JOIN )
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
	@Table( name = "PersonOneToOneJoinIgnore" )
	public static class PersonOneToOneJoinIgnore extends Person {
		@Id
		private Long id;

		@OneToOne(cascade = CascadeType.PERSIST)
		@NotFound( action = NotFoundAction.IGNORE )
		@JoinColumn(
				name = "cityName",
				referencedColumnName = "name",
				foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
		)
		@Fetch( FetchMode.JOIN )
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
	@Table( name = "PersonOneToOneSelectException" )
	public static class PersonOneToOneSelectException extends Person {
		@Id
		private Long id;

		@OneToOne(cascade = CascadeType.PERSIST)
		@JoinColumn(
				name = "cityName",
				referencedColumnName = "name",
				foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
		)
		@Fetch( FetchMode.SELECT )
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
	@Table( name = "PersonOneToOneSelectIgnore" )
	public static class PersonOneToOneSelectIgnore extends Person {
		@Id
		private Long id;

		@OneToOne(cascade = CascadeType.PERSIST)
		@NotFound( action = NotFoundAction.IGNORE )
		@JoinColumn(
				name = "cityName",
				referencedColumnName = "name",
				foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
		)
		@Fetch( FetchMode.SELECT )
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
	@Table( name = "PersonManyToOneJoinException" )
	public static class PersonManyToOneJoinException extends Person {
		@Id
		private Long id;

		@ManyToOne(cascade = CascadeType.PERSIST)
		@JoinColumn(
				name = "cityName",
				referencedColumnName = "name",
				foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
		)
		@Fetch( FetchMode.JOIN )
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
	@Table( name = "PersonManyToOneJoinIgnore" )
	public static class PersonManyToOneJoinIgnore extends Person {
		@Id
		private Long id;

		@ManyToOne(cascade = CascadeType.PERSIST)
		@NotFound( action = NotFoundAction.IGNORE )
		@JoinColumn(
				name = "cityName",
				referencedColumnName = "name",
				foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
		)
		@Fetch( FetchMode.JOIN )
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

		@ManyToOne(cascade = CascadeType.PERSIST)
		@JoinColumn(
				name = "cityName",
				referencedColumnName = "name",
				foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
		)
		@Fetch( FetchMode.SELECT )
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
	@Table( name = "PersonManyToOneSelectIgnore" )
	public static class PersonManyToOneSelectIgnore extends Person {
		@Id
		private Long id;

		@ManyToOne(cascade = CascadeType.PERSIST)
		@NotFound( action = NotFoundAction.IGNORE )
		@JoinColumn(
				name = "cityName",
				referencedColumnName = "name",
				foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
		)
		@Fetch( FetchMode.SELECT )
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
	@Table( name = "PersonPkjcJoinException" )
	public static class PersonPkjcJoinException extends Person {
		@Id
		private Long id;

		@OneToOne(cascade = CascadeType.PERSIST)
		@PrimaryKeyJoinColumn
		@Fetch(FetchMode.JOIN )
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
	@Table( name = "PersonPkjcJoinIgnore" )
	public static class PersonPkjcJoinIgnore extends Person {
		@Id
		private Long id;

		@OneToOne(cascade = CascadeType.PERSIST)
		@PrimaryKeyJoinColumn
		@NotFound(action = NotFoundAction.IGNORE)
		@Fetch(FetchMode.JOIN )
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
	@Table( name = "PersonPkjcSelectException" )
	public static class PersonPkjcSelectException extends Person {
		@Id
		private Long id;

		@OneToOne(cascade = CascadeType.PERSIST)
		@PrimaryKeyJoinColumn
		@Fetch(FetchMode.SELECT )
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
	@Table( name = "PersonPkjcSelectIgnore" )
	public static class PersonPkjcSelectIgnore extends Person {
		@Id
		private Long id;

		@OneToOne(cascade = CascadeType.PERSIST)
		@PrimaryKeyJoinColumn
		@NotFound(action = NotFoundAction.IGNORE)
		@Fetch(FetchMode.SELECT )
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
	@Table( name = "PersonMapsIdJoinException" )
	public static class PersonMapsIdJoinException extends Person {
		@Id
		private Long id;

		@OneToOne
		@MapsId
		@Fetch(FetchMode.JOIN )
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
	@Table( name = "PersonMapsIdJoinIgnore" )
	public static class PersonMapsIdJoinIgnore extends Person {
		@Id
		private Long id;

		@OneToOne
		@MapsId
		@NotFound(action = NotFoundAction.IGNORE)
		@Fetch(FetchMode.JOIN )
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
	@Table( name = "PersonMapsIdSelectException" )
	public static class PersonMapsIdSelectException extends Person {
		@Id
		private Long id;

		@OneToOne
		@MapsId
		@Fetch(FetchMode.SELECT )
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
	@Table( name = "PersonMapsIdSelectIgnore" )
	public static class PersonMapsIdSelectIgnore extends Person {
		@Id
		private Long id;

		@OneToOne
		@MapsId
		@NotFound(action = NotFoundAction.IGNORE)
		@Fetch(FetchMode.SELECT )
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
	@Table( name = "PersonMapsIdColumnJoinException" )
	public static class PersonMapsIdColumnJoinException extends Person {
		@Id
		private Long id;

		@OneToOne
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
	@Table( name = "PersonMapsIdColumnJoinIgnore" )
	public static class PersonMapsIdColumnJoinIgnore extends Person {
		@Id
		private Long id;

		@OneToOne
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
	@Table( name = "PersonMapsIdColumnSelectException" )
	public static class PersonMapsIdColumnSelectException extends Person {
		@Id
		private Long id;

		@OneToOne
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
	@Table( name = "PersonMapsIdColumnSelectIgnore" )
	public static class PersonMapsIdColumnSelectIgnore extends Person {
		@Id
		private Long id;

		@OneToOne
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