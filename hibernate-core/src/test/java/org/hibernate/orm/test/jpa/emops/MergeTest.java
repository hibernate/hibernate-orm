/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.emops;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Emmanuel Bernard
 */

@Jpa(annotatedClasses = {
		Race.class,
		Competitor.class,
		Competition.class,
		Empire.class,
		Colony.class
})
public class MergeTest {
	@Test
	public void testMergeWithIndexColumn(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					try {
						Race race = new Race();
						race.competitors.add( new Competitor( "Name" ) );
						race.competitors.add( new Competitor() );
						race.competitors.add( new Competitor() );
						entityManager.getTransaction().begin();
						entityManager.persist( race );
						entityManager.flush();
						entityManager.clear();
						race.competitors.add( new Competitor() );
						race.competitors.remove( 2 );
						race.competitors.remove( 1 );
						race.competitors.get( 0 ).setName( "Name2" );
						race = entityManager.merge( race );
						entityManager.flush();
						entityManager.clear();
						race = entityManager.find( Race.class, race.id );
						assertEquals( 2, race.competitors.size() );
						assertEquals( "Name2", race.competitors.get( 0 ).getName() );
						entityManager.getTransaction().rollback();
					}
					catch (Exception e) {
						if ( entityManager.getTransaction().isActive() ) {
							entityManager.getTransaction().rollback();
						}
						throw e;
					}
				}
		);
	}

	@Test
	public void testMergeManyToMany(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					try {
						Competition competition = new Competition();
						competition.getCompetitors().add( new Competitor( "Name" ) );
						competition.getCompetitors().add( new Competitor() );
						competition.getCompetitors().add( new Competitor() );
						entityManager.getTransaction().begin();
						entityManager.persist( competition );
						entityManager.flush();
						entityManager.clear();
						competition.getCompetitors().add( new Competitor() );
						competition.getCompetitors().remove( 2 );
						competition.getCompetitors().remove( 1 );
						competition.getCompetitors().get( 0 ).setName( "Name2" );
						competition = entityManager.merge( competition );
						entityManager.flush();
						entityManager.clear();
						competition = entityManager.find( Competition.class, competition.getId() );
						assertEquals( 2, competition.getCompetitors().size() );
						// we cannot assume that the order in the list is maintained - HHH-4516
						String changedCompetitorName;
						if ( competition.getCompetitors().get( 0 ).getName() != null ) {
							changedCompetitorName = competition.getCompetitors().get( 0 ).getName();
						}
						else {
							changedCompetitorName = competition.getCompetitors().get( 1 ).getName();
						}
						assertEquals( "Name2", changedCompetitorName );
						entityManager.getTransaction().rollback();
					}
					catch (Exception e) {
						if ( entityManager.getTransaction().isActive() ) {
							entityManager.getTransaction().rollback();
						}
						throw e;
					}
				}
		);
	}

	@Test
	public void testMergeManyToManyWithDeference(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					try {
						Competition competition = new Competition();
						competition.getCompetitors().add( new Competitor( "Name" ) );
						competition.getCompetitors().add( new Competitor() );
						competition.getCompetitors().add( new Competitor() );
						entityManager.getTransaction().begin();
						entityManager.persist( competition );
						entityManager.flush();
						entityManager.clear();
						List<Competitor> newComp = new ArrayList<Competitor>();
						newComp.add( competition.getCompetitors().get( 0 ) );
						newComp.add( new Competitor() );
						newComp.get( 0 ).setName( "Name2" );
						competition.setCompetitors( newComp );
						competition = entityManager.merge( competition );
						entityManager.flush();
						entityManager.clear();
						competition = entityManager.find( Competition.class, competition.getId() );
						assertEquals( 2, competition.getCompetitors().size() );
						// we cannot assume that the order in the list is maintained - HHH-4516
						String changedCompetitorName;
						if ( competition.getCompetitors().get( 0 ).getName() != null ) {
							changedCompetitorName = competition.getCompetitors().get( 0 ).getName();
						}
						else {
							changedCompetitorName = competition.getCompetitors().get( 1 ).getName();
						}
						assertEquals( "Name2", changedCompetitorName );
						entityManager.getTransaction().rollback();
					}
					catch (Exception e) {
						if ( entityManager.getTransaction().isActive() ) {
							entityManager.getTransaction().rollback();
						}
						throw e;
					}
				}
		);
	}

	@Test
	public void testRemoveAndMerge(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					try {
						Race race = new Race();
						entityManager.getTransaction().begin();
						entityManager.persist( race );
						entityManager.flush();
						entityManager.clear();
						race = entityManager.find( Race.class, race.id );
						entityManager.remove( race );
						try {
							race = entityManager.merge( race );
							entityManager.flush();
							fail( "Should raise an IllegalArgumentException" );
						}
						catch (IllegalArgumentException e) {
							//all good
						}
						catch (Exception e) {
							fail( "Should raise an IllegalArgumentException" );
						}
						entityManager.getTransaction().rollback();
					}
					catch (Exception e) {
						if ( entityManager.getTransaction().isActive() ) {
							entityManager.getTransaction().rollback();
						}
						throw e;
					}
				}
		);
	}

	@Test
	public void testConcurrentMerge(EntityManagerFactoryScope scope) {
		Race race = new Race();

		scope.inTransaction(
				entityManager -> {
					race.name = "Derby";
					entityManager.persist( race );
					entityManager.flush();
				}
		);

		race.name = "Magnicourt";

		scope.inTransaction(
				entityManager -> {
					Race race2 = entityManager.find( Race.class, race.id );
					race2.name = "Mans";

					entityManager.merge( race );
					entityManager.flush();
				}
		);

		scope.inTransaction(
				entityManager -> {
					Race race2 = entityManager.find( Race.class, race.id );
					assertEquals( "Magnicourt", race2.name, "Last commit win in merge" );

					entityManager.remove( race2 );
				}
		);
	}

	@Test
	public void testMergeUnidirectionalOneToMany(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					try {
						entityManager.getTransaction().begin();
						Empire roman = new Empire();
						entityManager.persist( roman );
						entityManager.flush();
						entityManager.clear();
						roman = entityManager.find( Empire.class, roman.getId() );
						Colony gaule = new Colony();
						roman.getColonies().add( gaule );
						entityManager.merge( roman );
						entityManager.flush();
						entityManager.clear();
						roman = entityManager.find( Empire.class, roman.getId() );
						assertEquals( 1, roman.getColonies().size() );
						entityManager.getTransaction().rollback();
					}
					catch (Exception e) {
						if ( entityManager.getTransaction().isActive() ) {
							entityManager.getTransaction().rollback();
						}
						throw e;
					}
				}
		);
	}
}
