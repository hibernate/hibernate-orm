package org.hibernate.orm.test.cache;

import java.util.List;
import java.util.Set;

import org.hibernate.AnnotationException;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.boot.MetadataSources;

import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * A {@code @Cache} annotation on an attribute which is not a collection used to be silently ignored.
 */
@Jira("https://hibernate.atlassian.net/browse/HHH-21122")
public class CacheAnnotationOnAttributeTest {

	@Test
	public void testCacheOnManyToOne() {
		assertRejected( "WithManyToOne.parent", WithManyToOne.class, Parent.class );
	}

	@Test
	public void testCacheOnOneToOne() {
		assertRejected( "WithOneToOne.parent", WithOneToOne.class, Parent.class );
	}

	@Test
	public void testCacheOnBasic() {
		assertRejected( "WithBasic.name", WithBasic.class );
	}

	@Test
	public void testCacheOnEmbedded() {
		assertRejected( "WithEmbedded.address", WithEmbedded.class );
	}

	@Test
	public void testCacheOnAttributeOfEmbeddable() {
		assertRejected( "WithCachedEmbeddable.address.city", WithCachedEmbeddable.class );
	}

	@Test
	public void testCacheOnCollections() {
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			final var metadataSources =
					new MetadataSources( registry )
							.addAnnotatedClass( WithCollections.class )
							.addAnnotatedClass( Child.class );
			assertDoesNotThrow( () -> metadataSources.buildMetadata() );
		}
	}

	private static void assertRejected(String path, Class<?>... classes) {
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			final var metadataSources = new MetadataSources( registry );
			for ( var entityClass : classes ) {
				metadataSources.addAnnotatedClass( entityClass );
			}
			final var thrown = assertThrows( AnnotationException.class, metadataSources::buildMetadata );
			assertThat( thrown.getMessage() )
					.contains( path )
					.contains( "is annotated '@Cache' but is not a collection" );
		}
	}

	@Entity(name = "Parent")
	@Table(name = "cache_attr_parent")
	public static class Parent {
		@Id
		Long id;
	}

	@Entity(name = "WithManyToOne")
	@Table(name = "cache_attr_m2o")
	public static class WithManyToOne {
		@Id
		Long id;

		@ManyToOne
		@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
		Parent parent;
	}

	@Entity(name = "WithOneToOne")
	@Table(name = "cache_attr_o2o")
	public static class WithOneToOne {
		@Id
		Long id;

		@OneToOne
		@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
		Parent parent;
	}

	@Entity(name = "WithBasic")
	@Table(name = "cache_attr_basic")
	public static class WithBasic {
		@Id
		Long id;

		@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
		String name;
	}

	@Entity(name = "WithEmbedded")
	@Table(name = "cache_attr_emb")
	public static class WithEmbedded {
		@Id
		Long id;

		@Embedded
		@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
		Address address;
	}

	@Entity(name = "WithCachedEmbeddable")
	@Table(name = "cache_attr_emb_attr")
	public static class WithCachedEmbeddable {
		@Id
		Long id;

		@Embedded
		CachedAddress address;
	}

	@Entity(name = "WithCollections")
	@Table(name = "cache_attr_coll")
	public static class WithCollections {
		@Id
		Long id;

		@OneToMany(mappedBy = "owner")
		@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
		List<Child> children;

		@ElementCollection
		@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
		Set<String> tags;
	}

	@Entity(name = "Child")
	@Table(name = "cache_attr_child")
	public static class Child {
		@Id
		Long id;

		@ManyToOne
		WithCollections owner;
	}

	@Embeddable
	public static class Address {
		String city;
	}

	@Embeddable
	public static class CachedAddress {
		@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
		String city;
	}
}
