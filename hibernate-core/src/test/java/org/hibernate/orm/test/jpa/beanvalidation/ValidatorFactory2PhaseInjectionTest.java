/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.beanvalidation;

import java.net.URL;

import jakarta.persistence.EntityManagerFactory;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.boot.spi.EntityManagerFactoryBuilder;

import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.util.ServiceRegistryUtil;

import org.hibernate.orm.test.jpa.xml.versions.JpaXsdVersionsTest;


import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Test injection of ValidatorFactory using WF/Hibernate 2-phase boot process
 *
 * @author Steve Ebersole
 */

@BaseUnitTest
public class ValidatorFactory2PhaseInjectionTest {
	private ValidatorFactory vf;

	@BeforeEach
	public void before() {
		vf = Validation.byDefaultProvider().configure().buildValidatorFactory();
	}

	@AfterEach
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
				ServiceRegistryUtil.createBaseSettings()
		);
		emfb.withValidatorFactory( vf );

		EntityManagerFactory emf = emfb.build();
		try {
			assertSame( vf, emf.getProperties().get( AvailableSettings.JPA_VALIDATION_FACTORY ) );
		}
		finally {
			emf.close();
		}
	}
}
