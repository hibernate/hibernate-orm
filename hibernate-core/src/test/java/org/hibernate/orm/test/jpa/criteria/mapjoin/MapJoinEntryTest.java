/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria.mapjoin;

import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.MapJoin;
import jakarta.persistence.criteria.Root;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Jpa(annotatedClasses = {Customer.class, CustomerOrder.class})
public class MapJoinEntryTest {

	@BeforeEach
	public void setup(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Customer customer = new Customer();
			customer.setName( "Morgan Philips" );
			customer.addOrder( "online", "AA Glass Cleaner", 3 );

			entityManager.persist( customer );
		} );
	}

	@AfterEach
	public void cleanupTestData(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	@JiraKey(value = "HHH-12945")
	public void testMapJoinEntryCriteria(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();

			CriteriaQuery<Map.Entry> query = criteriaBuilder.createQuery( Map.Entry.class );
			Root<Customer> customer = query.from( Customer.class );
			MapJoin<Customer, String, CustomerOrder> orderMap = customer.join( Customer_.orderMap );
			query.select( orderMap.entry() );

			TypedQuery<Map.Entry> typedQuery = entityManager.createQuery( query );
			List<Map.Entry> resultList = typedQuery.getResultList();

			assertEquals( 1, resultList.size() );
			assertEquals( "online", resultList.get( 0 ).getKey() );
			assertEquals( "AA Glass Cleaner", ( (CustomerOrder) resultList.get( 0 ).getValue() ).getItem() );
		} );
	}

	@Test
	public void testMapJoinEntryJPQL(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			TypedQuery<Map.Entry> query = entityManager.createQuery( "SELECT ENTRY(mp) FROM Customer c JOIN c.orderMap mp",
					Map.Entry.class );
			List<Map.Entry> resultList = query.getResultList();

			assertEquals( 1, resultList.size() );
			assertEquals( "online", resultList.get( 0 ).getKey() );
			assertEquals( "AA Glass Cleaner", ( (CustomerOrder) resultList.get( 0 ).getValue() ).getItem() );
		} );
	}
}
