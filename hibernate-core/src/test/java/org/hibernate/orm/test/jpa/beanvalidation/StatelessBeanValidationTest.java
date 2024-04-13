/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.beanvalidation;

import jakarta.validation.ConstraintViolationException;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Emmanuel Bernard
 */
@SessionFactory
@DomainModel(annotatedClasses = CupHolder.class)
@ServiceRegistry(settings = @Setting(name = AvailableSettings.JAKARTA_VALIDATION_MODE, value = "auto"))
public class StatelessBeanValidationTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope){
		scope.inTransaction(
				session -> session.createQuery( "delete from CupHolder" ).executeUpdate()
		);
	}

	@Test
	public void testStatelessBeanValidationIntegrationOnInsert(SessionFactoryScope scope) {
		scope.inStatelessTransaction(
				entityManager -> {
					CupHolder ch = new CupHolder();
					ch.setRadius( new BigDecimal( "12" ) );
					ch.setTitle( "foo" );
					try {
						entityManager.insert(ch);
						fail( "invalid object should not be persisted" );
					}
					catch (ConstraintViolationException e) {
						assertEquals( 1, e.getConstraintViolations().size() );
					}
					assertFalse(
							entityManager.getTransaction().getRollbackOnly(),
							"Stateless session errors don't need to mark the transaction for rollback"
					);
				}
		);
	}

	@Test
	public void testStatelessBeanValidationIntegrationOnUpdate(SessionFactoryScope scope) {
		scope.inStatelessTransaction(
				entityManager -> {
					CupHolder ch = new CupHolder();
					ch.setId(123);
					ch.setRadius( new BigDecimal( "12" ) );
					ch.setTitle( "foo" );
					try {
						entityManager.update(ch);
						fail( "invalid object should not be persisted" );
					}
					catch (ConstraintViolationException e) {
						assertEquals( 1, e.getConstraintViolations().size() );
					}
					assertFalse(
							entityManager.getTransaction().getRollbackOnly(),
							"Stateless session errors don't need to mark the transaction for rollback"
					);
				}
		);
	}

	@Test
	public void testStatelessBeanValidationIntegrationOnUpsert(SessionFactoryScope scope) {
		scope.inStatelessTransaction(
				entityManager -> {
					CupHolder ch = new CupHolder();
					ch.setId(123);
					ch.setRadius( new BigDecimal( "12" ) );
					ch.setTitle( "foo" );
					try {
						entityManager.upsert(ch);
						fail( "invalid object should not be persisted" );
					}
					catch (ConstraintViolationException e) {
						assertEquals( 1, e.getConstraintViolations().size() );
					}
					assertFalse(
							entityManager.getTransaction().getRollbackOnly(),
							"Stateless session errors don't need to mark the transaction for rollback"
					);
				}
		);
	}
}
