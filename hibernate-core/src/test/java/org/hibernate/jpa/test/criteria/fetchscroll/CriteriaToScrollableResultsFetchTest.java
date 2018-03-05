
package org.hibernate.jpa.test.criteria.fetchscroll;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.hibernate.Query;
import org.hibernate.ScrollableResults;
import org.hibernate.dialect.AbstractHANADialect;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.junit.Test;
import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.TestForIssue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-10062")
public class CriteriaToScrollableResultsFetchTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[]{
				Customer.class,
				Order.class,
				OrderLine.class,
				Product.class,
				PurchaseOrg.class,
				Facility.class,
				Site.class
		};
	}

	@Test
	@SkipForDialect(value = AbstractHANADialect.class, comment = "HANA only supports forward-only cursors")
	public void testWithScroll() {
		// Creates data necessary for test
		Long facilityId = populate();
		// Controller iterates the data
		for ( OrderLine line : getOrderLinesScrolled( facilityId ) ) {
			// This should ~NOT~ fail with a LazilyLoadException
			assertNotNull( line.getProduct().getFacility().getSite().getName() );
		}
	}

	@Test
	public void testNoScroll() {
		// Creates data necessary for test.
		Long facilityId = populate();
		// Controller iterates the data
		for ( OrderLine line : getOrderLinesJpaFetched( facilityId ) ) {
			assertNotNull( line.getProduct().getFacility().getSite().getName() );
		}
	}

	private List<OrderLine> getOrderLinesScrolled(Long facilityId) {
		EntityManager em = getOrCreateEntityManager();
		try {
			em.getTransaction().begin();

			Set<PurchaseOrg> purchaseOrgs = getPurchaseOrgsByFacilityId( facilityId, em );
			assertEquals( "Expected one purchase organization.", 1, purchaseOrgs.size() );
			System.out.println( purchaseOrgs );

			TypedQuery<OrderLine> query = getOrderLinesQuery( purchaseOrgs, em );

			Query hibernateQuery = query.unwrap( Query.class );
			hibernateQuery.setReadOnly( true );
			hibernateQuery.setCacheable( false );

			List<OrderLine> lines = new ArrayList<>();
			ScrollableResults scrollableResults = hibernateQuery.scroll();
			scrollableResults.last();
			int rows = scrollableResults.getRowNumber() + 1;
			scrollableResults.beforeFirst();
			while ( scrollableResults.next() ) {
				lines.add( (OrderLine) scrollableResults.get( 0 ) );
			}
			assertNotNull( lines );
			assertEquals( "Expected one order line", 1, lines.size() );

			em.getTransaction().commit();
			return lines;
		}
		catch (Throwable t) {
			if ( em.getTransaction().isActive() ) {
				em.getTransaction().rollback();
			}
			throw t;
		}
		finally {
			em.close();
		}
	}

	private List<OrderLine> getOrderLinesJpaFetched(Long facilityId) {
		EntityManager em = getOrCreateEntityManager();
		try {
			em.getTransaction().begin();

			Set<PurchaseOrg> purchaseOrgs = getPurchaseOrgsByFacilityId( facilityId, em );
			assertEquals( "Expected one purchase organization.", 1, purchaseOrgs.size() );
			System.out.println( purchaseOrgs );

			TypedQuery<OrderLine> query = getOrderLinesQuery( purchaseOrgs, em );
			List<OrderLine> lines = query.getResultList();
			em.getTransaction().commit();
			return lines;
		}
		catch (Throwable t) {
			if ( em.getTransaction().isActive() ) {
				em.getTransaction().rollback();
			}
			throw t;
		}
		finally {
			em.close();
		}
	}

	private Set<PurchaseOrg> getPurchaseOrgsByFacilityId(Long facilityId, EntityManager em) {
		Set<PurchaseOrg> orgs = new HashSet<>();
		try {
			for ( PurchaseOrg purchaseOrg : findAll( PurchaseOrg.class, em ) ) {
				for ( Facility facility : purchaseOrg.getFacilities() ) {
					if ( facility.getId().equals( facilityId ) ) {
						orgs.add( purchaseOrg );
						break;
					}
				}
			}
		}
		catch (Exception e) {

		}
		finally {
			return orgs;
		}
	}

	private <T> List<T> findAll(Class<T> clazz, EntityManager em) {
		return em.createQuery( "SELECT o FROM " + clazz.getSimpleName() + " o", clazz ).getResultList();
	}

	private TypedQuery<OrderLine> getOrderLinesQuery(Collection<PurchaseOrg> purchaseOrgs, EntityManager em) {
		CriteriaBuilder cb = em.getCriteriaBuilder();
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

		return em.createQuery( query );
	}

	private Long populate() {
		final EntityManager em = getOrCreateEntityManager();
		try {
			em.getTransaction().begin();

			Customer customer = new Customer();
			customer.setName( "MGM" );
			em.persist( customer );

			Site site = new Site();
			site.setName( "NEW YORK" );
			site.setCustomer( customer );
			em.persist( site );

			Facility facility = new Facility();
			facility.setName( "ACME" );
			facility.setSite( site );
			facility.setCustomer( customer );
			em.persist( facility );

			PurchaseOrg purchaseOrg = new PurchaseOrg();
			purchaseOrg.setName( "LOONEY TUNES" );
			purchaseOrg.setCustomer( customer );
			purchaseOrg.setFacilities( Arrays.asList( facility ) );
			em.persist( purchaseOrg );

			Product product = new Product( facility, "0000 0001" );
			em.persist( product );

			Order order = new Order( purchaseOrg, "12345" );
			OrderLine line1 = new OrderLine( order, 1L, product );

			Set<OrderLine> lines = new HashSet<>();
			lines.add( line1 );

			order.setLines( lines );

			em.persist( order );

			em.getTransaction().commit();

			return facility.getId();
		}
		catch (Throwable t) {
			if ( em.getTransaction().isActive() ) {
				em.getTransaction().rollback();
			}
			throw t;
		}
		finally {
			em.close();
		}
	}

}
