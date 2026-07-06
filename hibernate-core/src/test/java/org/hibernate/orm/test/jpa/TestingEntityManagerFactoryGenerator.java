/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa;

import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import java.util.Map;

import org.hibernate.boot.pipeline.internal.BootstrapPipeline;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;

import org.hibernate.testing.orm.jpa.PersistenceUnitDescriptorAdapter;

/**
 * @author Steve Ebersole
 */
public class TestingEntityManagerFactoryGenerator {
	public static EntityManagerFactory generateEntityManagerFactory(Object... settings) {
		return generateEntityManagerFactory( SettingsGenerator.generateSettings( settings ) );
	}

	public static EntityManagerFactory generateEntityManagerFactory(Map settings) {
		return generateEntityManagerFactory( new PersistenceUnitDescriptorAdapter(), settings );
	}

	public static EntityManagerFactory generateEntityManagerFactoryForClasses(List<Class<?>> classes, Object... settings) {
		return generateEntityManagerFactory(
				new PersistenceUnitDescriptorAdapter() {
					@Override
					public List<String> getManagedClassNames() {
						return classes.stream().map( Class::getName ).toList();
					}
				},
				SettingsGenerator.generateSettings( settings )
		);
	}

	public static EntityManagerFactory generateEntityManagerFactory(PersistenceUnitDescriptor descriptor, Object... settings) {
		return generateEntityManagerFactory( descriptor, SettingsGenerator.generateSettings( settings ) );
	}

	public static EntityManagerFactory generateEntityManagerFactory(PersistenceUnitDescriptor descriptor, Map<String,Object> settings) {
		return BootstrapPipeline.build( descriptor, settings );
	}
}
