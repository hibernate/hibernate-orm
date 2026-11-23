package org.hibernate.orm.test.mapping.collections.defaultsemantics;

import java.util.List;

import org.hibernate.AnnotationException;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.internal.log.DeprecationLogger;
import org.hibernate.mapping.Bag;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.IdentifierBag;
import org.hibernate.orm.test.mapping.collections.defaultsemantics.list.sub.ScopeOwner;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Logger;
import org.hibernate.testing.orm.junit.MessageKeyInspection;
import org.hibernate.testing.orm.junit.MessageKeyWatcher;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.cfg.MappingSettings.DEFAULT_LIST_SEMANTICS;

/// @author Steve Ebersole
@JiraKey("HHH-20893")
@SuppressWarnings("removal")
class DefaultListSemanticsTests {
	@Test
	void listPackage() {
		verifyPackage( org.hibernate.orm.test.mapping.collections.defaultsemantics.list.Model.Owner.class,
				org.hibernate.orm.test.mapping.collections.defaultsemantics.list.Model.Child.class, true );
	}

	@Test
	void bagPackage() {
		verifyPackage( org.hibernate.orm.test.mapping.collections.defaultsemantics.bag.Model.Owner.class,
				org.hibernate.orm.test.mapping.collections.defaultsemantics.bag.Model.Child.class, false );
	}

	private void verifyPackage(Class<?> owner, Class<?> child, boolean indexed) {
		try (var registry = new StandardServiceRegistryBuilder()
				.applySetting( DEFAULT_LIST_SEMANTICS, indexed ? "BAG" : "LIST" ).build()) {
			final var metadata = new MetadataSources( registry ).addAnnotatedClass( owner )
					.addAnnotatedClass( child ).buildMetadata();
			assertCollection( metadata, owner, "plain", indexed ? org.hibernate.mapping.List.class : Bag.class );
			assertCollection( metadata, owner, "bag", Bag.class );
			assertCollection( metadata, owner, "indexed", org.hibernate.mapping.List.class );
			assertCollection( metadata, owner, "based", org.hibernate.mapping.List.class );
			assertCollection( metadata, owner, "ordered", Bag.class );
			assertCollection( metadata, owner, "sqlOrdered", Bag.class );
			assertCollection( metadata, owner, "identified", IdentifierBag.class );
			assertCollection( metadata, owner, "inverse", Bag.class );
			assertCollection( metadata, owner, "inverseIndexed", org.hibernate.mapping.List.class );
			assertCollection( metadata, owner, "inverseMany", Bag.class );
			assertCollection( metadata, child, "owners", indexed ? org.hibernate.mapping.List.class : Bag.class );
			final var plain = metadata.getCollectionBinding( owner.getName() + ".plain" );
			assertThat( plain.getCollectionTable().getColumns().stream()
					.anyMatch( column -> column.getName().equals( "plain_ORDER" ) ) ).isEqualTo( indexed );
		}
	}

	@Test
	void declaringScopeAndNoSubpackageInheritance() {
		try (var registry = new StandardServiceRegistryBuilder().build()) {
			final var metadata = new MetadataSources( registry ).addAnnotatedClass( ScopeOwner.class ).buildMetadata();
			assertCollection( metadata, ScopeOwner.class, "local", Bag.class );
			assertCollection( metadata, ScopeOwner.class, "inherited", org.hibernate.mapping.List.class );
			final var component = (Component) metadata.getEntityBinding( ScopeOwner.class.getName() )
					.getProperty( "details" ).getValue();
			assertThat( component.getProperty( "embedded" ).getValue() ).isInstanceOf( org.hibernate.mapping.List.class );
		}
	}

	@Test
	void implicitPositionsSurviveReloadAndReordering() {
		try (var registry = new StandardServiceRegistryBuilder()
				.applySetting( "hibernate.hbm2ddl.auto", "create-drop" ).build()) {
			final var metadata = new MetadataSources( registry ).addAnnotatedClass( ScopeOwner.class ).buildMetadata();
			try (var factory = metadata.buildSessionFactory()) {
				factory.inTransaction( session -> {
					final var owner = new ScopeOwner();
					owner.id = 1;
					owner.inherited = new java.util.ArrayList<>( List.of( "z", "a", "z" ) );
					session.persist( owner );
				} );
				factory.inTransaction( session -> {
					final var owner = session.find( ScopeOwner.class, 1 );
					assertThat( owner.inherited ).containsExactly( "z", "a", "z" );
					owner.inherited.remove( 0 );
					owner.inherited.add( "b" );
				} );
				factory.inTransaction( session -> assertThat( session.find( ScopeOwner.class, 1 ).inherited )
						.containsExactly( "a", "z", "b" ) );
			}
		}
	}

	@Test
	void incompatibleMetadataStillFails() {
		for (var type : List.of(
				org.hibernate.orm.test.mapping.collections.defaultsemantics.list.Model.InvalidOrder.class,
				org.hibernate.orm.test.mapping.collections.defaultsemantics.list.Model.InvalidBase.class,
				org.hibernate.orm.test.mapping.collections.defaultsemantics.bag.Model.InvalidOrder.class,
				org.hibernate.orm.test.mapping.collections.defaultsemantics.bag.Model.InvalidBase.class )) {
			try (var registry = new StandardServiceRegistryBuilder().build()) {
				assertThatThrownBy( () -> new MetadataSources( registry ).addAnnotatedClass( type ).buildMetadata() )
						.isInstanceOf( AnnotationException.class ).hasMessageContaining( "annotated '@Bag'" );
			}
		}
	}

	@Test
	@MessageKeyInspection(
			messageKey = "HHH90000021",
			logger = @Logger(loggerName = DeprecationLogger.CATEGORY)
	)
	void settingWarningEvenWhenOverridden(MessageKeyWatcher watcher) {
		try (var registry = new StandardServiceRegistryBuilder().build()) {
			new MetadataSources( registry ).addAnnotatedClass( ScopeOwner.class ).buildMetadata();
			assertThat( watcher.wasTriggered() ).isFalse();
		}
		try (var registry = new StandardServiceRegistryBuilder().applySetting( DEFAULT_LIST_SEMANTICS, "BAG" ).build()) {
			final var metadata = new MetadataSources( registry ).addAnnotatedClass( ScopeOwner.class ).buildMetadata();
			assertCollection( metadata, ScopeOwner.class, "inherited", org.hibernate.mapping.List.class );
			assertThat( watcher.getTriggeredMessages() ).hasSize( 1 );
			assertThat( watcher.getFirstTriggeredMessage() ).contains( DEFAULT_LIST_SEMANTICS, "@DefaultListSemantics" );
		}
	}

	@Test
	void programmaticFallback() {
		try (var registry = new StandardServiceRegistryBuilder().build()) {
			final var metadata = new MetadataSources( registry ).addAnnotatedClass( ScopeOwner.class )
					.getMetadataBuilder().applyImplicitListSemantics( org.hibernate.metamodel.CollectionClassification.LIST )
					.build();
			assertCollection( metadata, ScopeOwner.class, "local", org.hibernate.mapping.List.class );
		}
	}

	private static void assertCollection(Metadata metadata, Class<?> owner, String name, Class<?> expected) {
		assertThat( metadata.getEntityBinding( owner.getName() ).getProperty( name ).getValue() ).isInstanceOf( expected );
	}
}
