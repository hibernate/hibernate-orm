/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa;

import jakarta.persistence.EntityManagerFactory;
import java.util.Map;

import org.hibernate.jpa.boot.spi.Bootstrap;
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

	public static EntityManagerFactory generateEntityManagerFactory(PersistenceUnitDescriptor descriptor, Object... settings) {
		return generateEntityManagerFactory( descriptor, SettingsGenerator.generateSettings( settings ) );
	}

	public static EntityManagerFactory generateEntityManagerFactory(PersistenceUnitDescriptor descriptor, Map settings) {
		return Bootstrap.getEntityManagerFactoryBuilder( descriptor, settings ).build();
	}
}
