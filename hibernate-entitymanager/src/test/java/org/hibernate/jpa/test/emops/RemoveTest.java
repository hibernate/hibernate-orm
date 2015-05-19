/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.emops;

import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.OptimisticLockException;

import org.jboss.logging.Logger;
import org.junit.Assert;
import org.junit.Test;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

/**
 * @author Emmanuel Bernard
 */
public class RemoveTest extends BaseEntityManagerFunctionalTestCase {
	private static final Logger log = Logger.getLogger( RemoveTest.class );

	@Test
	public void testRemove() {
		Race race = new Race();
		race.competitors.add( new Competitor() );
		race.competitors.add( new Competitor() );
		race.competitors.add( new Competitor() );
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.persist( race );
		em.flush();
		em.remove( race );
		em.flush();
		em.getTransaction().rollback();
		em.close();
	}

	@Test
	public void testRemoveAndFind() {
		Race race = new Race();
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.persist( race );
		em.remove( race );
		Assert.assertNull( em.find( Race.class, race.id ) );
		em.getTransaction().rollback();
		em.close();
	}

	@Test
	public void testUpdatedAndRemove() throws Exception {
		Music music = new Music();
		music.setName( "Classical" );
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.persist( music );
		em.getTransaction().commit();
		em.clear();


		EntityManager em2 = entityManagerFactory().createEntityManager();
		try {
			em2.getTransaction().begin();
			//read music from 2nd EM
			music = em2.find( Music.class, music.getId() );
		}
		catch (Exception e) {
			em2.getTransaction().rollback();
			em2.close();
			throw e;
		}

		//change music
        em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.find( Music.class, music.getId() ).setName( "Rap" );
		em.getTransaction().commit();

		try {
			em2.remove( music ); //remove changed music
			em2.flush();
			Assert.fail( "should have an optimistic lock exception" );
		}
        catch( OptimisticLockException e ) {
            log.debug("success");
		}
		finally {
			em2.getTransaction().rollback();
			em2.close();
		}

		//clean
        em.getTransaction().begin();
		em.remove( em.find( Music.class, music.getId() ) );
	    em.getTransaction().commit();
		em.close();
	}

	@Override
    public Class[] getAnnotatedClasses() {
		return new Class[] {
				Race.class,
				Competitor.class,
				Music.class
		};
	}


	@Override
	@SuppressWarnings( {"unchecked"})
	protected void addConfigOptions(Map options) {
		options.put( "hibernate.jdbc.batch_size", "0" );
	}
}
