package org.hibernate.test.notfound;

import java.io.Serializable;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Gail Badner
 */
@TestForIssue( jiraKey = "HHH-12436")
public class OptionalEagerMappedByNotFoundTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				PersonOneToOneJoinException.class,
				PersonOneToOneJoinIgnore.class,
				PersonOneToOneSelectException.class,
				PersonOneToOneSelectIgnore.class,
				Employment.class
		};
	}

	@Override
	protected void configure(Configuration configuration) {
		super.configure( configuration );

		configuration.setProperty( AvailableSettings.SHOW_SQL, Boolean.TRUE.toString() );
		configuration.setProperty( AvailableSettings.FORMAT_SQL, Boolean.TRUE.toString() );
	}

	@After
	public void deleteData() {
		doInHibernate(
				this::sessionFactory, session -> {
					session.createQuery( "delete from Person" ).executeUpdate();
					session.createQuery( "delete from Employment" ).executeUpdate();
				}
		);
	}

	@Test
	public void  testOneToOneJoinException() {
		setupTest( PersonOneToOneJoinException.class, 1L, false );
		doInHibernate(
				this::sessionFactory, session -> {
					Person pCheck = session.find( PersonOneToOneJoinException.class, 1L );
					checkResult( pCheck );
				}
		);
	}

	@Test
	public void testOneToOneJoinIgnore() {
		setupTest( PersonOneToOneJoinIgnore.class, 1L, false );
		doInHibernate(
				this::sessionFactory, session -> {
					Person pCheck = session.find( PersonOneToOneJoinIgnore.class, 1L );
					checkResult( pCheck );
				}
		);
	}

	@Test
	public void testOneToOneSelectException() {
		setupTest( PersonOneToOneSelectException.class, 1L, false );
		doInHibernate(
				this::sessionFactory, session -> {
					Person pCheck = session.find( PersonOneToOneSelectException.class, 1L );
					checkResult( pCheck );
				}
		);
	}

	@Test
	public void testOneToOneSelectIgnore() {
		setupTest( PersonOneToOneSelectIgnore.class, 1L, false );
		doInHibernate(
				this::sessionFactory, session -> {
					Person pCheck = session.find( PersonOneToOneSelectIgnore.class, 1L );
					checkResult( pCheck );
				}
		);
	}

	private <T extends Person> void setupTest(Class<T> clazz, long id, boolean isMapsId ) {
		persistData( clazz, id, isMapsId );
		doInHibernate(
				this::sessionFactory, session -> {
					Person p = session.find( clazz, id );
					assertEquals( "New York", p.getEmployment().getName() );
				}
		);

		doInHibernate(
				this::sessionFactory, session -> {
					session.createNativeQuery( "delete from Employment where id = " + id )
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
	@Table( name = "PersonOneToOneJoinException" )
	public static class PersonOneToOneJoinException extends Person {
		@OneToOne(mappedBy = "person", cascade = CascadeType.PERSIST)
		@NotFound(action = NotFoundAction.EXCEPTION)
		@Fetch( FetchMode.JOIN )
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
	@Table( name = "PersonOneToOneJoinIgnore" )
	public static class PersonOneToOneJoinIgnore extends Person {
		@Id
		private Long id;

		@OneToOne(mappedBy = "person", cascade = CascadeType.PERSIST)
		@NotFound( action = NotFoundAction.IGNORE )
		@Fetch( FetchMode.JOIN )
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
	@Table( name = "PersonOneToOneSelectException" )
	public static class PersonOneToOneSelectException extends Person {
		@Id
		private Long id;

		@OneToOne(mappedBy = "person", cascade = CascadeType.PERSIST)
		@NotFound(action = NotFoundAction.EXCEPTION)
		@Fetch( FetchMode.SELECT )
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
	@Table( name = "PersonOneToOneSelectIgnore" )
	public static class PersonOneToOneSelectIgnore extends Person {
		@Id
		private Long id;

		@OneToOne(mappedBy = "person", cascade = CascadeType.PERSIST)
		@NotFound( action = NotFoundAction.IGNORE )
		@Fetch( FetchMode.SELECT )
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