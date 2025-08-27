/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.beanvalidation;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.MessageInterpolator;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.ValidationSettings;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Emmanuel Bernard
 */
@ServiceRegistry(
		settings = @Setting(name = ValidationSettings.JAKARTA_VALIDATION_MODE, value = "auto"),
		settingProviders = @SettingProvider(settingName = ValidationSettings.JAKARTA_VALIDATION_FACTORY,
				provider = BeanValidationProvidedFactoryTest.ValidatorFactoryProvider.class)
)
@DomainModel(annotatedClasses = {
		CupHolder.class
})
@SessionFactory
class BeanValidationProvidedFactoryTest {
	@Test
	void testListeners(SessionFactoryScope scope) {
		scope.inSession( s -> {
			CupHolder ch = new CupHolder();
			ch.setRadius( new BigDecimal( "12" ) );
			Transaction tx = s.beginTransaction();
			try {
				s.persist( ch );
				s.flush();
				fail( "invalid object should not be persisted" );
			}
			catch (ConstraintViolationException e) {
				assertThat( e.getConstraintViolations() ).hasSize( 1 );
				assertThat( e.getConstraintViolations().iterator().next().getMessage() ).isEqualTo( "Oops" );
			}
			tx.rollback();
		} );
	}

	public static class ValidatorFactoryProvider implements SettingProvider.Provider<ValidatorFactory> {
		@Override
		public ValidatorFactory getSetting() {
			final MessageInterpolator messageInterpolator = new MessageInterpolator() {

				public String interpolate(String s, Context context) {
					return "Oops";
				}

				public String interpolate(String s, Context context, Locale locale) {
					return interpolate( s, context );
				}
			};
			final jakarta.validation.Configuration<?> configuration = Validation.byDefaultProvider().configure();
			configuration.messageInterpolator( messageInterpolator );
			return configuration.buildValidatorFactory();
		}
	}
}
