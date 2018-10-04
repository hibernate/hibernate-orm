package org.hibernate.test.subselect;

import java.util.Properties;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Subselect;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;

/**
 * @author Oleksii Chumak
 */
public class SubselectSubstituteBracketsNonDefaultSchemaTest extends BaseCoreFunctionalTestCase {

	private static final String SCHEMA_NAME = SubselectSubstituteBracketsNonDefaultSchemaTest.class.getSimpleName();

	@Before
	public void prepareTestData() {
		doInHibernate( this::sessionFactory, session -> {
			session.persist( new Foo( 1, "name1" ) );
			session.persist( new Foo( 2, "name2" ) );
			session.persist( new Foo( 3, "name3" ) );
		} );
	}

	@After
	public void deleteTestData() {
		doInHibernate( this::sessionFactory, session -> {
			session.createQuery( "DELETE SubselectSubstituteBracketsTest$Foo" ).executeUpdate();
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

	@Override
	public String[] getMappings() {
		return new String[] { "subselect/SubselectSubstituteBracketsNonDefaultSchemaTest.hbm.xml" };
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Foo.class,
				ViewFoo.class
		};
	}


	@Override
	protected void configure(Configuration configuration) {
		final Properties properties = new Properties();
		properties.put( AvailableSettings.DEFAULT_SCHEMA, SCHEMA_NAME );
		configuration.addProperties( properties );
	}

	@Override
	protected String createSecondSchema() {
		return "abc";
	}

	@Entity
	@Table(schema = "abc", name = "FOO")
	public static class Foo {

		@Id
		@Column(name = "ID")
		public Integer id;

		@Column(name = "NAME")
		public String name;

		public Foo() {
		}

		public Foo(Integer id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	@Entity
	@Table(schema = "abc")
	@Subselect("SELECT ID,NAME FROM {h-schema}FOO")
	public static class ViewFoo {

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
}
