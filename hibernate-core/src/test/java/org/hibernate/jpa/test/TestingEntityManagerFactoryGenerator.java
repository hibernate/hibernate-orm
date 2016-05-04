/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test;

import javax.persistence.EntityManagerFactory;
import java.util.Map;

import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;

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
