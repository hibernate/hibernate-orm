/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations;

import org.hibernate.cfg.Configuration;

import org.hibernate.testing.ServiceRegistryBuilder;
import org.hibernate.testing.orm.junit.Jira;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Marco Belladelli
 */
@Jira( "https://hibernate.atlassian.net/browse/HHH-18172" )
public class InvalidAnnotatedClassTypesTest {
	@Test
	public void testEntityAndEmbeddable() {
		try {
			Configuration config = new Configuration();
			config.addAnnotatedClass( EntityAndEmbeddable.class );
			config.buildSessionFactory( ServiceRegistryBuilder.buildServiceRegistry() );
			fail( "Classes annotated with multiple types should not be allowed" );
		}
		catch (Exception e) {
			assertThat( e ).isNotNull();
		}
	}

	@Test
	public void testMappedSuperclassAndEntity() {
		try {
			Configuration config = new Configuration();
			config.addAnnotatedClass( MappedSuperclassAndEntity.class );
			config.addAnnotatedClass( ExtendingEntity.class );
			config.buildSessionFactory( ServiceRegistryBuilder.buildServiceRegistry() );
			fail( "Classes annotated with multiple types should not be allowed" );
		}
		catch (Exception e) {
			assertThat( e ).isNotNull();
		}
	}

	@Test
	public void testEmbeddableAndMappedSuperclass() {
		try {
			Configuration config = new Configuration();
			config.addAnnotatedClass( EmbeddableAndMappedSuperclass.class );
			config.addAnnotatedClass( ExtendingEmbeddable.class );
			config.buildSessionFactory( ServiceRegistryBuilder.buildServiceRegistry() );
			fail( "Classes annotated with multiple types should not be allowed" );
		}
		catch (Exception e) {
			assertThat( e ).isNotNull();
		}
	}

	@Entity
	@Embeddable
	static class EntityAndEmbeddable {
		@Id
		private Long id;
	}

	@MappedSuperclass
	@Entity
	static class MappedSuperclassAndEntity {
		@Id
		private Long id;
	}

	static class ExtendingEntity extends MappedSuperclassAndEntity {
	}

	@Embeddable
	@MappedSuperclass
	static class EmbeddableAndMappedSuperclass {
	}

	static class ExtendingEmbeddable extends EmbeddableAndMappedSuperclass {}
}
