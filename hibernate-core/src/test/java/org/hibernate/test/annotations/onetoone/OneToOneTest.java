/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.onetoone;

import java.util.Iterator;

import org.hibernate.EmptyInterceptor;
import org.hibernate.MappingException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.hibernate.testing.transaction.TransactionUtil;
import org.hibernate.test.annotations.Customer;
import org.hibernate.test.annotations.Discount;
import org.hibernate.test.annotations.Passport;
import org.hibernate.test.annotations.Ticket;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Emmanuel Bernard
 */
public class OneToOneTest extends BaseNonConfigCoreFunctionalTestCase {
	@Test
	public void testEagerFetching() throws Exception {
		final String clientName = "Emmanuel";
		TransactionUtil.doInHibernate( this::sessionFactory, session -> {
			Client c = new Client();
			c.setName( clientName );
			Address a = new Address();
			a.setCity( "Courbevoie" );
			c.setAddress( a );
			session.persist( c );
		} );

		final Client client = TransactionUtil.doInHibernate( this::sessionFactory, session -> {
			Query q = session.createQuery( "select c from Client c where c.name = :name" );
			q.setString( "name", clientName );
			Client c = (Client) q.uniqueResult();
			//c = (Client) s.get(Client.class, c.getId());

			assertNotNull( c );
			return c;
		} );

		assertNotNull( client.getAddress() );
		//assertTrue( "Should be eager fetched", Hibernate.isInitialized( c.getAddress() ) );
	}

	@Test
	public void testDefaultOneToOne() throws Exception {
		//test a default one to one and a mappedBy in the other side
		Long customerId = TransactionUtil.doInHibernate( this::sessionFactory, session -> {
			Customer c = new Customer();
			c.setName( "Hibernatus" );
			Passport p = new Passport();
			p.setNumber( "123456789" );
			session.persist( c ); //we need the id to assigned it to passport
			c.setPassport( p );
			p.setOwner( c );
			p.setId( c.getId() );
			return c.getId();
		} );

		TransactionUtil.doInHibernate( this::sessionFactory, session -> {
			Customer c = session.get( Customer.class, customerId );
			assertNotNull( c );
			Passport p = c.getPassport();

			assertNotNull( p );
			assertEquals( "123456789", p.getNumber() );
			assertNotNull( p.getOwner() );
			assertEquals( "Hibernatus", p.getOwner().getName() );
		} );
	}

	@Test
	public void testOneToOneWithExplicitFk() throws Exception {
		final Client c = new Client();
		Address a = new Address();
		a.setCity( "Paris" );
		c.setName( "Emmanuel" );
		c.setAddress( a );

		TransactionUtil.doInHibernate( this::sessionFactory, session -> {
			session.persist( c );
		} );

		TransactionUtil.doInHibernate( this::sessionFactory, session -> {
			Client client = session.get( Client.class, c.getId() );

			assertNotNull( client );
			assertNotNull( client.getAddress() );
			assertEquals( "Paris", client.getAddress().getCity() );
		} );
	}

	@Test
	public void testOneToOneWithExplicitSecondaryTableFk() throws Exception {
		final Client c = new Client();
		Address a = new Address();
		a.setCity( "Paris" );
		c.setName( "Emmanuel" );
		c.setSecondaryAddress( a );

		TransactionUtil.doInHibernate( this::sessionFactory, session -> {
			session.persist( c );
		} );

		TransactionUtil.doInHibernate( this::sessionFactory, session -> {
			final Client client = session.get( Client.class, c.getId() );

			assertNotNull( client );
			assertNotNull( client.getSecondaryAddress() );
			assertEquals( "Paris", client.getSecondaryAddress().getCity() );
		} );
	}

	@Test
	public void testUnidirectionalTrueOneToOne() throws Exception {
		final Body b = new Body();
		final Heart h = new Heart();
		b.setHeart( h );
		b.setId( 1 );
		h.setId( b.getId() ); //same PK
		TransactionUtil.doInHibernate( this::sessionFactory, session -> {
			session.persist( h );
			session.persist( b );
		} );

		TransactionUtil.doInHibernate( this::sessionFactory, session -> {
			final Body body = session.get( Body.class, b.getId() );

			assertNotNull( body );
			assertNotNull( body.getHeart() );
			assertEquals( h.getId(), body.getHeart().getId() );
		} );
	}

