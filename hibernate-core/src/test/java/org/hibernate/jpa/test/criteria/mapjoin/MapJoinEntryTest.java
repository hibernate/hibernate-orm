/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.criteria.mapjoin;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.MapJoin;
import javax.persistence.criteria.Root;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.testing.TestForIssue;
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
	@TestForIssue(jiraKey = "HHH-12945")
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
