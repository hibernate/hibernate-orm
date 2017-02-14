/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.onetoone.formula;

import org.junit.Test;

import org.hibernate.FetchMode;
import org.hibernate.Hibernate;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.criterion.Property;
import org.hibernate.dialect.Oracle8iDialect;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.transaction.TransactionUtil;

import org.hibernate.type.descriptor.java.internal.StringJavaDescriptor;
import org.hibernate.type.descriptor.sql.spi.ClobSqlDescriptor;
import org.hibernate.type.internal.BasicTypeImpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * @author Gavin King
 */
public class OneToOneFormulaTest extends BaseCoreFunctionalTestCase {
	private static BasicTypeImpl<String> textAsMaterializedClobType = new BasicTypeImpl( StringJavaDescriptor.INSTANCE,
																				 ClobSqlDescriptor.CLOB_BINDING );


	public String[] getMappings() {
		return new String[] { "onetoone/formula/Person.hbm.xml" };
	}

	public void configure(Configuration cfg) {
		if ( Oracle8iDialect.class.isInstance( getDialect() ) ) {
			cfg.registerTypeOverride( textAsMaterializedClobType );
		}
		cfg.setProperty( Environment.USE_SECOND_LEVEL_CACHE, "false" );
		cfg.setProperty( Environment.GENERATE_STATISTICS, "true" );
		cfg.setProperty( Environment.DEFAULT_BATCH_FETCH_SIZE, "2" );
	}

	@Test
	public void testOneToOneFormula() {
		final Person p = new Person();
		p.setName("Gavin King");
		Address a = new Address();
		a.setPerson(p);
		a.setType("HOME");
		a.setZip("3181");
		a.setState("VIC");
		a.setStreet("Karbarook Ave");
		p.setAddress(a);

		TransactionUtil.doInHibernate( this::sessionFactory, session->{
			session.persist(p);
		} );

		TransactionUtil.doInHibernate( this::sessionFactory, session->{
			Person person = (Person) session.createQuery("from Person").uniqueResult();

			assertNotNull( person.getAddress() );
			assertTrue( Hibernate.isInitialized( person.getAddress() ) );
			assertNull( person.getMailingAddress() );

			session.clear();

			person = (Person) session.createQuery( "from Person p left join fetch p.mailingAddress left join fetch p.address" )
					.uniqueResult();

			assertNotNull( person.getAddress() );
			assertTrue( Hibernate.isInitialized( person.getAddress() ) );
			assertNull( person.getMailingAddress() );

			session.clear();

			person = (Person) session.createQuery("from Person p left join fetch p.address").uniqueResult();

			assertNotNull( person.getAddress() );
			assertTrue( Hibernate.isInitialized( person.getAddress() ) );
			assertNull( person.getMailingAddress() );

			session.clear();

			person = (Person) session.createCriteria(Person.class)
					.createCriteria("address")
					.add( Property.forName("zip").eq("3181") )
					.uniqueResult();
			assertNotNull(person);

			session.clear();

			person = (Person) session.createCriteria(Person.class)
					.setFetchMode("address", FetchMode.JOIN)
					.uniqueResult();

			assertNotNull( person.getAddress() );
			assertTrue( Hibernate.isInitialized( person.getAddress() ) );
			assertNull( person.getMailingAddress() );

			session.clear();

			person = (Person) session.createCriteria(Person.class)
					.setFetchMode("mailingAddress", FetchMode.JOIN)
					.uniqueResult();

			assertNotNull( person.getAddress() );
			assertTrue( Hibernate.isInitialized( person.getAddress() ) );
			assertNull( person.getMailingAddress() );

			session.delete(person);
		} );
	}

	@Test
	public void testOneToOneEmbeddedCompositeKey() {
		final Person p = new Person();
		p.setName("Gavin King");

		TransactionUtil.doInHibernate( this::sessionFactory, session -> {
			Address a = new Address();
			a.setPerson(p);
			a.setType("HOME");
			a.setZip("3181");
			a.setState("VIC");
			a.setStreet("Karbarook Ave");
			p.setAddress(a);

			session.persist(p);
		}  );

		TransactionUtil.doInHibernate( this::sessionFactory, session -> {
			Address address = new Address();
			address.setType("HOME");
			address.setPerson(p);
			address = session.load(Address.class, address);
			assertFalse( Hibernate.isInitialized(address) );
			address.getPerson();
			address.getType();
			assertFalse( Hibernate.isInitialized(address) );
			assertEquals(address.getZip(), "3181");

			session.clear();

			address = new Address();
			address.setType("HOME");
			address.setPerson(p);
			Address a2 = session.get(Address.class, address);
			assertTrue( Hibernate.isInitialized(address) );
			assertSame(a2, address);
			assertSame(a2.getPerson(), p); //this is a little bit desirable
			assertEquals(address.getZip(), "3181");

			session.delete(a2);
			session.delete( session.get( Person.class, p.getName() ) ); //this is certainly undesirable! oh well...

		}  );
	}

}
