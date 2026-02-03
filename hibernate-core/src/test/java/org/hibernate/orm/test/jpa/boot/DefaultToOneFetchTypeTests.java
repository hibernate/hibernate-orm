/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.boot;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.boot.spi.EntityManagerFactoryBuilder;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.ToOne;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
public class DefaultToOneFetchTypeTests {
	@Test
	void testDefault() {
		check( "default-fetch", false );
	}

	@Test
	void testLazy() {
		check( "lazy-fetch", true );
	}

	@Test
	void testEager() {
		check( "eager-fetch", false );
	}

	private static void check(String unitName, boolean expectation) {
		final URL xml = Thread.currentThread().getContextClassLoader().getResource( "units/to-one-fetch-type/persistence.xml" );
		final EntityManagerFactoryBuilder builder = Bootstrap.getEntityManagerFactoryBuilder(
				xml,
				unitName,
				(Map) Environment.getProperties()
		);
		verify( builder.metadata(), expectation );
	}

	private static void verify(MetadataImplementor metadata, boolean expectation) {
		PersistentClass postDescriptor = metadata.getEntityBinding( Post.class.getName() );
		final Property authorProperty = postDescriptor.getProperty( "author" );
		final ToOne authorPropertyMapping = (ToOne) authorProperty.getValue();
		assertThat( authorPropertyMapping.isLazy() ).isEqualTo( expectation);
	}

	@Test
	void testDefaultPersistenceConfiguration() {
		try( final EntityManagerFactory emf = new HibernatePersistenceConfiguration( "unit" )
				.managedClass( User.class )
				.managedClass( Post.class )
				.createEntityManagerFactory() ) {
			var postDescriptor = emf.unwrap( SessionFactoryImplementor.class ).getMappingMetamodel().getEntityDescriptor( Post.class );
			final AttributeMapping authorAttribute = postDescriptor.findAttributeMapping( "author" );
			assertThat( authorAttribute.getMappedFetchOptions().getTiming() ).isEqualTo( FetchTiming.IMMEDIATE );
		}
	}

	@Test
	void testLazyPersistenceConfiguration() {
		try( final EntityManagerFactory emf = new HibernatePersistenceConfiguration( "unit" )
				.defaultToOneFetchType( FetchType.LAZY )
				.managedClass( User.class )
				.managedClass( Post.class )
				.createEntityManagerFactory() ) {
			var postDescriptor = emf.unwrap( SessionFactoryImplementor.class ).getMappingMetamodel().getEntityDescriptor( Post.class );
			final AttributeMapping authorAttribute = postDescriptor.findAttributeMapping( "author" );
			assertThat( authorAttribute.getMappedFetchOptions().getTiming() ).isEqualTo( FetchTiming.DELAYED );
		}
	}

	@Test
	void testEagerPersistenceConfiguration() {
		try( final EntityManagerFactory emf = new HibernatePersistenceConfiguration( "unit" )
				.defaultToOneFetchType( FetchType.EAGER )
				.managedClass( User.class )
				.managedClass( Post.class )
				.createEntityManagerFactory() ) {
			var postDescriptor = emf.unwrap( SessionFactoryImplementor.class ).getMappingMetamodel().getEntityDescriptor( Post.class );
			final AttributeMapping authorAttribute = postDescriptor.findAttributeMapping( "author" );
			assertThat( authorAttribute.getMappedFetchOptions().getTiming() ).isEqualTo( FetchTiming.IMMEDIATE );
		}
	}

	@Entity(name="User")
	@Table(name="`users`")
	public static class User {
		@Id
		private Integer id;
		private String name;
	}

	@Entity(name="Post")
	@Table(name="posts")
	public static class Post {
		@Id
		private Integer id;
		private String name;
		@ManyToOne
		@JoinColumn(name="author_fk")
		private User author;
	}
}
