/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.test.components;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import org.hibernate.Hibernate;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author alan.oleary
 */
@Jpa(annotatedClasses = {Client.class, Alias.class})
public class ComponentCriteriaTest {

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	public void testEmbeddableInPath(EntityManagerFactoryScope scope) {
		Client client = new Client( 111, "steve", "ebersole" );
		scope.inTransaction( entityManager -> entityManager.persist( client ) );

		scope.inTransaction( entityManager -> {
			CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			CriteriaQuery<Client> cq = cb.createQuery( Client.class );
			Root<Client> root = cq.from( Client.class );
			cq.where( cb.equal( root.get( "name" ).get( "firstName" ), client.getName().getFirstName() ) );
			List<Client> list = entityManager.createQuery( cq ).getResultList();
			assertEquals( 1, list.size() );
		} );

		// HHH-5792
		scope.inTransaction( entityManager -> {
			TypedQuery<Client> q = entityManager.createQuery(
					"SELECT c FROM Client c JOIN c.name n WHERE n.firstName = '"
							+ client.getName().getFirstName() + "'",
					Client.class
			);
			assertEquals( 1, q.getResultList().size() );
		} );
	}

	@Test
	@JiraKey(value = "HHH-9642")
	public void testOneToManyJoinFetchedInEmbeddable(EntityManagerFactoryScope scope) {
		Client client = new Client( 111, "steve", "ebersole" );
		Alias alias = new Alias( "a", "guy", "work" );
		client.getName().getAliases().add( alias );
		scope.inTransaction( entityManager -> entityManager.persist( client ) );

		List<Client> list = new ArrayList<>();
		scope.inTransaction( entityManager -> {
			CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			CriteriaQuery<Client> cq = cb.createQuery( Client.class );
			Root<Client> root = cq.from( Client.class );
			root.fetch( Client_.name ).fetch( Name_.aliases );
			cq.where( cb.equal( root.get( "name" ).get( "firstName" ), client.getName().getFirstName() ) );
			list.addAll( entityManager.createQuery( cq ).getResultList() );
			assertEquals( 1, list.size() );
			Client c = list.get( 0 );
			assertTrue( Hibernate.isInitialized( c.getName().getAliases() ) );
		} );

		scope.inTransaction( entityManager -> {
			TypedQuery<Client> q = entityManager.createQuery(
					"SELECT c FROM Client c JOIN FETCH c.name.aliases WHERE c.name.firstName = '"
							+ client.getName().getFirstName() + "'",
					Client.class
			);
			assertEquals( 1, q.getResultList().size() );
			Client c = list.get( 0 );
			assertTrue( Hibernate.isInitialized( c.getName().getAliases() ) );
		} );

		scope.inTransaction( entityManager -> {
			TypedQuery<Client> q = entityManager.createQuery(
					"SELECT c FROM Client c JOIN  c.name n join FETCH n.aliases WHERE c.name.firstName = '"
							+ client.getName().getFirstName() + "'",
					Client.class
			);
			assertEquals( 1, q.getResultList().size() );
			Client c = list.get( 0 );
			assertTrue( Hibernate.isInitialized( c.getName().getAliases() ) );
		} );

		scope.inTransaction( entityManager -> {
			Client c = entityManager.merge( client );
			entityManager.remove( c );
		} );
	}

	@Test
	@JiraKey(value = "HHH-4586")
	public void testParameterizedFunctions(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
					CriteriaBuilder cb = entityManager.getCriteriaBuilder();
					// lower
					CriteriaQuery<Client> cq = cb.createQuery( Client.class );
					Root<Client> root = cq.from( Client.class );
					cq.where( cb.equal( cb.lower( root.get( Client_.name ).get( Name_.lastName ) ), "test" ) );
					entityManager.createQuery( cq ).getResultList();
					// upper
					cq = cb.createQuery( Client.class );
					root = cq.from( Client.class );
					cq.where( cb.equal( cb.upper( root.get( Client_.name ).get( Name_.lastName ) ), "test" ) );
					entityManager.createQuery( cq ).getResultList();
				}
		);
	}
}
