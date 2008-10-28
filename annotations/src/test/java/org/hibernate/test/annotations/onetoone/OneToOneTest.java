//$Id$
package org.hibernate.test.annotations.onetoone;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.test.annotations.Customer;
import org.hibernate.test.annotations.Discount;
import org.hibernate.test.annotations.Passport;
import org.hibernate.test.annotations.TestCase;
import org.hibernate.test.annotations.Ticket;

/**
 * @author Emmanuel Bernard
 */
public class OneToOneTest extends TestCase {

	public OneToOneTest(String x) {
		super( x );
	}

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
		c = (Client) q.uniqueResult();
		//c = (Client) s.get(Client.class, c.getId());
		assertNotNull( c );
		tx.commit();
		s.close();
		assertNotNull( c.getAddress() );
		//assertTrue( "Should be eager fetched", Hibernate.isInitialized( c.getAddress() ) );

	}

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
		c = (Customer) s.get( Customer.class, c.getId() );
		assertNotNull( c );
		p = c.getPassport();
		assertNotNull( p );
		assertEquals( "123456789", p.getNumber() );
		assertNotNull( p.getOwner() );
		assertEquals( "Hibernatus", p.getOwner().getName() );
		tx.commit(); // commit or rollback is the same, we don't care for read queries
		s.close();
	}

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
		c = (Client) s.get( Client.class, c.getId() );
		assertNotNull( c );
		assertNotNull( c.getAddress() );
		assertEquals( "Paris", c.getAddress().getCity() );
		tx.commit();
		s.close();
	}

	public void testUnidirectionalTrueOneToOne() throws Exception {
		Body b = new Body();
		Heart h = new Heart();
		b.setHeart( h );
		b.setId( new Integer( 1 ) );
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
		b = (Body) s.get( Body.class, b.getId() );
		assertNotNull( b );
		assertNotNull( b.getHeart() );
		assertEquals( h.getId(), b.getHeart().getId() );
		tx.commit();
		s.close();

	}

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
		c = (Computer) s.get( Computer.class, cid );
		assertNotNull( c );
		assertNotNull( c.getSerial() );
		assertEquals( sn.getValue(), c.getSerial().getValue() );
		tx.commit();
		s.close();
	}

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
		s.persist( affiliate );
		s.getTransaction().commit();

		s.clear();

		Transaction tx = s.beginTransaction();
		affiliate = (PartyAffiliate) s.get( PartyAffiliate.class, "id" );
		assertNotNull( affiliate.party );
		assertEquals( affiliate.partyId, affiliate.party.partyId );

		s.clear();

		party = (Party) s.get( Party.class, "id" );
		assertNotNull( party.partyAffiliate );
		assertEquals( party.partyId, party.partyAffiliate.partyId );

		s.delete( party );
		s.delete( party.partyAffiliate );
		tx.commit();
		s.close();
	}

	public void testBidirectionalFkOneToOne() throws Exception {
		Session s = openSession();
		s.getTransaction().begin();
		Trousers trousers = new Trousers();
		TrousersZip zip = new TrousersZip();
		trousers.id = new Integer( 1 );
		zip.id = new Integer( 2 );
		trousers.zip = zip;
		zip.trousers = trousers;
		s.persist( trousers );
		s.persist( zip );
		s.getTransaction().commit();

		s.clear();

		Transaction tx = s.beginTransaction();
		trousers = (Trousers) s.get( Trousers.class, trousers.id );
		assertNotNull( trousers.zip );
		assertEquals( zip.id, trousers.zip.id );

		s.clear();

		zip = (TrousersZip) s.get( TrousersZip.class, zip.id );
		assertNotNull( zip.trousers );
		assertEquals( trousers.id, zip.trousers.id );

		s.delete( zip );
		s.delete( zip.trousers );
		tx.commit();
		s.close();
	}

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
		owner = (Owner) s.get( Owner.class, owner.getId() );
		assertNotNull( owner );
		assertNotNull( owner.getAddress() );
		assertEquals( owner.getId(), owner.getAddress().getId() );
		tx.rollback();
		s.close();
	}

	/**
	 * @see org.hibernate.test.annotations.TestCase#getMappings()
	 */
	protected Class[] getMappings() {
		return new Class[]{
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

}
