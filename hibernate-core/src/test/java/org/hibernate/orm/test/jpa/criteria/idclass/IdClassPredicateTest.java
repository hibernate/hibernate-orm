/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria.idclass;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Erich Heard
 */
@Jpa(annotatedClasses = {Widget.class, Tool.class})
public class IdClassPredicateTest {

	@BeforeEach
	public void prepareTestData(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {

			Widget w = new Widget();
			w.setCode( "AAA" );
			w.setDivision( "NA" );
			w.setCost( 10.00 );
			entityManager.persist( w );

			w = new Widget();
			w.setCode( "AAA" );
			w.setDivision( "EU" );
			w.setCost( 12.50 );
			entityManager.persist( w );

			w = new Widget();
			w.setCode( "AAA" );
			w.setDivision( "ASIA" );
			w.setCost( 110.00 );
			entityManager.persist( w );

			w = new Widget();
			w.setCode( "BBB" );
			w.setDivision( "NA" );
			w.setCost( 14.00 );
			entityManager.persist( w );

			w = new Widget();
			w.setCode( "BBB" );
			w.setDivision( "EU" );
			w.setCost( 8.75 );
			entityManager.persist( w );

			w = new Widget();
			w.setCode( "BBB" );
			w.setDivision( "ASIA" );
			w.setCost( 86.22 );
			entityManager.persist( w );

			Tool t = new Tool();
			t.setName( "AAA" );
			t.setType( "NA" );
			t.setCost( 10.00 );
			entityManager.persist( t );

			t = new Tool();
			t.setName( "AAA" );
			t.setType( "EU" );
			t.setCost( 12.50 );
			entityManager.persist( t );

			t = new Tool();
			t.setName( "AAA" );
			t.setType( "ASIA" );
			t.setCost( 110.00 );
			entityManager.persist( t );

			t = new Tool();
			t.setName( "BBB" );
			t.setType( "NA" );
			t.setCost( 14.00 );
			entityManager.persist( t );

			t = new Tool();
			t.setName( "BBB" );
			t.setType( "EU" );
			t.setCost( 8.75 );
			entityManager.persist( t );

			t = new Tool();
			t.setName( "BBB" );
			t.setType( "ASIA" );
			t.setCost( 86.22 );
			entityManager.persist( t );

		} );
	}

	@AfterEach
	public void cleanupTestData(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	public void testCountIdClassAttributes(EntityManagerFactoryScope scope){
		scope.inTransaction( entityManager -> {
			CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			CriteriaQuery<Long> cq = cb.createQuery( Long.class );
			Root<Widget> path = cq.from( Widget.class );
			Expression<Long> countSelection = cb.count( path );
			cq.select( countSelection );
			entityManager.createQuery( cq ).getSingleResult();
		} );
	}


	@Test
	public void testDeclaredIdClassAttributes(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {

			// Packaging arguments for use in query.
			List<String> divisions = new ArrayList<>();
			divisions.add( "NA" );
			divisions.add( "EU" );

			// Building the query.
			CriteriaBuilder criteria = entityManager.getCriteriaBuilder();
			CriteriaQuery<Widget> query = criteria.createQuery( Widget.class );
			Root<Widget> root = query.from( Widget.class );

			Predicate predicate = root.get( "division" ).in( divisions );
			query.where( predicate );

			// Retrieving query.;
			List<Widget> widgets = entityManager.createQuery( query ).getResultList();
			Assertions.assertEquals( 4, widgets.size() );

		} );
	}

	@Test
	public void testSupertypeIdClassAttributes(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {

			// Packaging arguments for use in query.
			List<String> types = new ArrayList<>();
			types.add( "NA" );
			types.add( "EU" );

			// Building the query.
			CriteriaBuilder criteria = entityManager.getCriteriaBuilder();
			CriteriaQuery<Tool> query = criteria.createQuery( Tool.class );
			Root<Tool> root = query.from( Tool.class );

			Predicate predicate = root.get( "type" ).in( types );
			query.where( predicate );

			// Retrieving query.
			List<Tool> tools = entityManager.createQuery( query ).getResultList();
			Assertions.assertEquals( 4, tools.size() );

		} );
	}
}
