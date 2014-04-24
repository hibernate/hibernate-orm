/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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

import org.hibernate.jpa.test.metamodel.AbstractMetamodelSpecificTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import junit.framework.Assert;

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
		List<String> divisions = new ArrayList<String>( );
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
		List<String> types = new ArrayList<String>( );
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
