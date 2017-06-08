/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.graphs.flush;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.hibernate.Hibernate;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.cfg.Environment;
import org.hibernate.jpa.QueryHints;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.TestForIssue;
import org.junit.Before;
import org.junit.Test;

/**
 * @author David Gofferje
 */
public class EntityGraphFlushTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	@SuppressWarnings( {"unchecked"})
	protected void addConfigOptions(Map options) {
		options.put( Environment.GENERATE_STATISTICS, "true" );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-11213")
	public void testFlushOnEntityGraphHintedQuery() {
		EntityManager entityManager = getOrCreateEntityManager();
		entityManager.getTransaction().begin();
		
		Query query = entityManager.createQuery( "from " + Location.class.getName() );
		Location location = (Location)query.getSingleResult();

		// modify the address of the location
		location.address = "123 somewhereelse";

		EntityGraph<?> entityGraph = entityManager.getEntityGraph( "getCompany" );
		query = entityManager.createQuery( "from " + Company.class.getName()  );
		query.setHint( QueryHints.HINT_LOADGRAPH, entityGraph );

		// should be flushed before executing the query
		entityManagerFactory().getStatistics().clear();
		assertEquals( 0, entityManagerFactory().getStatistics().getFlushCount() );
		List results = query.getResultList();
		assertEquals( 1, entityManagerFactory().getStatistics().getFlushCount() );

		Company companyResult = (Company)results.get( 0 );
		assertTrue( Hibernate.isInitialized( companyResult.location ) );
		
		assertTrue( companyResult.location.address.equals("123 somewhereelse"));
		
		Session session = entityManager.unwrap(Session.class);
		SQLQuery sqlQuery = session.createSQLQuery( "select * from Location l where l.address = '123 somewhereelse'" );
		assertEquals(1, sqlQuery.list().size());
		 
		entityManager.getTransaction().commit();
		entityManager.close();

	}
	
	@Before
	public void createData() {
		EntityManager entityManager = getOrCreateEntityManager();
		entityManager.getTransaction().begin();
			
		Location location = new Location();
		location.address = "123 somewhere";
		location.zip = 12345;
		entityManager.persist( location );
			
		Company company = new Company();
		company.location = location;

		entityManager.persist( company );
		
		entityManager.getTransaction().commit();
		entityManager.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Company.class, Location.class };
	}	
}
