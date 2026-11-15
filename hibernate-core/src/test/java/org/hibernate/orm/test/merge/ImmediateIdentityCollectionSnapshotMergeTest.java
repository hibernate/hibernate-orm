package org.hibernate.orm.test.merge;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import org.hibernate.Hibernate;
import org.hibernate.cfg.FlushSettings;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PreCollectionUpdateEventListener;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@DomainModel(annotatedClasses = {
		ImmediateIdentityCollectionSnapshotMergeTest.Owner.class,
		ImmediateIdentityCollectionSnapshotMergeTest.Child.class
})
@SessionFactory
@ServiceRegistry(settings = @Setting(name = FlushSettings.GRAPH_DEFER_IDENTITY_INSERTS, value = "false"))
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsIdentityColumns.class)
@Jira("https://hibernate.atlassian.net/browse/HHH-21124")
class ImmediateIdentityCollectionSnapshotMergeTest {

	@Test
	void mergeRetainsCollectionEventBeforeImage(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new Owner() ) );
		final var detached = scope.fromTransaction( session -> {
			final var owner = session.find( Owner.class, 1L );
			Hibernate.initialize( owner.children );
			return owner;
		} );
		final var child = new Child();
		child.owner = detached;
		detached.children.add( child );

		final List<Integer> snapshotSizes = new ArrayList<>();
		scope.getSessionFactory().getServiceRegistry().getService( EventListenerRegistry.class )
				.appendListeners( EventType.PRE_COLLECTION_UPDATE, (PreCollectionUpdateEventListener) event -> {
					final var snapshot = event.getSession().getPersistenceContextInternal()
							.getCollectionEntry( event.getCollection() ).getSnapshot();
					snapshotSizes.add( ( (Collection<?>) snapshot ).size() );
				} );

		scope.inTransaction( session -> {
			// Preload the owner so merge initializes the collection after inserting the child.
			final var managed = session.find( Owner.class, 1L );
			assertFalse( Hibernate.isInitialized( managed.children ) );
			assertEquals( 1, session.merge( detached ).children.size() );
		} );
		assertEquals( List.of( 0 ), snapshotSizes );
	}

	@Entity(name = "IdentitySnapshotOwner")
	static class Owner {
		@Id
		long id = 1L;

		@OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
		List<Child> children = new ArrayList<>();
	}

	@Entity(name = "IdentitySnapshotChild")
	static class Child {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		Long id;

		@ManyToOne
		Owner owner;
	}
}
