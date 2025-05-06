/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.proxy;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.MappingException;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.service.ServiceRegistry;

import org.hibernate.testing.ServiceRegistryBuilder;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 *
 * @author Christian Beikov
 */
@JiraKey("HHH-14460")
@BytecodeEnhanced
public class MissingSetterWithEnhancementTest {
	private ServiceRegistry serviceRegistry;

	@BeforeEach
	public void setUp() {
		final BootstrapServiceRegistryBuilder builder = new BootstrapServiceRegistryBuilder();
		builder.applyClassLoader( getClass().getClassLoader() );
		serviceRegistry = ServiceRegistryUtil.serviceRegistryBuilder( builder.build() )
				.applySettings( Environment.getProperties() )
				.build();
	}

	@AfterEach
	public void tearDown() {
		if ( serviceRegistry != null ) {
			ServiceRegistryBuilder.destroy( serviceRegistry );
		}
	}

	@Test
	public void testEnhancedClassMissesSetterForProperty() {
		Configuration cfg = new Configuration();
		cfg.addAnnotatedClass( EntityWithMissingSetter.class );
		try (SessionFactory sf = cfg.buildSessionFactory( serviceRegistry )) {
			fail( "Setter is missing for `name`. SessionFactory creation should fail." );
		}
		catch (MappingException e) {
			assertEquals(
					"Could not locate setter method for property 'name' of class '"
							+ EntityWithMissingSetter.class.getName() + "'",
					e.getMessage()
			);
		}
	}

	@Entity
	@Access(AccessType.PROPERTY)
	public static class EntityWithMissingSetter {
		private Long id;
		@Column
		@Access(AccessType.FIELD)
		private int someInt;

		@Id
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return null;
		}

	}
}
