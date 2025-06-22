/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.onetoone;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.orm.test.annotations.Customer;
import org.hibernate.orm.test.annotations.Discount;
import org.hibernate.orm.test.annotations.Passport;
import org.hibernate.orm.test.annotations.Ticket;
import org.hibernate.query.Query;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaRoot;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.hibernate.testing.transaction.TransactionUtil;
import org.junit.Test;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Emmanuel Bernard
 */
public class OneToOneTest extends BaseNonConfigCoreFunctionalTestCase {
	@Test
	public void testEagerFetching() {
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
			q.setParameter( "name", clientName );
			Client c = (Client) q.uniqueResult();
			//c = (Client) s.get(Client.class, c.getId());

			assertNotNull( c );
			return c;
		} );

		assertNotNull( client.getAddress() );
		//assertTrue( "Should be eager fetched", Hibernate.isInitialized( c.getAddress() ) );
	}

	@Test
	public void testDefaultOneToOne() {
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
	public void testOneToOneWithExplicitFk() {
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
	public void testOneToOneWithExplicitSecondaryTableFk() {
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
	public void testUnidirectionalTrueOneToOne() {
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
	public void testCompositePk() {
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
	public void testBidirectionalTrueOneToOne() {
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

				s.remove( party );
				s.remove( party.partyAffiliate );
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
	public void testBidirectionalFkOneToOne() {
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

				s.remove( zip );
				s.remove( zip.trousers );
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
	@JiraKey(value = "HHH-6723")
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

			CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
			CriteriaQuery<Owner> criteria = criteriaBuilder.createQuery( Owner.class );
			Root<Owner> root = criteria.from( Owner.class );
			criteria.where( criteriaBuilder.equal( root.get("id"), owner.getId() ) );
			owner = s.createQuery( criteria ).uniqueResult();
//			owner = (Owner) s.createCriteria( Owner.class )
//					.add( Restrictions.idEq( owner.getId() ) )
//					.uniqueResult();

			assertNotNull( owner );
			assertNotNull( owner.getAddress() );
			assertEquals( owner.getId(), owner.getAddress().getId() );
			s.flush();
			s.clear();

			CriteriaQuery<OwnerAddress> criteriaQuery = criteriaBuilder.createQuery( OwnerAddress.class );
			Root<OwnerAddress> ownerAddressRoot = criteriaQuery.from( OwnerAddress.class );
			criteriaQuery.where( criteriaBuilder.equal( ownerAddressRoot.get( "id" ), address.getId() ) );

			address = s.createQuery( criteriaQuery ).uniqueResult();
//			address = (OwnerAddress) s.createCriteria( OwnerAddress.class )
//					.add( Restrictions.idEq( address.getId() ) )
//					.uniqueResult();

			address = s.get( OwnerAddress.class, address.getId() );
			assertNotNull( address );
			assertNotNull( address.getOwner() );
			assertEquals( address.getId(), address.getOwner().getId() );

			s.flush();
			s.clear();
		} );
	}

	@Test
	@JiraKey(value = "HHH-5757")
	public void testHqlQuery() {
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

	@Test
	public void testDereferenceOneToOne() {
		TransactionUtil.doInHibernate( this::sessionFactory, session -> {
			Client c1 = new Client();
			c1.setName( "C1" );
			Client c2 = new Client();
			c2.setName( "C2" );
			Client c3 = new Client();
			c3.setName( "C3" );
			Address a = new Address();
			a.setCity( "Vienna" );
			c1.setAddress( a );
			c3.setAddress( new Address() );
			session.persist( c1 );
			session.persist( c2 );
			session.persist( c3 );
		} );

		TransactionUtil.doInHibernate( this::sessionFactory, session -> {
			HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
			JpaCriteriaQuery<Client> query = cb.createQuery( Client.class );
			JpaRoot<Client> root = query.from( Client.class );
			query.where( root.get( "address" ).get( "city" ).isNull() );
			List<Client> resultList = session.createQuery( query ).getResultList();

			assertEquals( 1, resultList.size() );
			assertEquals( "C3", resultList.get( 0 ).getName() );
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

}