	@Test
	public void testCompositePk() throws Exception {
		final ComputerPk cid = new ComputerPk();
		final SerialNumber sn = new SerialNumber();
		TransactionUtil.doInHibernate( this::sessionFactory, session -> {
			cid.setBrand( "IBM" );
			cid.setModel( "ThinkPad" );
			Computer c = new Computer();
			c.setId( cid );
			c.setCpu( "2 GHz" );
			SerialNumberPk sid = new SerialNumberPk();
			sid.setBrand( cid.getBrand() );
			sid.setModel( cid.getModel() );
			sn.setId( sid );
			sn.setValue( "REZREZ23424" );
			c.setSerial( sn );
			session.persist( c );
		} );

		TransactionUtil.doInHibernate( this::sessionFactory, session -> {
			Computer c = session.get( Computer.class, cid );
			assertNotNull( c );
			assertNotNull( c.getSerial() );
			assertEquals( sn.getValue(), c.getSerial().getValue() );
		} );
	}

	@Test
	public void testBidirectionalTrueOneToOne() throws Exception {
		try (Session s = openSession()) {
			Party party = new Party();
			PartyAffiliate affiliate = new PartyAffiliate();
			affiliate.partyId = "id";
			party.partyId = "id";
			party.partyAffiliate = affiliate;
			affiliate.party = party;

			s.getTransaction().begin();
			try {

				s.persist( party );
				s.getTransaction().commit();
			}
			catch (Exception e) {
				if ( s.getTransaction() != null && s.getTransaction().isActive() ) {
					s.getTransaction().rollback();
				}
				throw e;
			}

			s.clear();

			Transaction tx = s.beginTransaction();
			try {
				affiliate = s.get( PartyAffiliate.class, "id" );
				assertNotNull( affiliate.party );
				assertEquals( affiliate.partyId, affiliate.party.partyId );

				s.clear();

				party = s.get( Party.class, "id" );
				assertNotNull( party.partyAffiliate );
				assertEquals( party.partyId, party.partyAffiliate.partyId );

				s.delete( party );
				s.delete( party.partyAffiliate );
				tx.commit();
			}
			catch (Exception e) {
				if ( s.getTransaction() != null && s.getTransaction().isActive() ) {
					s.getTransaction().rollback();
				}
				throw e;
			}
		}
	}

	@Test
	public void testBidirectionalFkOneToOne() throws Exception {
		try (Session s = openSession()) {
			s.getTransaction().begin();
			Trousers trousers = new Trousers();
			TrousersZip zip = new TrousersZip();
			try {
				trousers.id = 1;
				zip.id = 2;
				trousers.zip = zip;
				zip.trousers = trousers;
				s.persist( trousers );
				s.persist( zip );
				s.getTransaction().commit();
			}
			catch (Exception e) {
				if ( s.getTransaction() != null && s.getTransaction().isActive() ) {
					s.getTransaction().rollback();
				}
				throw e;
			}

			s.clear();

			Transaction tx = s.beginTransaction();
			try {
				trousers = s.get( Trousers.class, trousers.id );
				assertNotNull( trousers.zip );
				assertEquals( zip.id, trousers.zip.id );

				s.clear();

				zip = s.get( TrousersZip.class, zip.id );
				assertNotNull( zip.trousers );
				assertEquals( trousers.id, zip.trousers.id );

				s.delete( zip );
				s.delete( zip.trousers );
				tx.commit();
			}
			catch (Exception e) {
				if ( s.getTransaction() != null && s.getTransaction().isActive() ) {
					s.getTransaction().rollback();
				}
				throw e;
			}
		}
	}

