/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.beanvalidation;

import java.nio.file.Path;
import org.junit.jupiter.api.io.TempDir;
import org.hibernate.boot.pipeline.internal.source.PersistenceUnitSources;
import java.net.URL;
import java.util.Map;

import jakarta.persistence.EntityManagerFactory;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;

import org.hibernate.boot.pipeline.internal.BootstrapPipeline;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.boot.internal.PersistenceUnitInfoDescriptor;

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
	public void testInjectionAvailabilityFromEmf(@TempDir Path directory) throws Exception {
		final var root = directory.toUri().toURL();
		final Map<String,Object> settings = ServiceRegistryUtil.createBaseSettings();
		settings.put( AvailableSettings.JPA_VALIDATION_FACTORY, vf );

		EntityManagerFactory emf = BootstrapPipeline.build(
				PersistenceUnitSources.container( new PersistenceUnitInfoDescriptor(
						new JpaXsdVersionsTest.PersistenceUnitInfoImpl( "my-test" ) {
							@Override
							public URL getPersistenceUnitRootUrl() {
								return root;
							}
						}
				) ),
				settings
		);
		try {
			assertSame( vf, emf.getProperties().get( AvailableSettings.JPA_VALIDATION_FACTORY ) );
		}
		finally {
			emf.close();
		}
	}
}
