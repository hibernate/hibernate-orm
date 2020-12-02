/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.beanvalidation;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import javax.persistence.RollbackException;
import javax.validation.ConstraintViolationException;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Emmanuel Bernard
 */
@Jpa(
		annotatedClasses = { CupHolder.class },
		integrationSettings = { @Setting(name = AvailableSettings.JPA_VALIDATION_MODE, value = "auto") }
)
public class BeanValidationTest {

	@Test
	public void testBeanValidationIntegrationOnFlush(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					CupHolder ch = new CupHolder();
					ch.setRadius( new BigDecimal( "12" ) );
					ch.setTitle( "foo" );
					entityManager.getTransaction().begin();
					try {
						entityManager.persist(ch);
						entityManager.flush();
						fail( "invalid object should not be persisted" );
					}
					catch (ConstraintViolationException e) {
						assertEquals( 1, e.getConstraintViolations().size() );
					}
					assertTrue(
							entityManager.getTransaction().getRollbackOnly(),
							"A constraint violation exception should mark the transaction for rollback"
					);
					entityManager.getTransaction().rollback();
					entityManager.clear();
				}
		);
	}

	@Test
	public void testBeanValidationIntegrationOnCommit(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					CupHolder ch = new CupHolder();
					ch.setRadius(new BigDecimal("9"));
					ch.setTitle("foo");
					entityManager.getTransaction().begin();
					entityManager.persist(ch);
					entityManager.flush();
					try {
						ch.setRadius(new BigDecimal("12"));
						entityManager.getTransaction().commit();
						fail("invalid object should not be persisted");
					}
					catch (RollbackException e) {
						final Throwable cve = e.getCause();
						assertTrue(cve instanceof ConstraintViolationException);
						assertEquals(1, ((ConstraintViolationException) cve).getConstraintViolations().size());
					}
					entityManager.close();
				}
		);
	}

	@Test
	@RequiresDialect(H2Dialect.class)
	public void testTitleColumnHasExpectedLength(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					int len = (Integer) entityManager.createNativeQuery(
							"select CHARACTER_MAXIMUM_LENGTH from INFORMATION_SCHEMA.COLUMNS c where c.TABLE_NAME = 'CUPHOLDER' and c.COLUMN_NAME = 'TITLE'"
					).getSingleResult();
					assertEquals(64, len);
				}
		);
	}
}
