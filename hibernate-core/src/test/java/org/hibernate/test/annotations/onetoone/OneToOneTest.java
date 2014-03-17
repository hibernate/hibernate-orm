/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.annotations.onetoone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.hibernate.EmptyInterceptor;
import org.hibernate.MappingException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.relational.TableSpecification;
import org.hibernate.test.annotations.Customer;
import org.hibernate.test.annotations.Discount;
import org.hibernate.test.annotations.Passport;
import org.hibernate.test.annotations.Ticket;
import org.hibernate.testing.FailureExpectedWithNewMetamodel;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Emmanuel Bernard
 */
@FailureExpectedWithNewMetamodel
public class OneToOneTest extends BaseCoreFunctionalTestCase {
	@Test
	public void testEagerFetching() throws Exception {
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		Client c = new Client();
		c.setName( "Emmanuel" );
		Address a = new Address();
		a.setCity( "Courbevoie" );
		c.setAddress( a );
		s.persist( c );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		Query q = s.createQuery( "select c from Client c where c.name = :name" );
		q.setString( "name", c.getName() );
		c = ( Client ) q.uniqueResult();
		//c = (Client) s.get(Client.class, c.getId());
		assertNotNull( c );
		tx.commit();
		s.close();
		assertNotNull( c.getAddress() );
		//assertTrue( "Should be eager fetched", Hibernate.isInitialized( c.getAddress() ) );

	}

	@Test
	public void testDefaultOneToOne() throws Exception {
		//test a default one to one and a mappedBy in the other side
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		Customer c = new Customer();
		c.setName( "Hibernatus" );
		Passport p = new Passport();
		p.setNumber( "123456789" );
		s.persist( c ); //we need the id to assigned it to passport
		c.setPassport( p );
		p.setOwner( c );
		p.setId( c.getId() );
		tx.commit();
		s.close();
		s = openSession();
		tx = s.beginTransaction();
		c = ( Customer ) s.get( Customer.class, c.getId() );
		assertNotNull( c );
		p = c.getPassport();
		assertNotNull( p );
		assertEquals( "123456789", p.getNumber() );
		assertNotNull( p.getOwner() );
		assertEquals( "Hibernatus", p.getOwner().getName() );
		tx.commit(); // commit or rollback is the same, we don't care for read queries
		s.close();
	}

	@Test
	public void testOneToOneWithExplicitFk() throws Exception {
		Client c = new Client();
		Address a = new Address();
		a.setCity( "Paris" );
		c.setName( "Emmanuel" );
		c.setAddress( a );

		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		s.persist( c );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		c = ( Client ) s.get( Client.class, c.getId() );
		assertNotNull( c );
		assertNotNull( c.getAddress() );
		assertEquals( "Paris", c.getAddress().getCity() );
		tx.commit();
		s.close();
	}

	@Test
	public void testOneToOneWithExplicitSecondaryTableFk() throws Exception {
		Client c = new Client();
		Address a = new Address();
		a.setCity( "Paris" );
		c.setName( "Emmanuel" );
		c.setSecondaryAddress( a );

		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		s.persist( c );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		c = ( Client ) s.get( Client.class, c.getId() );
		assertNotNull( c );
		assertNotNull( c.getSecondaryAddress() );
		assertEquals( "Paris", c.getSecondaryAddress().getCity() );
		tx.commit();
		s.close();
	}

	@Test
	public void testUnidirectionalTrueOneToOne() throws Exception {
		Body b = new Body();
		Heart h = new Heart();
		b.setHeart( h );
		b.setId( 1 );
		h.setId( b.getId() ); //same PK
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		s.persist( h );
		s.persist( b );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		b = ( Body ) s.get( Body.class, b.getId() );
		assertNotNull( b );
		assertNotNull( b.getHeart() );
		assertEquals( h.getId(), b.getHeart().getId() );
		tx.commit();
		s.close();
	}

	@Test
	public void testCompositePk() throws Exception {
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		ComputerPk cid = new ComputerPk();
		cid.setBrand( "IBM" );
		cid.setModel( "ThinkPad" );
		Computer c = new Computer();
		c.setId( cid );
		c.setCpu( "2 GHz" );
		SerialNumberPk sid = new SerialNumberPk();
		sid.setBrand( cid.getBrand() );
		sid.setModel( cid.getModel() );
		SerialNumber sn = new SerialNumber();
		sn.setId( sid );
		sn.setValue( "REZREZ23424" );
		c.setSerial( sn );
		s.persist( c );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		c = ( Computer ) s.get( Computer.class, cid );
		assertNotNull( c );
		assertNotNull( c.getSerial() );
		assertEquals( sn.getValue(), c.getSerial().getValue() );
		tx.commit();
		s.close();
	}

