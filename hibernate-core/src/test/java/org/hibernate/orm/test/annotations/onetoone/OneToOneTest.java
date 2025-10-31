/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.onetoone;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.hibernate.Hibernate;
import org.hibernate.orm.test.annotations.Customer;
import org.hibernate.orm.test.annotations.Discount;
import org.hibernate.orm.test.annotations.Passport;
import org.hibernate.orm.test.annotations.Ticket;
import org.hibernate.query.Query;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaRoot;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * @author Emmanuel Bernard
 */
@DomainModel(
		annotatedClasses = {
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
		}
)
@SessionFactory
public class OneToOneTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Test
	public void testEagerFetching(SessionFactoryScope scope) {
		final String clientName = "Emmanuel";
		scope.inTransaction( session -> {
			Client c = new Client();
			c.setName( clientName );
			Address a = new Address();
			a.setCity( "Courbevoie" );
			c.setAddress( a );
			session.persist( c );
		} );

		final Client client = scope.fromTransaction( session -> {
			Query<Client> q = session.createQuery( "select c from Client c where c.name = :name", Client.class );
			q.setParameter( "name", clientName );
			Client c = q.uniqueResult();
			//c = (Client) s.get(Client.class, c.getId());

			assertThat( c ).isNotNull();
			return c;
		} );

		assertThat( client.getAddress() ).isNotNull();
		assertThat( Hibernate.isInitialized( client.getAddress() ) )
				.describedAs( "Should be eager fetched" )
				.isTrue();
	}

	@Test
	public void testDefaultOneToOne(SessionFactoryScope scope) {
		//test a default one to one and a mappedBy in the other side
		Long customerId = scope.fromTransaction( session -> {
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

		scope.inTransaction( session -> {
			Customer c = session.find( Customer.class, customerId );
			assertThat( c ).isNotNull();
			Passport p = c.getPassport();

			assertThat( p ).isNotNull();
			assertThat( p.getNumber() ).isEqualTo( "123456789" );
			assertThat( p.getOwner() ).isNotNull();
			assertThat( p.getOwner().getName() ).isEqualTo( "Hibernatus" );
		} );
	}

	@Test
	public void testOneToOneWithExplicitFk(SessionFactoryScope scope) {
		final Client c = new Client();
		Address a = new Address();
		a.setCity( "Paris" );
		c.setName( "Emmanuel" );
		c.setAddress( a );

		scope.inTransaction( session ->
				session.persist( c )
		);

		scope.inTransaction( session -> {
			Client client = session.find( Client.class, c.getId() );

			assertThat( client ).isNotNull();
			assertThat( client.getAddress() ).isNotNull();
			assertThat( client.getAddress().getCity() ).isEqualTo( "Paris" );
		} );
	}

	@Test
	public void testOneToOneWithExplicitSecondaryTableFk(SessionFactoryScope scope) {
		final Client c = new Client();
		Address a = new Address();
		a.setCity( "Paris" );
		c.setName( "Emmanuel" );
		c.setSecondaryAddress( a );

		scope.inTransaction( session ->
				session.persist( c )
		);

		scope.inTransaction( session -> {
			final Client client = session.find( Client.class, c.getId() );

			assertThat( client ).isNotNull();
			assertThat( client.getSecondaryAddress() ).isNotNull();
			assertThat( client.getSecondaryAddress().getCity() ).isEqualTo( "Paris" );
		} );
	}

	@Test
	public void testUnidirectionalTrueOneToOne(SessionFactoryScope scope) {
		final Body b = new Body();
		final Heart h = new Heart();
		b.setHeart( h );
		b.setId( 1 );
		h.setId( b.getId() ); //same PK
		scope.inTransaction( session -> {
			session.persist( h );
			session.persist( b );
		} );

		scope.inTransaction( session -> {
			final Body body = session.find( Body.class, b.getId() );

			assertThat( body ).isNotNull();
			assertThat( body.getHeart() ).isNotNull();
			assertThat( body.getHeart().getId() ).isEqualTo( h.getId() );
		} );
	}

	@Test
	public void testCompositePk(SessionFactoryScope scope) {
		final ComputerPk cid = new ComputerPk();
		final SerialNumber sn = new SerialNumber();
		scope.inTransaction( session -> {
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

		scope.inTransaction( session -> {
			Computer c = session.find( Computer.class, cid );
			assertThat( c ).isNotNull();
			assertThat( c.getSerial() ).isNotNull();
			assertThat( c.getSerial().getValue() ).isEqualTo( sn.getValue() );
		} );
	}

	@Test
	public void testBidirectionalTrueOneToOne(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					try {
						Party party = new Party();
						PartyAffiliate affiliate = new PartyAffiliate();
						affiliate.partyId = "id";
						party.partyId = "id";
						party.partyAffiliate = affiliate;
						affiliate.party = party;

						session.beginTransaction();

						session.persist( party );
						session.getTransaction().commit();

						session.clear();

						session.beginTransaction();

						affiliate = session.find( PartyAffiliate.class, "id" );
						assertThat( affiliate.party ).isNotNull();
						assertThat( affiliate.party.partyId ).isEqualTo( affiliate.partyId );

						session.clear();

						party = session.find( Party.class, "id" );
						assertThat( party.partyAffiliate ).isNotNull();
						assertThat( party.partyAffiliate.partyId ).isEqualTo( party.partyId );

						session.remove( party );
						session.remove( party.partyAffiliate );
						session.getTransaction().commit();
					}
					finally {
						if ( session.getTransaction() != null && session.getTransaction().isActive() ) {
							session.getTransaction().rollback();
						}
					}
				}
		);
	}

	@Test
	public void testBidirectionalFkOneToOne(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					try {
						session.beginTransaction();
						Trousers trousers = new Trousers();
						TrousersZip zip = new TrousersZip();

						trousers.id = 1;
						zip.id = 2;
						trousers.zip = zip;
						zip.trousers = trousers;
						session.persist( trousers );
						session.persist( zip );
						session.getTransaction().commit();

						session.clear();

						session.beginTransaction();
						trousers = session.find( Trousers.class, trousers.id );
						assertThat( trousers.zip ).isNotNull();
						assertThat( trousers.zip.id ).isEqualTo( zip.id );

						session.clear();

						zip = session.find( TrousersZip.class, zip.id );
						assertThat( zip.trousers ).isNotNull();
						assertThat( zip.trousers.id ).isEqualTo( trousers.id );

						session.remove( zip );
						session.remove( zip.trousers );
						session.getTransaction().commit();
					}
					finally {
						if ( session.getTransaction() != null && session.getTransaction().isActive() ) {
							session.getTransaction().rollback();
						}
					}
				}
		);
	}

	@Test
	public void testForeignGenerator(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Owner owner = new Owner();
			OwnerAddress address = new OwnerAddress();
			owner.setAddress( address );
			address.setOwner( owner );
			session.persist( owner );
			session.flush();
			session.clear();
			owner = session.find( Owner.class, owner.getId() );
			assertThat( owner ).isNotNull();
			assertThat( owner.getAddress() ).isNotNull();
			assertThat( owner.getAddress().getId() ).isEqualTo( owner.getId() );
		} );
	}

	@Test
	@JiraKey(value = "HHH-6723")
	public void testPkOneToOneSelectStatementDoesNotGenerateExtraJoin(SessionFactoryScope scope) {
		// This test uses an interceptor to verify that correct number of joins are generated.
		scope.inTransaction( session -> {

			Owner owner = new Owner();
			OwnerAddress address = new OwnerAddress();
			owner.setAddress( address );
			address.setOwner( owner );
			session.persist( owner );
			session.flush();
			session.clear();

			owner = session.find( Owner.class, owner.getId() );
			assertThat( owner ).isNotNull();
			assertThat( owner.getAddress() ).isNotNull();
			assertThat( owner.getAddress().getId() ).isEqualTo( owner.getId() );
			session.flush();
			session.clear();

			address = session.find( OwnerAddress.class, address.getId() );
			assertThat( address ).isNotNull();
			assertThat( address.getOwner() ).isNotNull();
			assertThat( address.getOwner().getId() ).isEqualTo( address.getId() );

			session.flush();
			session.clear();

			CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
			CriteriaQuery<Owner> criteria = criteriaBuilder.createQuery( Owner.class );
			Root<Owner> root = criteria.from( Owner.class );
			criteria.where( criteriaBuilder.equal( root.get( "id" ), owner.getId() ) );
			owner = session.createQuery( criteria ).uniqueResult();
//			owner = (Owner) s.createCriteria( Owner.class )
//					.add( Restrictions.idEq( owner.getId() ) )
//					.uniqueResult();

			assertThat( owner ).isNotNull();
			assertThat( owner.getAddress() ).isNotNull();
			assertThat( owner.getAddress().getId() ).isEqualTo( owner.getId() );
			session.flush();
			session.clear();

			CriteriaQuery<OwnerAddress> criteriaQuery = criteriaBuilder.createQuery( OwnerAddress.class );
			Root<OwnerAddress> ownerAddressRoot = criteriaQuery.from( OwnerAddress.class );
			criteriaQuery.where( criteriaBuilder.equal( ownerAddressRoot.get( "id" ), address.getId() ) );

			address = session.createQuery( criteriaQuery ).uniqueResult();
//			address = (OwnerAddress) s.createCriteria( OwnerAddress.class )
//					.add( Restrictions.idEq( address.getId() ) )
//					.uniqueResult();

			address = session.find( OwnerAddress.class, address.getId() );
			assertThat( address ).isNotNull();
			assertThat( address.getOwner() ).isNotNull();
			assertThat( address.getOwner().getId() ).isEqualTo( address.getId() );

			session.flush();
			session.clear();
		} );
	}

	@Test
	@JiraKey(value = "HHH-5757")
	public void testHqlQuery(SessionFactoryScope scope) {
		//test a default one to one and a mappedBy in the other side
		final Passport passport = scope.fromTransaction( session -> {
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

		final Customer customer = scope.fromTransaction( session -> {
			final Customer c = session.createQuery( "from Customer c where c.passport = :passport ", Customer.class )
					.setParameter( "passport", passport )
					.getSingleResult();

			assertThat( c ).isNotNull();
			return c;
		} );

		scope.inTransaction( session -> {
			final Passport p = session.createQuery( "from Passport p where p.owner = :owner ", Passport.class )
					.setParameter( "owner", customer )
					.getSingleResult();

			assertThat( p ).isNotNull();
		} );
	}

	@Test
	public void testDereferenceOneToOne(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
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

		scope.inTransaction( session -> {
			HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
			JpaCriteriaQuery<Client> query = cb.createQuery( Client.class );
			JpaRoot<Client> root = query.from( Client.class );
			query.where( root.get( "address" ).get( "city" ).isNull() );
			List<Client> resultList = session.createQuery( query ).getResultList();

			assertThat( resultList.size() ).isEqualTo( 1 );
			assertThat( resultList.get( 0 ).getName() ).isEqualTo( "C3" );
		} );
	}
}
