/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.criteria.idclass;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.jpa.test.metamodel.AbstractMetamodelSpecificTest;

/**
 * @author Erich Heard
 */
public class IdClassPredicateTest extends AbstractMetamodelSpecificTest {

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] {
				Widget.class,
				Tool.class
		};
	}

	@Before
	public void prepareTestData() {
		EntityManager em = entityManagerFactory().createEntityManager();
		em.getTransaction().begin();

		Widget w = new Widget();
		w.setCode( "AAA" );
		w.setDivision( "NA" );
		w.setCost( 10.00 );
		em.persist( w );

		w = new Widget();
		w.setCode( "AAA" );
		w.setDivision( "EU" );
		w.setCost( 12.50 );
		em.persist( w );

		w = new Widget();
		w.setCode( "AAA" );
		w.setDivision( "ASIA" );
		w.setCost( 110.00 );
		em.persist( w );

		w = new Widget();
		w.setCode( "BBB" );
		w.setDivision( "NA" );
		w.setCost( 14.00 );
		em.persist( w );

		w = new Widget();
		w.setCode( "BBB" );
		w.setDivision( "EU" );
		w.setCost( 8.75 );
		em.persist( w );

		w = new Widget();
		w.setCode( "BBB" );
		w.setDivision( "ASIA" );
		w.setCost( 86.22 );
		em.persist( w );

		Tool t = new Tool();
		t.setName( "AAA" );
		t.setType( "NA" );
		t.setCost( 10.00 );
		em.persist( t );

		t = new Tool();
		t.setName( "AAA" );
		t.setType( "EU" );
		t.setCost( 12.50 );
		em.persist( t );

		t = new Tool();
		t.setName( "AAA" );
		t.setType( "ASIA" );
		t.setCost( 110.00 );
		em.persist( t );

		t = new Tool();
		t.setName( "BBB" );
		t.setType( "NA" );
		t.setCost( 14.00 );
		em.persist( t );

		t = new Tool();
		t.setName( "BBB" );
		t.setType( "EU" );
		t.setCost( 8.75 );
		em.persist( t );

		t = new Tool();
		t.setName( "BBB" );
		t.setType( "ASIA" );
		t.setCost( 86.22 );
		em.persist( t );

		em.getTransaction().commit();
		em.close();
	}


	@After
	public void cleanupTestData() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.createQuery( "delete Widget" ).executeUpdate();
		em.createQuery( "delete Tool" ).executeUpdate();
		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void testCountIdClassAttributes(){
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Long> cq = cb.createQuery(Long.class);
		Root<Widget> path = cq.from(Widget.class);
		Expression<Long> countSelection = cb.count(path);
		cq.select(countSelection);
		Long count = em.createQuery(cq).getSingleResult();
//		// Packaging arguments for use in query.
//		List<String> divisions = new ArrayList<String>( );
//		divisions.add( "NA" );
//		divisions.add( "EU" );
//
//		// Building the query.
//		CriteriaBuilder criteria = em.getCriteriaBuilder( );
//		CriteriaQuery<Widget> query = criteria.createQuery( Widget.class );
//		Root<Widget> root = query.from( Widget.class );
//
//		Predicate predicate = root.get( "division" ).in( divisions );
//		query.where( predicate );
//
//		// Retrieving query.;
//		List<Widget> widgets = em.createQuery( query ).getResultList( );
//		Assert.assertEquals( 4, widgets.size() );

		em.getTransaction().commit();
		em.close();
	}


	@Test
	public void testDeclaredIdClassAttributes( ) {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();

		// Packaging arguments for use in query.
		List<String> divisions = new ArrayList<>( );
		divisions.add( "NA" );
		divisions.add( "EU" );
			
		// Building the query.
		CriteriaBuilder criteria = em.getCriteriaBuilder( );
		CriteriaQuery<Widget> query = criteria.createQuery( Widget.class );
		Root<Widget> root = query.from( Widget.class );
			
		Predicate predicate = root.get( "division" ).in( divisions );
		query.where( predicate );

		// Retrieving query.;
		List<Widget> widgets = em.createQuery( query ).getResultList( );
		Assert.assertEquals( 4, widgets.size() );

		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void testSupertypeIdClassAttributes( ) {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();

		// Packaging arguments for use in query.
		List<String> types = new ArrayList<>( );
		types.add( "NA" );
		types.add( "EU" );

		// Building the query.
		CriteriaBuilder criteria = em.getCriteriaBuilder( );
		CriteriaQuery<Tool> query = criteria.createQuery( Tool.class );
		Root<Tool> root = query.from( Tool.class );

		Predicate predicate = root.get( "type" ).in( types );
		query.where( predicate );

		// Retrieving query.
		List<Tool> tools = em.createQuery( query ).getResultList( );
		Assert.assertEquals( 4, tools.size() );

		em.getTransaction().commit();
		em.close();
	}
}