	@Test
	public void testBidirectionalTrueOneToOne() throws Exception {
		Session s = openSession();
		s.getTransaction().begin();
		Party party = new Party();
		PartyAffiliate affiliate = new PartyAffiliate();
		affiliate.partyId = "id";
		party.partyId = "id";
		party.partyAffiliate = affiliate;
		affiliate.party = party;
		
		s.persist( party );
		s.getTransaction().commit();

		s.clear();

		Transaction tx = s.beginTransaction();
		affiliate = ( PartyAffiliate ) s.get( PartyAffiliate.class, "id" );
		assertNotNull( affiliate.party );
		assertEquals( affiliate.partyId, affiliate.party.partyId );

		s.clear();

		party = ( Party ) s.get( Party.class, "id" );
		assertNotNull( party.partyAffiliate );
		assertEquals( party.partyId, party.partyAffiliate.partyId );

		s.delete( party );
		s.delete( party.partyAffiliate );
		tx.commit();
		s.close();
	}

	@Test
	public void testBidirectionalFkOneToOne() throws Exception {
		Session s = openSession();
		s.getTransaction().begin();
		Trousers trousers = new Trousers();
		TrousersZip zip = new TrousersZip();
		trousers.id = 1;
		zip.id = 2;
		trousers.zip = zip;
		zip.trousers = trousers;
		s.persist( trousers );
		s.persist( zip );
		s.getTransaction().commit();

		s.clear();

		Transaction tx = s.beginTransaction();
		trousers = ( Trousers ) s.get( Trousers.class, trousers.id );
		assertNotNull( trousers.zip );
		assertEquals( zip.id, trousers.zip.id );

		s.clear();

		zip = ( TrousersZip ) s.get( TrousersZip.class, zip.id );
		assertNotNull( zip.trousers );
		assertEquals( trousers.id, zip.trousers.id );

		s.delete( zip );
		s.delete( zip.trousers );
		tx.commit();
		s.close();
	}

	@Test
	public void testForeignGenerator() {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Owner owner = new Owner();
		OwnerAddress address = new OwnerAddress();
		owner.setAddress( address );
		address.setOwner( owner );
		s.persist( owner );
		s.flush();
		s.clear();
		owner = ( Owner ) s.get( Owner.class, owner.getId() );
		assertNotNull( owner );
		assertNotNull( owner.getAddress() );
		assertEquals( owner.getId(), owner.getAddress().getId() );
		tx.rollback();
		s.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-4606" )
	public void testJoinColumnConfiguredInXml() {
		EntityBinding entityBinding = metadata().getEntityBinding( Son.class.getName() );
		TableSpecification table = entityBinding.getSecondaryTables().values().iterator().next().getSecondaryTableReference();
		org.hibernate.metamodel.spi.relational.Column c1= table.locateColumn( "foo" );
		assertNotNull( c1 );
		org.hibernate.metamodel.spi.relational.Column c2= table.locateColumn( "bar" );
		assertNotNull( c2 );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-6723" )
	public void testPkOneToOneSelectStatementDoesNotGenerateExtraJoin() {
		// This test uses an interceptor to verify that correct number of joins are generated.
		Session s = openSession(new JoinCounter(1));
		Transaction tx = s.beginTransaction();
		Owner owner = new Owner();
		OwnerAddress address = new OwnerAddress();
		owner.setAddress( address );
		address.setOwner( owner );
		s.persist( owner );
		s.flush();
		s.clear();
		
		owner = ( Owner ) s.get( Owner.class, owner.getId() );
		assertNotNull( owner );
		assertNotNull( owner.getAddress() );
		assertEquals( owner.getId(), owner.getAddress().getId() );
		s.flush();
		s.clear();
		
		address = ( OwnerAddress ) s.get( OwnerAddress.class, address.getId() );
		assertNotNull( address );
		assertNotNull( address.getOwner() );
		assertEquals( address.getId(), address.getOwner().getId() );

		s.flush();
		s.clear();

		owner = ( Owner ) s.createCriteria( Owner.class )
				.add( Restrictions.idEq( owner.getId() ) )
				.uniqueResult();

		assertNotNull( owner );
		assertNotNull( owner.getAddress() );
		assertEquals( owner.getId(), owner.getAddress().getId() );
		s.flush();
		s.clear();

		address = ( OwnerAddress ) s.createCriteria( OwnerAddress.class )
				.add( Restrictions.idEq( address.getId() ) )
				.uniqueResult();

		address = ( OwnerAddress ) s.get( OwnerAddress.class, address.getId() );
		assertNotNull( address );
		assertNotNull( address.getOwner() );
		assertEquals( address.getId(), address.getOwner().getId() );

		s.flush();
		s.clear();

		tx.rollback();
		s.close();
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
				ComputerPk.class,
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
