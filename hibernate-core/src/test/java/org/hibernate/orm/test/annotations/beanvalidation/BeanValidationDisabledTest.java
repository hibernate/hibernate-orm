/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.beanvalidation;

import jakarta.validation.ConstraintViolationException;
import org.hibernate.cfg.ValidationSettings;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Emmanuel Bernard
 */
@ServiceRegistry(
		settings = @Setting(name = ValidationSettings.JAKARTA_VALIDATION_MODE, value = "none")
)
@DomainModel(annotatedClasses = {
		Address.class,
		CupHolder.class
})
@SessionFactory
class BeanValidationDisabledTest {
	@Test
	void testListeners(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			CupHolder ch = new CupHolder();
			ch.setRadius( new BigDecimal( "12" ) );
			try {
				s.persist( ch );
				s.flush();
			}
			catch (ConstraintViolationException e) {
				fail( "invalid object should not be validated" );
			}
		} );
	}

	@Test
	void testDDLDisabled(SessionFactoryScope scope) {
		PersistentClass classMapping = scope.getMetadataImplementor().getEntityBinding( Address.class.getName() );
		Column countryColumn = (Column) classMapping.getProperty( "country" ).getSelectables().get( 0 );
		assertTrue( countryColumn.isNullable(), "DDL constraints are applied" );
	}
}
