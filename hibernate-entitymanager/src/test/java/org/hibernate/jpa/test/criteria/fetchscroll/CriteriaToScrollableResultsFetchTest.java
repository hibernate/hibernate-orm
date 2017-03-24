
package org.hibernate.jpa.test.criteria.fetchscroll;

import java.util.Collection;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.*;
import javax.persistence.criteria.*;

import org.hibernate.Query;
import org.hibernate.ScrollableResults;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.jpa.test.criteria.fetchscroll.Customer;
import org.hibernate.jpa.test.criteria.fetchscroll.Customer_;
import org.hibernate.jpa.test.criteria.fetchscroll.Facility;
import org.hibernate.jpa.test.criteria.fetchscroll.Facility_;
import org.hibernate.jpa.test.criteria.fetchscroll.OrderLine;
import org.hibernate.jpa.test.criteria.fetchscroll.OrderLine_;
import org.hibernate.jpa.test.criteria.fetchscroll.Product;
import org.hibernate.jpa.test.criteria.fetchscroll.Product_;
import org.hibernate.jpa.test.criteria.fetchscroll.PurchaseOrg;
import org.hibernate.jpa.test.criteria.fetchscroll.PurchaseOrg_;
import org.hibernate.jpa.test.criteria.fetchscroll.Order;
import org.hibernate.jpa.test.criteria.fetchscroll.Order_;
import org.hibernate.jpa.test.criteria.fetchscroll.Site;
import org.hibernate.jpa.test.criteria.fetchscroll.Site_;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class CriteriaToScrollableResultsFetchTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] {
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
	public void testWithScroll() {
		// Creates data necessary for test
		Long facilityId = populate();			
		// Controller iterates the data
		for(OrderLine line : getOrderLinesScrolled(facilityId)) {
			assertNotNull(line.getProduct().getFacility().getSite().getName());
		}		

	}

	@Test
	public void testNoScroll() {
		// Creates data necessary for test.
		Long facilityId = populate();
		// Controller iterates the data
		for(OrderLine line : getOrderLinesJpaFetched(facilityId)) {
			assertNotNull(line.getProduct().getFacility().getSite().getName());
		}
	}

	private List<OrderLine> getOrderLinesScrolled(Long facilityId) {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();		
		
		Set<PurchaseOrg> purchaseOrgs = getPurchaseOrgsByFacilityId(facilityId, em);
		assertEquals("Expected one purchase organization.", 1, purchaseOrgs.size());
		
		TypedQuery<OrderLine> query = getOrderLinesQuery(purchaseOrgs, em);

		Query hibernateQuery = query.unwrap(Query.class);
		hibernateQuery.setReadOnly(true);
		hibernateQuery.setCacheable(false);
		
		List<OrderLine> lines = new ArrayList<>();
		ScrollableResults scrollableResults = hibernateQuery.scroll();
		scrollableResults.last();
		int rows = scrollableResults.getRowNumber() + 1;
		scrollableResults.beforeFirst();
		while(scrollableResults.next()) {
			lines.add((OrderLine)scrollableResults.get(0));
		}
		assertNotNull(lines);
		assertEquals("Expected one order line", 1, lines.size());
		
		em.getTransaction().commit();
		em.close();
		return lines;
	}

	private List<OrderLine> getOrderLinesJpaFetched(Long facilityId) {		
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();		
		
		Set<PurchaseOrg> purchaseOrgs = getPurchaseOrgsByFacilityId(facilityId, em);
		assertEquals("Expected one purchase organization.", 1, purchaseOrgs.size());
		
		TypedQuery<OrderLine> query = getOrderLinesQuery(purchaseOrgs, em);
		List<OrderLine> lines = query.getResultList();
		em.close();
		return lines;
	}
	
	private Set<PurchaseOrg> getPurchaseOrgsByFacilityId(Long facilityId, EntityManager em) {
		Set<PurchaseOrg> orgs = new HashSet<>();
		try {
			for(PurchaseOrg purchaseOrg : findAll(PurchaseOrg.class, em)) {
				for(Facility facility : purchaseOrg.getFacilities()) {
					if(facility.getId().equals(facilityId)) {
						orgs.add(purchaseOrg);
						break;
					}
				}
			}
		}
		catch(Exception ex) {
		}
		finally {
			return orgs;
		}
	}

	private <T> List<T> findAll(Class<T> clazz, EntityManager em) {
		return em.createQuery("SELECT o FROM " + clazz.getSimpleName() + " o", clazz).getResultList();
	}
		
	private TypedQuery<OrderLine> getOrderLinesQuery(Collection<PurchaseOrg> purchaseOrgs, EntityManager em) {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<OrderLine> query = cb.createQuery(OrderLine.class);
		Root<OrderLine> root = query.from(OrderLine.class);
		Path<OrderLineId> idPath = root.get(OrderLine_.id);

		Join<OrderLine, Product> productJoin = (Join<OrderLine, Product>)root.fetch(OrderLine_.product);
		productJoin.fetch(Product_.facility).fetch(Facility_.site);
		
		Join<OrderLine, Order> orderJoin = (Join<OrderLine, Order>)root.fetch(OrderLine_.header);
		orderJoin.fetch(Order_.purchaseOrg);
		
		Set<Long> ids = new HashSet<>();
		for(PurchaseOrg org : purchaseOrgs)
			ids.add(org.getId());

		List<Predicate> predicates = new ArrayList<>();
		predicates.add(idPath.get(OrderLineId_.purchaseOrgId).in(ids));
		
		query.select(root).where(predicates.toArray(new Predicate[predicates.size()]));
		
		return em.createQuery(query);		
	}
	
	private Long populate() {
	
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
	
		Customer customer = new Customer();
		customer.setName("MGM");
		em.persist(customer);

		Site site = new Site();
		site.setName("NEW YORK");
		site.setCustomer(customer);
		em.persist(site);
		
		Facility facility = new Facility();
		facility.setName("ACME");
		facility.setSite(site);
		facility.setCustomer(customer);
		em.persist(facility);
		
		PurchaseOrg purchaseOrg = new PurchaseOrg();
		purchaseOrg.setName("LOONEY TUNES");
		purchaseOrg.setCustomer(customer);
		purchaseOrg.setFacilities(Arrays.asList(facility));
		em.persist(purchaseOrg);
		
		Product product = new Product(facility, "0000 0001");	
		em.persist(product);
		
		Order order = new Order(purchaseOrg, "12345");				
		OrderLine line1 = new OrderLine(order, 1L, product);

		Set<OrderLine> lines = new HashSet<>();
		lines.add(line1);

		order.setLines(lines);
		
		em.persist(order);
		
		em.getTransaction().commit();
                em.clear();
		em.close();

		return facility.getId();
	}
	
}
