/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.beanvalidation;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.RollbackException;
import jakarta.validation.ConstraintViolationException;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Emmanuel Bernard
 */
@Jpa(
		annotatedClasses = { CupHolder.class },
		integrationSettings = { @Setting(name = AvailableSettings.JAKARTA_VALIDATION_MODE, value = "auto") }
)
public class BeanValidationTest {

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope){
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	public void testBeanValidationIntegrationOnFlush(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					CupHolder ch = new CupHolder();
					ch.setRadius( new BigDecimal( "12" ) );
					ch.setTitle( "foo" );
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
				}
		);
	}

	@Test
	public void testBeanValidationIntegrationOnCommit(EntityManagerFactoryScope scope) {
		try {
			scope.inTransaction(
					entityManager -> {
						CupHolder ch = new CupHolder();
						ch.setRadius( new BigDecimal( "9" ) );
						ch.setTitle( "foo" );
						entityManager.persist( ch );
						entityManager.flush();

						ch.setRadius( new BigDecimal( "12" ) );
					}
			);
			fail( "invalid object should not be persisted" );
		}
		catch (RollbackException e) {
			final Throwable cve = e.getCause();
			assertTrue( cve instanceof ConstraintViolationException );
			assertEquals( 1, ( (ConstraintViolationException) cve ).getConstraintViolations().size() );
		}
	}

	@Test
	@RequiresDialect(H2Dialect.class)
	public void testTitleColumnHasExpectedLength(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Number len = (Number) entityManager.createNativeQuery(
							"select CHARACTER_MAXIMUM_LENGTH from INFORMATION_SCHEMA.COLUMNS c where c.TABLE_NAME = 'CUPHOLDER' and c.COLUMN_NAME = 'TITLE'"
					).getSingleResult();
					assertEquals(64, len.intValue());
				}
		);
	}
}
