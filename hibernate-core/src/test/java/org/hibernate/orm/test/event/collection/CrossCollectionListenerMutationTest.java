/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.event.collection;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Column;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.FlushSettings;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PreCollectionUpdateEvent;
import org.hibernate.event.spi.PreCollectionUpdateEventListener;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Verifies that a later collection pre-listener may mutate an interpretation already
/// frozen for another collection without losing the change or firing its lifecycle twice.
///
/// @author Steve Ebersole
public class CrossCollectionListenerMutationTest {
	@DomainModel(annotatedClasses = Owner.class)
	@SessionFactory
	@ServiceRegistry(settings = {
			@Setting(name = FlushSettings.FLUSH_QUEUE_TYPE, value = "legacy"),
			@Setting(
					name = AvailableSettings.PHYSICAL_NAMING_STRATEGY,
					value = "org.hibernate.orm.test.event.collection.CrossCollectionListenerMutationTest$LegacyTableNamingStrategy"
			)
	})
	public static class LegacyQueue {
		@Test
		void laterListenerMutationRepreparesEarlierCollection(SessionFactoryScope scope) {
			verifyListenerMutation( scope );
		}
	}

	@DomainModel(annotatedClasses = Owner.class)
	@SessionFactory
	@ServiceRegistry(settings = {
			@Setting(name = FlushSettings.FLUSH_QUEUE_TYPE, value = "graph"),
			@Setting(
					name = AvailableSettings.PHYSICAL_NAMING_STRATEGY,
					value = "org.hibernate.orm.test.event.collection.CrossCollectionListenerMutationTest$GraphTableNamingStrategy"
			)
	})
	public static class GraphQueue {
		@Test
		void laterListenerMutationRepreparesEarlierCollection(SessionFactoryScope scope) {
			verifyListenerMutation( scope );
		}
	}

	private static void verifyListenerMutation(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var owner = new Owner();
			owner.id = 1L;
			owner.first.add( "first-initial" );
			owner.second.add( "second-initial" );
			session.persist( owner );
		} );

		final var listener = new MutateFirstCollectionFromSecondListener();
		scope.getSessionFactory().getEventListenerRegistry()
				.setListeners( EventType.PRE_COLLECTION_UPDATE, listener );

		scope.inTransaction( session -> {
			final var owner = session.find( Owner.class, 1L );
			owner.first.add( "first-user" );
			owner.second.add( "second-user" );
		} );

		assertThat( listener.invocations ).isEqualTo( 2 );
		scope.inTransaction( session -> {
			final var owner = session.find( Owner.class, 1L );
			assertThat( owner.first ).contains( "first-initial", "first-user" );
			assertThat( owner.second ).contains( "second-initial", "second-user" );
			assertThat( owner.first.size() + owner.second.size() ).isEqualTo( 5 );
			assertThat( owner.first.contains( "listener-change" )
					^ owner.second.contains( "listener-change" ) ).isTrue();
		} );
	}

	private static class MutateFirstCollectionFromSecondListener implements PreCollectionUpdateEventListener {
		private PersistentCollection<?> firstCollection;
		private int invocations;

		@Override
		@SuppressWarnings("unchecked")
		public void onPreUpdateCollection(PreCollectionUpdateEvent event) {
			invocations++;
			if ( firstCollection == null ) {
				firstCollection = event.getCollection();
			}
			else {
				( (Collection<String>) firstCollection ).add( "listener-change" );
			}
		}
	}

	private abstract static class PrefixedTableNamingStrategy extends PhysicalNamingStrategyStandardImpl {
		private final String prefix;

		private PrefixedTableNamingStrategy(String prefix) {
			this.prefix = prefix;
		}

		@Override
		public Identifier toPhysicalTableName(Identifier logicalName, JdbcEnvironment jdbcEnvironment) {
			return Identifier.toIdentifier( prefix + logicalName.getText(), logicalName.isQuoted() );
		}
	}

	public static class LegacyTableNamingStrategy extends PrefixedTableNamingStrategy {
		public LegacyTableNamingStrategy() {
			super( "legacy_cross_" );
		}
	}

	public static class GraphTableNamingStrategy extends PrefixedTableNamingStrategy {
		public GraphTableNamingStrategy() {
			super( "graph_cross_" );
		}
	}

	@Entity(name = "Owner")
	public static class Owner {
		@Id
		private Long id;

		@ElementCollection
		@Column(name = "first_element")
		private Set<String> first = new HashSet<>();

		@ElementCollection
		@Column(name = "second_value")
		private Set<String> second = new HashSet<>();
	}
}
