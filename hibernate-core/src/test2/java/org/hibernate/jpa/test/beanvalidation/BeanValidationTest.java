/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.beanvalidation;

import java.math.BigDecimal;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.RollbackException;
import javax.validation.ConstraintViolationException;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.beanvalidation.ValidationMode;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.RequiresDialect;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Emmanuel Bernard
 */
public class BeanValidationTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected void addMappings(Map settings) {
		settings.put(
				AvailableSettings.JPA_VALIDATION_MODE,
				ValidationMode.AUTO
		);
	}

	@Test
	public void testBeanValidationIntegrationOnFlush() {
		CupHolder ch = new CupHolder();
		ch.setRadius( new BigDecimal( "12" ) );
		ch.setTitle( "foo" );
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		try {
			em.persist( ch );
			em.flush();
			fail( "invalid object should not be persisted" );
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
		ch.setTitle( "foo" );
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.persist( ch );
		em.flush();
		try {
			ch.setRadius( new BigDecimal( "12" ) );
			em.getTransaction().commit();
			fail( "invalid object should not be persisted" );
		}
		catch ( RollbackException e ) {
			final Throwable cve = e.getCause();
			assertTrue( cve instanceof ConstraintViolationException );
			assertEquals( 1, ( (ConstraintViolationException) cve ).getConstraintViolations().size() );
		}
		em.close();
	}

	@Test
	@RequiresDialect(H2Dialect.class)
	public void testTitleColumnHasExpectedLength() {
		EntityManager em = getOrCreateEntityManager();
		int len = (Integer) em.createNativeQuery(
				"select CHARACTER_MAXIMUM_LENGTH from INFORMATION_SCHEMA.COLUMNS c where c.TABLE_NAME = 'CUPHOLDER' and c.COLUMN_NAME = 'TITLE'"
		).getSingleResult();
		assertEquals( 64, len );
	}

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] {
				CupHolder.class
		};
	}

}
