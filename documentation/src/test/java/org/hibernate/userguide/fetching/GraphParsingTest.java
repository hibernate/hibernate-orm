package org.hibernate.userguide.fetching;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertNotNull;

import java.text.ParseException;
import java.util.Collections;

import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.hibernate.graph.AbstractEntityGraphTest;
import org.hibernate.graph.EntityGraphParser;
import org.hibernate.graph.EntityGraphs;
import org.hibernate.userguide.fetching.GraphFetchingTest.Employee;
import org.hibernate.userguide.fetching.GraphFetchingTest.Department;
import org.hibernate.userguide.fetching.GraphFetchingTest.Project;
import org.junit.Assert;
import org.junit.Test;

public class GraphParsingTest extends AbstractEntityGraphTest {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[]{ Project.class, Employee.class, Department.class };
	}
	
	@Test
	public void testParsingExample1() {
		EntityManager entityManager = getOrCreateEntityManager();
		//tag::fetching-strategies-dynamic-fetching-entity-graph-parsing-example-1[]
		final EntityGraph<Project> graph = EntityGraphParser.parse(
			entityManager,
			Project.class,
			"employees( department )"
		);
		//end::fetching-strategies-dynamic-fetching-entity-graph-parsing-example-1[]
		
		Assert.assertNotNull( graph );
	}
	
	@Test
	public void testParsingExample2() {
		EntityManager entityManager = getOrCreateEntityManager();
		//tag::fetching-strategies-dynamic-fetching-entity-graph-parsing-example-2[]
		final EntityGraph<Project> graph = EntityGraphParser.parse(
			entityManager,
			Project.class,
			"employees( username, password, accessLevel, department( employees( username ) ) )"
		);
		//end::fetching-strategies-dynamic-fetching-entity-graph-parsing-example-2[]
		
		Assert.assertNotNull( graph );
	}
	
	@Test
	public void testMergingExample() {
		EntityManager entityManager = getOrCreateEntityManager();
		//tag::fetching-strategies-dynamic-fetching-entity-graph-merging-example[]
		final EntityGraph<Project> a = EntityGraphParser.parse(
			entityManager,
			Project.class,
			"employees( username )"
		);
	
		final EntityGraph<Project> b = EntityGraphParser.parse(
			entityManager,
			Project.class,
			"employees( password, accessLevel )"
		);
	
		final EntityGraph<Project> c = EntityGraphParser.parse(
			entityManager,
			Project.class,
			"employees( department( employees( username ) ) )"
		);
		
		final EntityGraph<Project> all = EntityGraphs.merge( entityManager, Project.class, a, b, c );
		//end::fetching-strategies-dynamic-fetching-entity-graph-merging-example[]
		
		final EntityGraph<Project> expected = EntityGraphParser.parse(
			entityManager,
			Project.class,
			"employees( username, password, accessLevel, department( employees( username ) ) )"
		);
		
		Assert.assertTrue( EntityGraphs.equal( expected, all ) );
	}
	
	@Test
	public void testFindExample() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Long userId = 1L;

			//tag::fetching-strategies-dynamic-fetching-entity-graph-apply-example-find[]
			Employee employee = EntityGraphs.find( 
				entityManager,
				Employee.class,
				userId,
				"username, accessLevel, department"
			);
			//end::fetching-strategies-dynamic-fetching-entity-graph-apply-example-find[]
		} );
	}
	
	@Test
	public void testQueryExample() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::fetching-strategies-dynamic-fetching-entity-graph-apply-example-query[]
			final String graphString = "username, accessLevel";
			final String queryString = "select e from Employee e where e.id = 1";
			
			final EntityGraph<Employee> graph = EntityGraphParser.parse(
				entityManager,
				Employee.class,
				graphString       // == "username, accessLevel"
			);
			
			// Given above, the following query1:
			
			TypedQuery<Employee> query1 = entityManager.createQuery( queryString, Employee.class );
			EntityGraphs.setFetchGraph( query1, graph );
			
			// is equal to the following query2:
			
			TypedQuery<Employee> query2 = EntityGraphs.createQuery(
				entityManager,
				Employee.class,
				"fetch " + graphString + " " + queryString
				// == "fetch username, accessLevel select e from Employee e where e.id = 1"
			);
			//end::fetching-strategies-dynamic-fetching-entity-graph-apply-example-query[]
		} );
	}
}