	@Test
	public void testForeignGenerator() {
		TransactionUtil.doInHibernate( this::sessionFactory, session -> {
			Owner owner = new Owner();
			OwnerAddress address = new OwnerAddress();
			owner.setAddress( address );
			address.setOwner( owner );
			session.persist( owner );
			session.flush();
			session.clear();
			owner = session.get( Owner.class, owner.getId() );
			assertNotNull( owner );
			assertNotNull( owner.getAddress() );
			assertEquals( owner.getId(), owner.getAddress().getId() );
		} );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-4606" )
	public void testJoinColumnConfiguredInXml() {
		PersistentClass pc = metadata().getEntityBinding( Son.class.getName() );
		Iterator iter = pc.getJoinIterator();
		Table table = ( ( Join ) iter.next() ).getTable();
		Iterator columnIter = table.getColumnIterator();
		boolean fooFound = false;
		boolean barFound = false;
		while ( columnIter.hasNext() ) {
			Column column = ( Column ) columnIter.next();
			if ( column.getName().equals( "foo" ) ) {
				fooFound = true;
			}
			if ( column.getName().equals( "bar" ) ) {
				barFound = true;
			}
		}
		assertTrue(
				"The mapping defines join columns which could not be found in the metadata.", fooFound && barFound
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-6723")
	public void testPkOneToOneSelectStatementDoesNotGenerateExtraJoin() {
		// This test uses an interceptor to verify that correct number of joins are generated.
		TransactionUtil.doInHibernate( this::sessionFactory, s -> {

			Owner owner = new Owner();
			OwnerAddress address = new OwnerAddress();
			owner.setAddress( address );
			address.setOwner( owner );
			s.persist( owner );
			s.flush();
			s.clear();

			owner = s.get( Owner.class, owner.getId() );
			assertNotNull( owner );
			assertNotNull( owner.getAddress() );
			assertEquals( owner.getId(), owner.getAddress().getId() );
			s.flush();
			s.clear();

			address = s.get( OwnerAddress.class, address.getId() );
			assertNotNull( address );
			assertNotNull( address.getOwner() );
			assertEquals( address.getId(), address.getOwner().getId() );

			s.flush();
			s.clear();

			owner = (Owner) s.createCriteria( Owner.class )
					.add( Restrictions.idEq( owner.getId() ) )
					.uniqueResult();

			assertNotNull( owner );
			assertNotNull( owner.getAddress() );
			assertEquals( owner.getId(), owner.getAddress().getId() );
			s.flush();
			s.clear();

			address = (OwnerAddress) s.createCriteria( OwnerAddress.class )
					.add( Restrictions.idEq( address.getId() ) )
					.uniqueResult();

			address = s.get( OwnerAddress.class, address.getId() );
			assertNotNull( address );
			assertNotNull( address.getOwner() );
			assertEquals( address.getId(), address.getOwner().getId() );

			s.flush();
			s.clear();
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-5757")
	public void testHqlQuery() throws Exception {
		//test a default one to one and a mappedBy in the other side
		final Passport passport = TransactionUtil.doInHibernate( this::sessionFactory, session -> {
			Customer c = new Customer();
			c.setName( "Hibernatus" );
			Passport p = new Passport();
			p.setNumber( "123456789" );
			session.persist( c ); //we need the id to assigned it to passport
			c.setPassport( p );
			p.setOwner( c );
			p.setId( c.getId() );
			return p;
		} );

		final Customer customer = TransactionUtil.doInHibernate( this::sessionFactory, session -> {
			final Customer c = (Customer) session.createQuery( "from Customer c where c.passport = :passport " )
					.setParameter( "passport", passport ).getSingleResult();

			assertThat( c, is( notNullValue() ) );
			return c;
		} );

		TransactionUtil.doInHibernate( this::sessionFactory, session -> {
			final Passport p = (Passport) session.createQuery( "from Passport p where p.owner = :owner " )
					.setParameter( "owner", customer ).getSingleResult();

			assertThat( p, is( notNullValue() ) );
		} );
	}
	
	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				PartyAffiliate.class,
				Party.class,
				Trousers.class,
				TrousersZip.class,
				Customer.class,
				Ticket.class,
				Discount.class,
				Passport.class,
				Client.class,
				Address.class,
				Computer.class,
				SerialNumber.class,
				Body.class,
				Heart.class,
				Owner.class,
				OwnerAddress.class
		};
	}

	@Override
	protected String[] getXmlFiles() {
		return new String[] { "org/hibernate/test/annotations/onetoone/orm.xml" };
	}

	/**
	 * Verifies that generated 'select' statement has desired number of joins 
	 * @author Sharath Reddy
	 *
	 */
	class JoinCounter extends EmptyInterceptor {
	     
	    private static final long serialVersionUID = -3689681272273261051L;
	    
	    private int expectedNumberOfJoins = 0;
	    private String nextValRegex;
	            
	    public JoinCounter(int val) {
	        super();
	        this.expectedNumberOfJoins = val;
	        try {
	        	nextValRegex = ".*" + getDialect().getSelectSequenceNextValString(".*") + ".*";
	        	nextValRegex = nextValRegex.replace( "(", "\\(" );
	        	nextValRegex = nextValRegex.replace( ")", "\\)" );
	        } catch (MappingException ex) {
	        	nextValRegex = "nextval";
	        }
	    }

	    public String onPrepareStatement(String sql) {
	        int numberOfJoins = 0;
	        if (sql.startsWith("select") & !sql.matches(nextValRegex)) {
	             numberOfJoins = count(sql, "join");
	             assertEquals( sql,  expectedNumberOfJoins, numberOfJoins );
	        }
	                        
	        return sql;
	     }
	    
	     /**
	       * Count the number of instances of substring within a string.
	       *
	       * @param string     String to look for substring in.
	       * @param substring  Sub-string to look for.
	       * @return           Count of substrings in string.
	       */
	      private int count(final String string, final String substring)
	      {
	         int count = 0;
	         int idx = 0;

	         while ((idx = string.indexOf(substring, idx)) != -1)
	         {
	            idx++;
	            count++;
	         }

	         return count;
	      }
	}
}
