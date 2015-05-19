/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.emops;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityManager;

import org.junit.Test;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Emmanuel Bernard
 */
public class MergeTest extends BaseEntityManagerFunctionalTestCase {
	@Test
	public void testMergeWithIndexColumn() {
		Race race = new Race();
		race.competitors.add( new Competitor( "Name" ) );
		race.competitors.add( new Competitor() );
		race.competitors.add( new Competitor() );
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.persist( race );
		em.flush();
		em.clear();
		race.competitors.add( new Competitor() );
		race.competitors.remove( 2 );
		race.competitors.remove( 1 );
		race.competitors.get( 0 ).setName( "Name2" );
		race = em.merge( race );
		em.flush();
		em.clear();
		race = em.find( Race.class, race.id );
		assertEquals( 2, race.competitors.size() );
		assertEquals( "Name2", race.competitors.get( 0 ).getName() );
		em.getTransaction().rollback();
		em.close();
	}

	@Test
	public void testMergeManyToMany() {
		Competition competition = new Competition();
		competition.getCompetitors().add( new Competitor( "Name" ) );
		competition.getCompetitors().add( new Competitor() );
		competition.getCompetitors().add( new Competitor() );
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.persist( competition );
		em.flush();
		em.clear();
		competition.getCompetitors().add( new Competitor() );
		competition.getCompetitors().remove( 2 );
		competition.getCompetitors().remove( 1 );
		competition.getCompetitors().get( 0 ).setName( "Name2" );
		competition = em.merge( competition );
		em.flush();
		em.clear();
		competition = em.find( Competition.class, competition.getId() );
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
		em.getTransaction().rollback();
		em.close();
	}

	@Test
	public void testMergeManyToManyWithDeference() {
		Competition competition = new Competition();
		competition.getCompetitors().add( new Competitor( "Name" ) );
		competition.getCompetitors().add( new Competitor() );
		competition.getCompetitors().add( new Competitor() );
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.persist( competition );
		em.flush();
		em.clear();
		List<Competitor> newComp = new ArrayList<Competitor>();
		newComp.add( competition.getCompetitors().get( 0 ) );
		newComp.add( new Competitor() );
		newComp.get( 0 ).setName( "Name2" );
		competition.setCompetitors( newComp );
		competition = em.merge( competition );
		em.flush();
		em.clear();
		competition = em.find( Competition.class, competition.getId() );
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
		em.getTransaction().rollback();
		em.close();
	}

	@Test
	public void testRemoveAndMerge() {
		Race race = new Race();
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.persist( race );
		em.flush();
		em.clear();
		race = em.find( Race.class, race.id );
		em.remove( race );
		try {
			race = em.merge( race );
			em.flush();
			fail( "Should raise an IllegalArgumentException" );
		}
		catch ( IllegalArgumentException e ) {
			//all good
		}
		catch ( Exception e ) {
			fail( "Should raise an IllegalArgumentException" );
		}
		em.getTransaction().rollback();
		em.close();
	}

	@Test
	public void testConcurrentMerge() {
		Race race = new Race();
		race.name = "Derby";
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.persist( race );
		em.flush();
		em.getTransaction().commit();
		em.close();

		race.name = "Magnicourt";

		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		Race race2 = em.find( Race.class, race.id );
		race2.name = "Mans";

		race = em.merge( race );
		em.flush();
		em.getTransaction().commit();
		em.close();

		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		race2 = em.find( Race.class, race.id );
		assertEquals( "Last commit win in merge", "Magnicourt", race2.name );

		em.remove( race2 );
		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void testMergeUnidirectionalOneToMany() throws Exception {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		Empire roman = new Empire();
		em.persist( roman );
		em.flush();
		em.clear();
		roman = em.find( Empire.class, roman.getId() );
		Colony gaule = new Colony();
		roman.getColonies().add( gaule );
		em.merge( roman );
		em.flush();
		em.clear();
		roman = em.find( Empire.class, roman.getId() );
		assertEquals( 1, roman.getColonies().size() );
		em.getTransaction().rollback();
		em.close();
	}

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] {
				Race.class,
				Competitor.class,
				Competition.class,
				Empire.class,
				Colony.class
		};
	}
}
