/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.beanvalidation;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.NotNull;
import org.hibernate.Transaction;
import org.hibernate.boot.beanvalidation.BeanValidationIntegrator;
import org.hibernate.cfg.ValidationSettings;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Emmanuel Bernard
 */
@ServiceRegistry(settings = {
		@Setting(name = ValidationSettings.JAKARTA_PERSIST_VALIDATION_GROUP, value = ""),
		@Setting(name = ValidationSettings.JAKARTA_UPDATE_VALIDATION_GROUP, value = ""),
		@Setting(name = ValidationSettings.JAKARTA_REMOVE_VALIDATION_GROUP,
				value = "jakarta.validation.groups.Default, org.hibernate.orm.test.annotations.beanvalidation.Strict"),
		@Setting(name = BeanValidationIntegrator.APPLY_CONSTRAINTS, value = "false"),
		@Setting(name = ValidationSettings.JAKARTA_VALIDATION_MODE, value = "auto"),
})
@DomainModel(annotatedClasses = {
		CupHolder.class
})
@SessionFactory
class BeanValidationGroupsTest {

	@Test
	void testListeners(SessionFactoryScope scope) {
		scope.inSession( s -> {
			CupHolder ch = new CupHolder();
			ch.setRadius( new BigDecimal( "12" ) );
			Transaction tx = s.beginTransaction();
			try {
				s.persist( ch );
				s.flush();
			}
			catch (ConstraintViolationException e) {
				fail( "invalid object should not be validated" );
			}
			try {
				ch.setRadius( null );
				s.flush();
			}
			catch (ConstraintViolationException e) {
				fail( "invalid object should not be validated" );
			}
			try {
				s.remove( ch );
				s.flush();
				fail( "invalid object should not be persisted" );
			}
			catch (ConstraintViolationException e) {
				assertThat( e.getConstraintViolations() ).hasSize( 1 );
				// TODO - seems this explicit case is necessary with JDK 5 (at least on Mac). With Java 6 there is no problem
				Annotation annotation = e.getConstraintViolations()
						.iterator()
						.next()
						.getConstraintDescriptor()
						.getAnnotation();
				assertThat( annotation.annotationType() ).isEqualTo( NotNull.class );
			}
			tx.rollback();
		} );
	}
}
