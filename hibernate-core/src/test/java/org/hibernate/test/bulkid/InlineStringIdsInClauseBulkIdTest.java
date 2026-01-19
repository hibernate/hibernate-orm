package org.hibernate.test.bulkid;

import java.sql.Statement;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.hql.spi.id.inline.InlineIdsInClauseBulkIdStrategy;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;

@RequiresDialectFeature(DialectChecks.SupportRowValueConstructorSyntaxInInList.class)
public class InlineStringIdsInClauseBulkIdTest extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Person.class, Doctor.class, Engineer.class
		};
	}

	@Override
	protected void configure(Configuration configuration) {
		super.configure( configuration );
		configuration.setProperty(
				AvailableSettings.HQL_BULK_ID_STRATEGY,
				InlineIdsInClauseBulkIdStrategy.class.getName()
		);
	}

	@Override
	protected boolean isCleanupTestDataRequired() {
		return true;
	}

	@Override
	protected boolean isCleanupTestDataUsingBulkDelete() {
		return true;
	}

	@Before
	public void setUp() {
		doInHibernate(
				this::sessionFactory, session -> {
					session.doWork( connection -> {
						final Statement statement = connection.createStatement();
						statement.execute(
								"insert into Person (employed, name, id) values (0, null, 'd0''')"
						);
						statement.execute(
								"insert into Doctor (specialization, id) values ('spec_0', 'd0''')"
						);
						statement.close();
						for ( int i = 1; i < 10; i++ ) {
							Doctor doctor = new Doctor();
							doctor.setId( "d" + i );
							doctor.setEmployed( i % 2 );
							doctor.setSpecialization( "spec_" + i );
							session.persist( doctor );
						}
					} );
				}
		);
		doInHibernate(
				this::sessionFactory, session -> {
					for ( int i = 0; i < 10; i++ ) {
						Engineer engineer = new Engineer();
						engineer.setId( "i" + i );
						engineer.setEmployed( i % 2 );
						engineer.setFellow( i % 2 );
						session.persist( engineer );
					}
				}
		);

		doInHibernate(
				this::sessionFactory, session -> {
					final Long count = session.createQuery(
									"select count(p) from Person p where employed = 1",
									Long.class
							)
							.getSingleResult();
					assertEquals( 10L, count.longValue() );
				}
		);
	}

	@Test
	public void testUpdate() {
		doInHibernate(
				this::sessionFactory, session -> {
					session.createQuery(
									"update Person set fellow = :fellow, name = :name where employed = :employed"
							).setParameter( "fellow", 0 )
							.setParameter( "name", "John Doe" )
							.setParameter( "employed", 0 )
							.executeUpdate();
					// 5 employed engineers updated to fellow = 0
					long fellows = session.createQuery(
							"select count(e) from Engineer e where fellow = 0",
							Long.class
					).getSingleResult();
					assertEquals( 5L, fellows );
					// 10 total employed persons updated to "John Doe"
					long johnDoes = session.createQuery(
							"select count(e) from Person e where name = 'John Doe'",
							Long.class
					).getSingleResult();
					assertEquals( 10L, johnDoes );
				}
		);
	}

	@Test
	public void testDeleteFromPerson() {
		doInHibernate(
				this::sessionFactory, session -> {
					session.createQuery( "delete from Person where employed = :employed" )
							.setParameter( "employed", 1 )
							.executeUpdate();
					long persons = session.createQuery( "select count (p) from Person p", Long.class ).getSingleResult();
					assertEquals( 10L, persons );
				}
		);
	}

	@Test
	public void testDeleteSingleDoctor() {
		doInHibernate(
				this::sessionFactory, session -> {
					session.createQuery( "delete from Doctor where specialization = 'spec_0'" ).executeUpdate();
					long doctors = session.createQuery( "select count (d) from Doctor d", Long.class ).getSingleResult();
					assertEquals( 9L, doctors );
				}
		);
	}

	@Entity(name = "Person")
	@Inheritance(strategy = InheritanceType.JOINED)
	public static class Person {
		@Id
		private String id;
		private String name;
		private int employed;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public int getEmployed() {
			return employed;
		}

		public void setEmployed(int employed) {
			this.employed = employed;
		}
	}

	@Entity(name = "Doctor")
	public static class Doctor extends Person {
		private String specialization;

		public String getSpecialization() {
			return specialization;
		}

		public void setSpecialization(String specialization) {
			this.specialization = specialization;
		}
	}

	@Entity(name = "Engineer")
	public static class Engineer extends Person {
		private int fellow;

		public int getFellow() {
			return fellow;
		}

		public void setFellow(int fellow) {
			this.fellow = fellow;
		}
	}
}