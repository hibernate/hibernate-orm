package org.hibernate.test.cte;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;

import org.hibernate.cfg.Environment;
import org.hibernate.hql.spi.id.cte.CTEMultiTableBulkIdStrategy;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Vlad Mihalcea
 */
public class CTETest extends BaseCoreFunctionalTestCase {

	@Override
	protected Configuration constructConfiguration() {
		Configuration configuration = super.constructConfiguration();
		configuration.setProperty( AvailableSettings.HQL_BULK_ID_STRATEGY, CTEMultiTableBulkIdStrategy.class.getName() );
		configuration.setProperty( AvailableSettings.SHOW_SQL, "true" );
		configuration.setProperty( AvailableSettings.DIALECT, "org.hibernate.dialect.PostgreSQL9Dialect" );
		configuration.setProperty( AvailableSettings.DRIVER, "org.postgresql.Driver" );
		configuration.setProperty( AvailableSettings.URL, "jdbc:postgresql:hibernate_orm_test" );
		configuration.setProperty( AvailableSettings.USER, "hibernate_orm_test" );
		configuration.setProperty( AvailableSettings.PASS, "hibernate_orm_test" );
		configuration.setProperty( AvailableSettings.POOL_SIZE, "5" );
		configuration.setProperty( AvailableSettings.FORMAT_SQL, "true" );
		configuration.setProperty( AvailableSettings.MAX_FETCH_DEPTH, "5" );
		configuration.setProperty( AvailableSettings.CACHE_REGION_PREFIX, "hibernate.test" );
		configuration.setProperty( AvailableSettings.CACHE_REGION_FACTORY, "org.hibernate.testing.cache.CachingRegionFactory" );
		configuration.setProperty( "hibernate.service.allow_crawling", "false" );
		configuration.setProperty( AvailableSettings.LOG_SESSION_METRICS, "true" );
		return configuration;
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Person.class,
				Doctor.class,
				Engineer.class
		};
	}

//	@Override
//	protected Configuration constructConfiguration() {
//		Configuration configuration = super.constructConfiguration();
//		configuration.setProperty( AvailableSettings.HQL_BULK_ID_STRATEGY, CTEMultiTableBulkIdStrategy.class.getName() );
//		configuration.setProperty( AvailableSettings.SHOW_SQL, "true" );
//		return configuration;
//	}

	@Test
	public void test() {
		Session session = openSession();
		Transaction transaction = session.beginTransaction();
		Doctor doctor = new Doctor();
		session.save( doctor );
		Engineer engineer = new Engineer();
		session.save( engineer );
		session.flush();
		session.createQuery( "delete from Person" ).executeUpdate();
		transaction.commit();
		session.close();
	}

	@Entity(name = "Person")
	@Inheritance(strategy = InheritanceType.JOINED)
	public static class Person {

		@Id
		@GeneratedValue
		private Long id;
	}

	@Entity(name = "Doctor")
	public static class Doctor extends Person {

	}

	@Entity(name = "Engineer")
	public static class Engineer extends Person {

	}
}