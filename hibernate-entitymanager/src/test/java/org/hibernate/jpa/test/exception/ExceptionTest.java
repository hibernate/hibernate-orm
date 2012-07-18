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
package org.hibernate.jpa.test.exception;

import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.OptimisticLockException;
import javax.persistence.PersistenceException;

import org.jboss.logging.Logger;
import org.junit.Test;

import org.hibernate.cfg.Environment;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.exception.ConstraintViolationException;

import org.hibernate.testing.TestForIssue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Emmanuel Bernard
 */
@SuppressWarnings("unchecked")
public class ExceptionTest extends BaseEntityManagerFunctionalTestCase {
	private static final Logger log = Logger.getLogger( ExceptionTest.class );

	@Test
	public void testOptimisticLockingException() throws Exception {
		EntityManager em = getOrCreateEntityManager();
		EntityManager em2 = entityManagerFactory().createEntityManager();
		em.getTransaction().begin();
		Music music = new Music();
		music.setName( "Old Country" );
		em.persist( music );
		em.getTransaction().commit();

		try {
			em2.getTransaction().begin();
			Music music2 = em2.find( Music.class, music.getId() );
			music2.setName( "HouseMusic" );
			em2.getTransaction().commit();
		}
		catch ( Exception e ) {
			em2.getTransaction().rollback();
			throw e;
		}
		finally {
			em2.close();
		}

		em.getTransaction().begin();
		music.setName( "Rock" );
		try {

			em.flush();
			fail( "Should raise an optimistic lock exception" );
		}
		catch ( OptimisticLockException e ) {
			//success
			assertEquals( music, e.getEntity() );
		}
		catch ( Exception e ) {
			fail( "Should raise an optimistic lock exception" );
		}
		finally {
			em.getTransaction().rollback();
			em.close();
		}
	}

	@Test
	public void testEntityNotFoundException() throws Exception {
		EntityManager em = getOrCreateEntityManager();
		Music music = em.getReference( Music.class, -1 );
		try {
			music.getName();
			fail( "Non existent entity should raise an exception when state is accessed" );
		}
		catch ( EntityNotFoundException e ) {
            log.debug("success");
		}
		finally {
			em.close();
		}
	}

	@Test
	public void testConstraintViolationException() throws Exception {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		Music music = new Music();
		music.setName( "Jazz" );
		em.persist( music );
		Musician lui = new Musician();
		lui.setName( "Lui Armstrong" );
		lui.setFavouriteMusic( music );
		em.persist( lui );
		em.getTransaction().commit();
		try {
			em.getTransaction().begin();
			String hqlDelete = "delete Music where name = :name";
			em.createQuery( hqlDelete ).setParameter( "name", "Jazz" ).executeUpdate();
			em.getTransaction().commit();
			fail();
		}
		catch ( PersistenceException e ) {
			Throwable t = e.getCause();
			assertTrue( "Should be a constraint violation", t instanceof ConstraintViolationException );
			em.getTransaction().rollback();
		}
		finally {
			em.close();
		}
	}

	@Test
	@TestForIssue( jiraKey = "HHH-4676" )
	public void testInterceptor() throws Exception {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		Instrument instrument = new Instrument();
		instrument.setName( "Guitar" );
		try {
			em.persist( instrument );
			fail( "Commit should have failed." );
		}
		catch ( RuntimeException e ) {
			assertTrue( em.getTransaction().getRollbackOnly() );
		}
		em.close();
	}

	@Override
	protected void addConfigOptions(Map options) {
		options.put( Environment.BATCH_VERSIONED_DATA, "false" );
	}

	@Override
    public Class[] getAnnotatedClasses() {
		return new Class[] {
				Music.class, Musician.class, Instrument.class
		};
	}
}
