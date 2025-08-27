/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.beanvalidation;

import jakarta.validation.ConstraintViolationException;
import org.hibernate.cfg.ValidationSettings;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Emmanuel Bernard
 */
@ServiceRegistry(
		settings = @Setting(name = ValidationSettings.JAKARTA_VALIDATION_MODE, value = "AUTO")
)
@DomainModel(annotatedClasses = CupHolder.class)
@SessionFactory
class BeanValidationAutoTest {
	@Test
	void testListeners(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			CupHolder ch = new CupHolder();
			ch.setRadius( new BigDecimal( "12" ) );
			try {
				s.persist( ch );
				s.flush();
				fail( "invalid object should not be persisted" );
			}
			catch (ConstraintViolationException e) {
				assertThat( e.getConstraintViolations() ).hasSize( 1 );
			}
		} );
	}
}
