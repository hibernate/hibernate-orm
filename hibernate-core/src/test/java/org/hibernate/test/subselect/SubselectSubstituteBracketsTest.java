package org.hibernate.test.subselect;

import java.util.Properties;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.annotations.Subselect;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.H2Dialect;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;

/**
 * @author Oleksii Chumak
 */
@RequiresDialect(H2Dialect.class)
public class SubselectSubstituteBracketsTest extends BaseCoreFunctionalTestCase {

	private static final String SCHEMA_NAME = "SubselectSubstituteBracketsTest";

	@Before
	public void prepareTestData() {
		doInHibernate( this::sessionFactory, session -> {
			session.doWork( connection -> {
				connection.createStatement()
						.executeUpdate( "CREATE TABLE " + SCHEMA_NAME + ".FOO (ID int not null, NAME varchar(255), primary key (ID))" );
				connection.createStatement()
						.executeUpdate( "INSERT INTO " + SCHEMA_NAME + ".FOO (ID,NAME) VALUES(1,'name1')" );
				connection.createStatement()
						.executeUpdate( "INSERT INTO " + SCHEMA_NAME + ".FOO (ID,NAME) VALUES(2,'name2')" );
				connection.createStatement()
						.executeUpdate( "INSERT INTO " + SCHEMA_NAME + ".FOO (ID,NAME) VALUES(3,'name3')" );
			} );
		} );
	}

	@After
	public void deleteTestData() {
		doInHibernate( this::sessionFactory, session -> {
			session.doWork( connection -> {
				connection.createStatement().executeUpdate( "DROP TABLE " + SCHEMA_NAME + ".FOO" );
			} );
		} );
	}

	@Test
	public void testAnnotationConfiguredSubselect() {
		doInHibernate( this::sessionFactory, session -> {
			assertEquals( "name1", session.get( ViewFoo.class, 1 ).name );
			assertEquals( "name2", session.get( ViewFoo.class, 2 ).name );
			assertEquals( "name3", session.get( ViewFoo.class, 3 ).name );

			long count = (long) session.createQuery( "SELECT COUNT(*) FROM SubselectSubstituteBracketsTest$ViewFoo" )
					.uniqueResult();
			assertEquals( 3L, count );
		} );
	}

	@Test
	public void testAnnotationConfiguredSubselectCatalog() {
		doInHibernate( this::sessionFactory, session -> {
			assertEquals( "name1", session.get( CatalogViewFoo.class, 1 ).name );
			assertEquals( "name2", session.get( CatalogViewFoo.class, 2 ).name );
			assertEquals( "name3", session.get( CatalogViewFoo.class, 3 ).name );

			long count = (long) session.createQuery(
					"SELECT COUNT(*) FROM SubselectSubstituteBracketsTest$CatalogViewFoo" )
					.uniqueResult();
			assertEquals( 3L, count );
		} );
	}

	@Test
	public void testXmlConfiguredSubselect() {
		doInHibernate( this::sessionFactory, session -> {
			assertEquals( "name1", session.get( XmlViewFoo.class, 1 ).name );
			assertEquals( "name2", session.get( XmlViewFoo.class, 2 ).name );
			assertEquals( "name3", session.get( XmlViewFoo.class, 3 ).name );

			long count = (long) session.createQuery( "SELECT COUNT(*) FROM SubselectSubstituteBracketsTest$XmlViewFoo" )
					.uniqueResult();
			assertEquals( 3L, count );
		} );
	}

	@Test
	public void testXmlConfiguredSubselectCatalog() {
		doInHibernate( this::sessionFactory, session -> {
			assertEquals( "name1", session.get( CatalogXmlViewFoo.class, 1 ).name );
			assertEquals( "name2", session.get( CatalogXmlViewFoo.class, 2 ).name );
			assertEquals( "name3", session.get( CatalogXmlViewFoo.class, 3 ).name );

			long count = (long) session.createQuery(
					"SELECT COUNT(*) FROM SubselectSubstituteBracketsTest$CatalogXmlViewFoo" )
					.uniqueResult();
			assertEquals( 3L, count );
		} );
	}

	@Override
	public String[] getMappings() {
		return new String[] { "subselect/SubselectSubstituteBracketsTest.hbm.xml" };
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				ViewFoo.class,
				CatalogViewFoo.class
		};
	}


	@Override
	protected void configure(Configuration configuration) {
		final Properties properties = new Properties();
		properties.put( AvailableSettings.DEFAULT_SCHEMA, SCHEMA_NAME );
		properties.put( AvailableSettings.DEFAULT_CATALOG, SCHEMA_NAME );
		configuration.addProperties( properties );
	}

	@Override
	protected String createSecondSchema() {
		return SCHEMA_NAME;
	}

	@Entity
	@Subselect("SELECT ID,NAME FROM {h-schema}FOO")
	public static class ViewFoo {

		@Id
		@Column(name = "ID")
		public Integer id;

		@Column(name = "NAME")
		public String name;
	}

	@Entity
	@Subselect("SELECT ID,NAME FROM {h-catalog}FOO")
	public static class CatalogViewFoo {

		@Id
		@Column(name = "ID")
		public Integer id;

		@Column(name = "NAME")
		public String name;
	}

	public static class XmlViewFoo {
		public Integer id;
		public String name;
	}

	public static class CatalogXmlViewFoo {
		public Integer id;
		public String name;
	}
}
