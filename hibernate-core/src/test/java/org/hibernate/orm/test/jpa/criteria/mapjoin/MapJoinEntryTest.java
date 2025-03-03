/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria.mapjoin;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Map;

import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.MapJoin;
import jakarta.persistence.criteria.Root;

import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Before;
import org.junit.Test;

public class MapJoinEntryTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[]{ Customer.class, CustomerOrder.class };
	}

	@Before
	public void setup() {
		doInJPA( this::entityManagerFactory, em -> {
			Customer customer = new Customer();
			customer.setName( "Morgan Philips" );
			customer.addOrder( "online", "AA Glass Cleaner", 3 );

			em.persist( customer );
		} );
	}

	@Test
	@JiraKey(value = "HHH-12945")
	public void testMapJoinEntryCriteria() {
		doInJPA( this::entityManagerFactory, em -> {
			CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();

			CriteriaQuery<Map.Entry> query = criteriaBuilder.createQuery( Map.Entry.class );
			Root<Customer> customer = query.from( Customer.class );
			MapJoin<Customer, String, CustomerOrder> orderMap = customer.join( Customer_.orderMap );
			query.select( orderMap.entry() );

			TypedQuery<Map.Entry> typedQuery = em.createQuery( query );
			List<Map.Entry> resultList = typedQuery.getResultList();

			assertEquals( 1, resultList.size() );
			assertEquals( "online", resultList.get( 0 ).getKey() );
			assertEquals( "AA Glass Cleaner", ( (CustomerOrder) resultList.get( 0 ).getValue() ).getItem() );
		} );
	}

	@Test
	public void testMapJoinEntryJPQL() {
		doInJPA( this::entityManagerFactory, em -> {
			TypedQuery<Map.Entry> query = em.createQuery( "SELECT ENTRY(mp) FROM Customer c JOIN c.orderMap mp",
					Map.Entry.class );
			List<Map.Entry> resultList = query.getResultList();

			assertEquals( 1, resultList.size() );
			assertEquals( "online", resultList.get( 0 ).getKey() );
			assertEquals( "AA Glass Cleaner", ( (CustomerOrder) resultList.get( 0 ).getValue() ).getItem() );
		} );
	}
}
