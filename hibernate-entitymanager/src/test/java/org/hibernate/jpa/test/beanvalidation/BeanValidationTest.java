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
package org.hibernate.jpa.test.beanvalidation;

import java.math.BigDecimal;
import javax.persistence.EntityManager;
import javax.persistence.RollbackException;
import javax.validation.ConstraintViolationException;

import org.junit.Test;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Emmanuel Bernard
 */
public class BeanValidationTest extends BaseEntityManagerFunctionalTestCase {
	@Test
	public void testBeanValidationIntegrationOnFlush() {
		CupHolder ch = new CupHolder();
		ch.setRadius( new BigDecimal( "12" ) );
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		try {
			em.persist( ch );
			em.flush();
			fail("invalid object should not be persisted");
		}
		catch ( ConstraintViolationException e ) {
			assertEquals( 1, e.getConstraintViolations().size() );
		}
		assertTrue(
				"A constraint violation exception should mark the transaction for rollback",
				em.getTransaction().getRollbackOnly()
		);
		em.getTransaction().rollback();
		em.close();
	}

	@Test
	public void testBeanValidationIntegrationOnCommit() {
		CupHolder ch = new CupHolder();
		ch.setRadius( new BigDecimal( "9" ) );
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.persist( ch );
		em.flush();
		try {
			ch.setRadius( new BigDecimal( "12" ) );
			em.getTransaction().commit();
			fail("invalid object should not be persisted");
		}
		catch ( RollbackException e ) {
			final Throwable cve = e.getCause();
			assertTrue( cve instanceof ConstraintViolationException );
			assertEquals( 1, ( (ConstraintViolationException) cve ).getConstraintViolations().size() );
		}
		em.close();
	}

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] {
				CupHolder.class
		};
	}
}
