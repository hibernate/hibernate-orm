/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.beanvalidation;

import java.math.BigDecimal;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import org.hibernate.cfg.ValidationSettings;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;


/**
 * @author Emmanuel Bernard
 */
@ServiceRegistry(
		settings = @Setting(name = ValidationSettings.JAKARTA_VALIDATION_MODE, value = "AUTO")
)
@DomainModel(annotatedClasses = {
		Button.class,
		Color.class,
		Display.class,
		DisplayConnector.class,
		PowerSupply.class,
		Screen.class
})
@SessionFactory
class HibernateTraversableResolverTest {
	@Test
	void testNonLazyAssocFieldWithConstraintsFailureExpected(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			Screen screen = new Screen();
			screen.setPowerSupply( null );
			try {
				s.persist( screen );
				s.flush();
				fail( "@NotNull on a non lazy association is not evaluated" );
			}
			catch (ConstraintViolationException e) {
				assertThat( e.getConstraintViolations() ).hasSize( 1 );
			}
		} );
	}

	@Test
	void testEmbedded(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			Screen screen = new Screen();
			PowerSupply ps = new PowerSupply();
			screen.setPowerSupply( ps );
			Button button = new Button();
			button.setName( null );
			button.setSize( 3 );
			screen.setStopButton( button );
			try {
				s.persist( screen );
				s.flush();
				fail( "@NotNull on embedded property is not evaluated" );
			}
			catch (ConstraintViolationException e) {
				assertThat( e.getConstraintViolations() ).hasSize( 1 );
				ConstraintViolation<?> cv = e.getConstraintViolations().iterator().next();
				assertThat( cv.getRootBeanClass() ).isEqualTo( Screen.class );
				// toString works since hibernate validator's Path implementation works accordingly. Should do a Path comparison though
				assertThat( cv.getPropertyPath().toString() ).isEqualTo( "stopButton.name" );
			}

		} );
	}

	@Test
	void testToOneAssocNotValidated(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			Screen screen = new Screen();
			PowerSupply ps = new PowerSupply();
			ps.setPosition( "1" );
			ps.setPower( new BigDecimal( 350 ) );
			screen.setPowerSupply( ps );
			try {
				s.persist( screen );
				s.flush();
				fail( "Associated objects should not be validated" );
			}
			catch (ConstraintViolationException e) {
				assertThat( e.getConstraintViolations() ).hasSize( 1 );
				final ConstraintViolation<?> constraintViolation = e.getConstraintViolations().iterator().next();
				assertThat( constraintViolation.getRootBeanClass() ).isEqualTo( PowerSupply.class );
			}
		} );
	}

	@Test
	void testCollectionAssocNotValidated(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			Screen screen = new Screen();
			screen.setStopButton( new Button() );
			screen.getStopButton().setName( "STOOOOOP" );
			PowerSupply ps = new PowerSupply();
			screen.setPowerSupply( ps );
			Color c = new Color();
			c.setName( "Blue" );
			s.persist( c );
			c.setName( null );
			screen.getDisplayColors().add( c );
			try {
				s.persist( screen );
				s.flush();
				fail( "Associated objects should not be validated" );
			}
			catch (ConstraintViolationException e) {
				assertThat( e.getConstraintViolations() ).hasSize( 1 );
				final ConstraintViolation<?> constraintViolation = e.getConstraintViolations().iterator().next();
				assertThat( constraintViolation.getRootBeanClass() ).isEqualTo( Color.class );
			}
		} );
	}

	@Test
	void testEmbeddedCollection(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			Screen screen = new Screen();
			PowerSupply ps = new PowerSupply();
			screen.setPowerSupply( ps );
			DisplayConnector conn = new DisplayConnector();
			conn.setNumber( 0 );
			screen.getConnectors().add( conn );
			try {
				s.persist( screen );
				s.flush();
				fail( "Collection of embedded objects should be validated" );
			}
			catch (ConstraintViolationException e) {
				assertThat( e.getConstraintViolations() ).hasSize( 1 );
				final ConstraintViolation<?> constraintViolation = e.getConstraintViolations().iterator().next();
				assertThat( constraintViolation.getRootBeanClass() ).isEqualTo( Screen.class );
				// toString works since hibernate validator's Path implementation works accordingly. Should do a Path comparison though
				assertThat( constraintViolation.getPropertyPath().toString() ).isEqualTo( "connectors[].number" );
			}
		});
	}

	@Test
	void testAssocInEmbeddedNotValidated(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			Screen screen = new Screen();
			screen.setStopButton( new Button() );
			screen.getStopButton().setName( "STOOOOOP" );
			PowerSupply ps = new PowerSupply();
			screen.setPowerSupply( ps );
			DisplayConnector conn = new DisplayConnector();
			conn.setNumber( 1 );
			screen.getConnectors().add( conn );
			final Display display = new Display();
			display.setBrand( "dell" );
			conn.setDisplay( display );
			s.persist( display );
			s.flush();
			try {
				display.setBrand( null );
				s.persist( screen );
				s.flush();
				fail( "Collection of embedded objects should be validated" );
			}
			catch (ConstraintViolationException e) {
				assertThat( e.getConstraintViolations() ).hasSize( 1 );
				final ConstraintViolation constraintViolation = e.getConstraintViolations().iterator().next();
				assertThat( constraintViolation.getRootBeanClass() ).isEqualTo( Display.class );
			}
		} );
	}
}
