/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.embeddable.strategy.instantiator.snapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hibernate.annotations.EmbeddableInstantiator;
import org.hibernate.metamodel.spi.ValueAccess;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The database snapshot taken to decide whether a detached entity is transient
 * does not select its embedded attributes. The embeddable must then be missing
 * from the snapshot instead of being instantiated from the default values of
 * the attributes that were not selected.
 */
@DomainModel(annotatedClasses = {
		DatabaseSnapshotInstantiatorTest.Author.class,
		DatabaseSnapshotInstantiatorTest.Post.class
})
@SessionFactory
@JiraKey("HHH-20810")
public class DatabaseSnapshotInstantiatorTest {

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void persistNewEntityReferencingDetachedEntity(SessionFactoryScope scope) {
		final Author author = new Author( 1, new Credentials( "oidc", 3 ) );
		scope.inTransaction( session -> session.persist( author ) );

		// No cascade and no version: the persist has to take a database
		// snapshot of the detached author to decide whether it is transient
		CredentialsInstantiator.instantiations.clear();
		scope.inTransaction( session -> session.persist( new Post( 1, author ) ) );
		assertThat( CredentialsInstantiator.instantiations ).isEmpty();

		scope.inTransaction( session -> {
			final Post post = session.find( Post.class, 1 );
			assertThat( post.author.id ).isEqualTo( 1 );
			assertThat( post.author.credentials.method ).isEqualTo( "oidc" );
			assertThat( post.author.credentials.failedLoginAttempts ).isEqualTo( 3 );
		} );
	}

	@Test
	void snapshotLeavesUnselectedEmbeddableNull(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new Author( 1, new Credentials( "oidc", 3 ) ) ) );

		CredentialsInstantiator.instantiations.clear();
		scope.inTransaction( session -> {
			final var persister = session.getSessionFactory().getMappingMetamodel()
					.getEntityDescriptor( Author.class );
			final Object[] snapshot = session.getPersistenceContextInternal().getDatabaseSnapshot( 1, persister );
			assertThat( snapshot[persister.getPropertyIndex( "credentials" )] ).isNull();
		} );
		assertThat( CredentialsInstantiator.instantiations ).isEmpty();
	}

	@Embeddable
	@EmbeddableInstantiator(CredentialsInstantiator.class)
	public static class Credentials {
		String method;
		// an attribute whose Java type has a non-null default value
		Integer failedLoginAttempts;

		public Credentials(String method, Integer failedLoginAttempts) {
			this.method = method;
			this.failedLoginAttempts = failedLoginAttempts;
		}
	}

	public static class CredentialsInstantiator implements org.hibernate.metamodel.spi.EmbeddableInstantiator {
		static final List<Object[]> instantiations = new ArrayList<>();

		@Override
		public Object instantiate(ValueAccess valueAccess) {
			// attributes are passed in alphabetical order
			final Object[] values = valueAccess.getValues();
			instantiations.add( values );
			final String method = (String) values[1];
			if ( method == null ) {
				throw new IllegalStateException( "'method' is null: " + Arrays.toString( values ) );
			}
			return new Credentials( method, (Integer) values[0] );
		}

		@Override
		public boolean isInstance(Object object) {
			return object instanceof Credentials;
		}

		@Override
		public boolean isSameClass(Object object) {
			return object instanceof Credentials;
		}
	}

	@Entity(name = "Author")
	public static class Author {
		@Id
		Integer id;
		@Embedded
		Credentials credentials;

		public Author() {
		}

		public Author(Integer id, Credentials credentials) {
			this.id = id;
			this.credentials = credentials;
		}
	}

	@Entity(name = "Post")
	public static class Post {
		@Id
		Integer id;
		@ManyToOne
		Author author;

		public Post() {
		}

		public Post(Integer id, Author author) {
			this.id = id;
			this.author = author;
		}
	}
}
