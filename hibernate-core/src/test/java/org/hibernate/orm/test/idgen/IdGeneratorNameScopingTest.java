/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.idgen;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.TableGenerator;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Tests for scoping of generator names
 *
 * @author Andrea Boriero
 */
@DomainModel( annotatedClasses = { IdGeneratorNameScopingTest.FirstEntity.class, IdGeneratorNameScopingTest.SecondEntity.class } )
@SessionFactory
public class IdGeneratorNameScopingTest {
	//
	@Test
	public void testLocalScoping() {
		// test Hibernate's default behavior which "violates" the spec but is much
		// more sane.
		// this works properly because Hibernate simply prefers the locally defined one
		buildMetadata( false );
	}

	@Test
	public void testGlobalScoping() {
		// now test JPA's strategy of globally scoping identifiers.
		// this will fail because both
		try {
			buildMetadata( true );
			Assertions.fail();
		}
		catch (Exception e) {
			assertThat( e ).isInstanceOf( IllegalArgumentException.class );
			assertThat( e.getMessage() ).startsWith( "Duplicate generator name" );
		}
	}

	public void buildMetadata(boolean jpaCompliantScoping) {
		final StandardServiceRegistry registry = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( AvailableSettings.JPA_ID_GENERATOR_GLOBAL_SCOPE_COMPLIANCE, jpaCompliantScoping )
				.build();

		try {
			// this will fail with global scoping and pass with local scoping
			new MetadataSources( registry )
					.addAnnotatedClass( FirstEntity.class )
					.addAnnotatedClass( SecondEntity.class )
					.buildMetadata();
		}
		finally {
			StandardServiceRegistryBuilder.destroy( registry );
		}
	}

	@Entity(name = "FirstEntity")
	@TableGenerator(
			name = "table-generator",
			table = "table_identifier_2",
			pkColumnName = "identifier",
			valueColumnName = "value",
			allocationSize = 5,
			initialValue = 1
	)
	public static class FirstEntity {
		@Id
		@GeneratedValue(strategy = GenerationType.TABLE, generator = "table-generator")
		private Long id;

		public Long getId() {
			return id;
		}
	}

	@Entity(name = "SecondEntity")
	@TableGenerator(
			name = "table-generator",
			table = "table_identifier",
			pkColumnName = "identifier",
			valueColumnName = "value",
			allocationSize = 5,
			initialValue = 10
	)
	public static class SecondEntity {
		@Id
		@GeneratedValue(strategy = GenerationType.TABLE, generator = "table-generator")
		private Long id;

		public Long getId() {
			return id;
		}
	}
}
