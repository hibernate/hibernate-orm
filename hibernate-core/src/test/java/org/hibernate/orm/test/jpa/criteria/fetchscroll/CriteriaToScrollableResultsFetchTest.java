/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria.fetchscroll;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.hibernate.ScrollableResults;
import org.hibernate.dialect.HANADialect;
import org.hibernate.query.Query;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.SkipForDialect;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Chris Cranford
 */
@JiraKey(value = "HHH-10062")
@Jpa(annotatedClasses = {
		Customer.class,
		Order.class,
		OrderLine.class,
		Product.class,
		PurchaseOrg.class,
		Facility.class,
		Site.class
})
public class CriteriaToScrollableResultsFetchTest {

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	@SkipForDialect(dialectClass = HANADialect.class, reason = "HANA only supports forward-only cursors")
	public void testWithScroll(EntityManagerFactoryScope scope) {
		// Creates data necessary for test
		Long facilityId = populate(scope);
		// Controller iterates the data
		for ( OrderLine line : getOrderLinesScrolled( scope, facilityId ) ) {
			// This should ~NOT~ fail with a LazilyLoadException
			assertNotNull( line.getProduct().getFacility().getSite().getName() );
		}
	}

	@Test
	public void testNoScroll(EntityManagerFactoryScope scope) {
		// Creates data necessary for test.
		Long facilityId = populate(scope);
		// Controller iterates the data
		for ( OrderLine line : getOrderLinesJpaFetched( scope, facilityId ) ) {
			assertNotNull( line.getProduct().getFacility().getSite().getName() );
		}
	}

	private List<OrderLine> getOrderLinesScrolled(EntityManagerFactoryScope scope, Long facilityId) {
		return scope.fromTransaction( entityManager -> {

			Set<PurchaseOrg> purchaseOrgs = getPurchaseOrgsByFacilityId( facilityId, entityManager );
			assertEquals( 1, purchaseOrgs.size(), "Expected one purchase organization." );

			TypedQuery<OrderLine> query = getOrderLinesQuery( entityManager, purchaseOrgs );

			Query<?> hibernateQuery = query.unwrap( Query.class );
			hibernateQuery.setReadOnly( true );
			hibernateQuery.setCacheable( false );

			List<OrderLine> lines = new ArrayList<>();
			try (ScrollableResults<?> scrollableResults = hibernateQuery.scroll()) {
				scrollableResults.last();
				scrollableResults.beforeFirst();
				while ( scrollableResults.next() ) {
					lines.add( (OrderLine) scrollableResults.get() );
				}
			}
			assertNotNull( lines );
			assertEquals( 1, lines.size(), "Expected one order line" );

			return lines;
		} );
	}

	private List<OrderLine> getOrderLinesJpaFetched(EntityManagerFactoryScope scope, Long facilityId) {
		return scope.fromTransaction( entityManager -> {

			Set<PurchaseOrg> purchaseOrgs = getPurchaseOrgsByFacilityId( facilityId, entityManager );
			assertEquals( 1, purchaseOrgs.size(), "Expected one purchase organization." );

			TypedQuery<OrderLine> query = getOrderLinesQuery( entityManager, purchaseOrgs );
			return query.getResultList();
		} );
	}

	private Set<PurchaseOrg> getPurchaseOrgsByFacilityId(Long facilityId, EntityManager em) {
		Set<PurchaseOrg> orgs = new HashSet<>();
		for ( PurchaseOrg purchaseOrg : findAll( PurchaseOrg.class, em ) ) {
			for ( Facility facility : purchaseOrg.getFacilities() ) {
				if ( facility.getId().equals( facilityId ) ) {
					orgs.add( purchaseOrg );
					break;
				}
			}
		}
		return orgs;
	}

	private <T> List<T> findAll(Class<T> clazz, EntityManager entityManager) {
		return entityManager.createQuery( "SELECT o FROM " + clazz.getSimpleName() + " o", clazz ).getResultList();
	}

	private TypedQuery<OrderLine> getOrderLinesQuery(EntityManager entityManager, Collection<PurchaseOrg> purchaseOrgs) {
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<OrderLine> query = cb.createQuery( OrderLine.class );
		Root<OrderLine> root = query.from( OrderLine.class );
		Path<OrderLineId> idPath = root.get( OrderLine_.id );

		Join<OrderLine, Product> productJoin = (Join<OrderLine, Product>) root.fetch( OrderLine_.product );
		productJoin.fetch( Product_.facility ).fetch( Facility_.site );

		Join<OrderLine, Order> orderJoin = (Join<OrderLine, Order>) root.fetch( OrderLine_.header );
		orderJoin.fetch( Order_.purchaseOrg );

		Set<Long> ids = new HashSet<>();
		for ( PurchaseOrg org : purchaseOrgs )
			ids.add( org.getId() );

		List<Predicate> predicates = new ArrayList<>();
		predicates.add( idPath.get( OrderLineId_.purchaseOrgId ).in( ids ) );

		query.select( root ).where( predicates.toArray( new Predicate[predicates.size()] ) );

		return entityManager.createQuery( query );
	}

	private Long populate(EntityManagerFactoryScope scope) {
		return scope.fromTransaction( entityManager -> {
			Customer customer = new Customer();
			customer.setName( "MGM" );
			entityManager.persist( customer );

			Site site = new Site();
			site.setName( "NEW YORK" );
			site.setCustomer( customer );
			entityManager.persist( site );

			Facility facility = new Facility();
			facility.setName( "ACME" );
			facility.setSite( site );
			facility.setCustomer( customer );
			entityManager.persist( facility );

			PurchaseOrg purchaseOrg = new PurchaseOrg();
			purchaseOrg.setName( "LOONEY TUNES" );
			purchaseOrg.setCustomer( customer );
			purchaseOrg.setFacilities( List.of( facility ) );
			entityManager.persist( purchaseOrg );

			Product product = new Product( facility, "0000 0001" );
			entityManager.persist( product );

			Order order = new Order( purchaseOrg, "12345" );

			OrderLine line1 = new OrderLine( order, 1L, product );
			Set<OrderLine> lines = new HashSet<>();
			lines.add( line1 );
			order.setLines( lines );

			entityManager.persist( order );

			return facility.getId();
		} );
	}

}
