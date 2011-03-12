//$Id$
package org.hibernate.ejb.test.emops;
import static org.hibernate.testing.TestLogger.LOG;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.OptimisticLockException;
import org.hibernate.ejb.test.TestCase;

/**
 * @author Emmanuel Bernard
 */
public class RemoveTest extends TestCase {

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

	public void testRemoveAndFind() {
		Race race = new Race();
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.persist( race );
		em.remove( race );
		assertNull( em.find( Race.class, race.id ) );
		em.getTransaction().rollback();
		em.close();
	}

	public void testUpdatedAndRemove() throws Exception {
		Music music = new Music();
		music.setName( "Classical" );
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.persist( music );
		em.getTransaction().commit();
		em.clear();


		EntityManager em2 = factory.createEntityManager();
		try {
			em2.getTransaction().begin();
			//read music from 2nd EM
			music = em2.find( Music.class, music.getId() );
		} catch (Exception e) {
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
			fail("should have an optimistic lock exception");
		}

        catch( OptimisticLockException e ) {
            LOG.debug("success");
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
    public Map getConfig() {
		Map cfg =  super.getConfig();
		cfg.put( "hibernate.jdbc.batch_size", "0");
		return cfg;
	}
}
