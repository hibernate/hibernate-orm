package org.hibernate.orm.test.collection.ordercol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.metamodel.UnsupportedMappingException;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The order column of a one-to-many list is written by an SQL {@code update}
 * of the table of the element. An order column which is insertable but not
 * updatable is rejected when nothing would ever write it.
 */
@ServiceRegistry(settings = @Setting(name = AvailableSettings.HBM2DDL_AUTO, value = "create-drop"))
@JiraKey("HHH-21260")
class NonUpdatableOrderColumnTest {

	@Test
	void unownedList(ServiceRegistryScope scope) {
		assertRejected( scope, "UnownedRoot.entries", UnownedRoot.class, UnownedEntry.class );
	}

	@Test
	void ownedList(ServiceRegistryScope scope) {
		assertRejected( scope, "OwnedRoot.entries", OwnedRoot.class, OwnedEntry.class );
	}

	@Test
	void elementMapsOrderColumn(ServiceRegistryScope scope) {
		inSessionFactory( scope, factory -> {
			factory.inTransaction( session -> {
				final var root = new MappedRoot();
				root.id = 1L;
				session.persist( root );
				for ( int i = 0; i < 3; i++ ) {
					final var entry = new MappedEntry();
					entry.id = 10L + i;
					entry.pos = i;
					entry.root = root;
					root.entries.add( entry );
					session.persist( entry );
				}
			} );
			factory.inTransaction( session -> assertThat( session.find( MappedRoot.class, 1L ).entries )
					.extracting( entry -> entry.id ).containsExactly( 10L, 11L, 12L ) );
		}, MappedRoot.class, MappedEntry.class );
	}

	@Test
	void notNullJoinColumn(ServiceRegistryScope scope) {
		inSessionFactory( scope, factory -> {
			factory.inTransaction( session -> {
				final var root = new BackrefRoot();
				root.id = 1L;
				for ( int i = 0; i < 3; i++ ) {
					final var entry = new BackrefEntry();
					entry.id = 10L + i;
					root.entries.add( entry );
				}
				session.persist( root );
			} );
			factory.inTransaction( session -> assertThat( session.find( BackrefRoot.class, 1L ).entries )
					.extracting( entry -> entry.id ).containsExactly( 10L, 11L, 12L ) );
		}, BackrefRoot.class, BackrefEntry.class );
	}

	@Test
	void neverWrittenOrderColumn(ServiceRegistryScope scope) {
		inSessionFactory( scope, factory -> factory.inTransaction( session -> {
			final var root = new ReadOnlyRoot();
			root.id = 1L;
			session.persist( root );
			final var entry = new ReadOnlyEntry();
			entry.id = 10L;
			entry.root = root;
			root.entries.add( entry );
			session.persist( entry );
		} ), ReadOnlyRoot.class, ReadOnlyEntry.class );
	}

	@Test
	void mapKeyColumn(ServiceRegistryScope scope) {
		inSessionFactory( scope, factory -> {
			factory.inTransaction( session -> {
				final var root = new KeyedRoot();
				root.id = 1L;
				session.persist( root );
				final var entry = new KeyedEntry();
				entry.id = 10L;
				entry.root = root;
				root.entries.put( "first", entry );
				session.persist( entry );
			} );
			factory.inTransaction( session -> assertThat( session.find( KeyedRoot.class, 1L ).entries )
					.containsOnlyKeys( "first" ) );
		}, KeyedRoot.class, KeyedEntry.class );
	}

	private static void assertRejected(ServiceRegistryScope scope, String role, Class<?>... entityClasses) {
		final var metadata = new MetadataSources( scope.getRegistry() )
				.addAnnotatedClasses( entityClasses )
				.buildMetadata();
		assertThatThrownBy( () -> metadata.buildSessionFactory().close() )
				.isInstanceOf( UnsupportedMappingException.class )
				.hasMessageContaining( role + "' is a one-to-many association with an order column which is not updatable" );
	}

	private static void inSessionFactory(
			ServiceRegistryScope scope,
			Consumer<SessionFactory> work,
			Class<?>... entityClasses) {
		try ( var factory = new MetadataSources( scope.getRegistry() )
				.addAnnotatedClasses( entityClasses )
				.buildMetadata()
				.buildSessionFactory() ) {
			work.accept( factory );
		}
	}

	@Entity(name = "UnownedRoot")
	@Table(name = "oc_unowned_root")
	static class UnownedRoot {
		@Id
		Long id;
		@OneToMany(mappedBy = "root")
		@OrderColumn(name = "pos", updatable = false)
		List<UnownedEntry> entries = new ArrayList<>();
	}

	@Entity(name = "UnownedEntry")
	@Table(name = "oc_unowned_entry")
	static class UnownedEntry {
		@Id
		Long id;
		@ManyToOne
		UnownedRoot root;
	}

	@Entity(name = "OwnedRoot")
	@Table(name = "oc_owned_root")
	static class OwnedRoot {
		@Id
		Long id;
		@OneToMany
		@JoinColumn(name = "root_id")
		@OrderColumn(name = "pos", updatable = false)
		List<OwnedEntry> entries = new ArrayList<>();
	}

	@Entity(name = "OwnedEntry")
	@Table(name = "oc_owned_entry")
	static class OwnedEntry {
		@Id
		Long id;
	}

	@Entity(name = "MappedRoot")
	@Table(name = "oc_mapped_root")
	static class MappedRoot {
		@Id
		Long id;
		@OneToMany(mappedBy = "root")
		@OrderColumn(name = "pos", updatable = false)
		List<MappedEntry> entries = new ArrayList<>();
	}

	@Entity(name = "MappedEntry")
	@Table(name = "oc_mapped_entry")
	static class MappedEntry {
		@Id
		Long id;
		@Column(name = "pos")
		int pos;
		@ManyToOne
		MappedRoot root;
	}

	@Entity(name = "BackrefRoot")
	@Table(name = "oc_backref_root")
	static class BackrefRoot {
		@Id
		Long id;
		@OneToMany(cascade = CascadeType.PERSIST)
		@JoinColumn(name = "root_id", nullable = false)
		@OrderColumn(name = "pos", updatable = false)
		List<BackrefEntry> entries = new ArrayList<>();
	}

	@Entity(name = "BackrefEntry")
	@Table(name = "oc_backref_entry")
	static class BackrefEntry {
		@Id
		Long id;
	}

	@Entity(name = "ReadOnlyRoot")
	@Table(name = "oc_readonly_root")
	static class ReadOnlyRoot {
		@Id
		Long id;
		@OneToMany(mappedBy = "root")
		@OrderColumn(name = "pos", insertable = false, updatable = false)
		List<ReadOnlyEntry> entries = new ArrayList<>();
	}

	@Entity(name = "ReadOnlyEntry")
	@Table(name = "oc_readonly_entry")
	static class ReadOnlyEntry {
		@Id
		Long id;
		@ManyToOne
		ReadOnlyRoot root;
	}

	@Entity(name = "KeyedRoot")
	@Table(name = "oc_keyed_root")
	static class KeyedRoot {
		@Id
		Long id;
		@OneToMany(mappedBy = "root")
		@MapKeyColumn(name = "code", updatable = false)
		Map<String, KeyedEntry> entries = new HashMap<>();
	}

	@Entity(name = "KeyedEntry")
	@Table(name = "oc_keyed_entry")
	static class KeyedEntry {
		@Id
		Long id;
		@ManyToOne
		KeyedRoot root;
	}
}
