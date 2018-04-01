/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.beanvalidation;

import java.net.URL;
import java.util.Collections;

import javax.persistence.EntityManagerFactory;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;

import org.hibernate.jpa.AvailableSettings;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.boot.spi.EntityManagerFactoryBuilder;
import org.hibernate.test.jpa.xml.versions.JpaXsdVersionsTest;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertSame;

/**
 * Test injection of ValidatorFactory using WF/Hibernate 2-phase boot process
 *
 * @author Steve Ebersole
 */
public class ValidatorFactory2PhaseInjectionTest extends BaseUnitTestCase {
	private ValidatorFactory vf;

	@Before
	public void before() {
		vf = Validation.byDefaultProvider().configure().buildValidatorFactory();
	}

	@After
	public void after() {
		if ( vf != null ) {
			vf.close();
		}
	}

	@Test
	public void testInjectionAvailabilityFromEmf() {
		EntityManagerFactoryBuilder emfb = Bootstrap.getEntityManagerFactoryBuilder(
				new JpaXsdVersionsTest.PersistenceUnitInfoImpl( "my-test" ) {
					@Override
					public URL getPersistenceUnitRootUrl() {
						// just get any known url...
						return HibernatePersistenceProvider.class.getResource( "/org/hibernate/jpa/persistence_1_0.xsd" );
					}
				},
				Collections.emptyMap()
		);
			emfb.withValidatorFactory( vf );

			EntityManagerFactory emf = emfb.build();
		try {
			assertSame( vf, emf.getProperties().get( AvailableSettings.VALIDATION_FACTORY ) );
		}
		finally {
			emf.close();
		}
	}
}
