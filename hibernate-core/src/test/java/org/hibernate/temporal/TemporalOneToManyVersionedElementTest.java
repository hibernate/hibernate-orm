package org.hibernate.temporal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.Temporal;
import org.hibernate.boot.MetadataSources;
import org.hibernate.metamodel.UnsupportedMappingException;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.cfg.StateManagementSettings.TEMPORAL_TABLE_STRATEGY;

/// A one-to-many association writes its join column and order column to the
/// row of the associated entity without incrementing its version. A version
/// of an entity has one revision, so the association cannot give a versioned
/// entity a new revision, and must be excluded from temporal versioning.
@JiraKey("HHH-21285")
@ServiceRegistry(settings = @Setting(name = TEMPORAL_TABLE_STRATEGY, value = "HISTORY_TABLE"))
class TemporalOneToManyVersionedElementTest {

	@Test
	void rejectsJoinColumnOfVersionedElement(ServiceRegistryScope scope) {
		assertRejected( scope, "Owner.items", Item.class, Owner.class );
	}

	@Test
	void rejectsOrderColumnWrittenByUnownedSide(ServiceRegistryScope scope) {
		assertRejected( scope, "Team.players", Player.class, Team.class );
	}

	@Test
	void allowsExcludedAssociation(ServiceRegistryScope scope) {
		assertAllowed( scope, ExcludingOwner.class, Item.class );
	}

	@Test
	void allowsUnversionedElement(ServiceRegistryScope scope) {
		assertAllowed( scope, PlainOwner.class, PlainItem.class );
	}

	@Test
	void allowsAssociationWhichWritesNoColumn(ServiceRegistryScope scope) {
		assertAllowed( scope, Club.class, Fan.class );
	}

	@Test
	@ServiceRegistry(settings = @Setting(name = TEMPORAL_TABLE_STRATEGY, value = "SINGLE_TABLE"))
	void allowsVersionedElementWithoutHistoryTable(ServiceRegistryScope scope) {
		assertAllowed( scope, Owner.class, Item.class );
	}

	private static void assertRejected(
			ServiceRegistryScope scope, String role, Class<?> elementClass, Class<?> ownerClass) {
		final var metadata = new MetadataSources( scope.getRegistry() )
				.addAnnotatedClasses( ownerClass, elementClass )
				.buildMetadata();
		assertThatThrownBy( () -> metadata.buildSessionFactory().close() )
				.isInstanceOf( UnsupportedMappingException.class )
				.hasMessageContaining( role + "' is a one-to-many association which writes the row of versioned entity '"
						+ elementClass.getName() + "'" )
				.hasMessageContaining( "@Temporal.Excluded" )
				.hasMessageContaining( "HISTORY_TABLE" );
	}

	private static void assertAllowed(ServiceRegistryScope scope, Class<?>... entityClasses) {
		new MetadataSources( scope.getRegistry() )
				.addAnnotatedClasses( entityClasses )
				.buildMetadata().buildSessionFactory().close();
	}

	@Entity(name = "VeOwner")
	@Table(name = "ve_owner")
	@Temporal
	static class Owner {
		@Id int id;
		@OneToMany
		@JoinColumn(name = "owner_id")
		Set<Item> items = new HashSet<>();
	}

	@Entity(name = "VeExcludingOwner")
	@Table(name = "ve_excluding_owner")
	@Temporal
	static class ExcludingOwner {
		@Id int id;
		@OneToMany
		@JoinColumn(name = "owner_id")
		@Temporal.Excluded
		Set<Item> items = new HashSet<>();
	}

	@Entity(name = "VeItem")
	@Table(name = "ve_item")
	@Temporal
	static class Item {
		@Id int id;
		@Version int version;
	}

	@Entity(name = "VeTeam")
	@Table(name = "ve_team")
	@Temporal
	static class Team {
		@Id int id;
		// the order column is not mapped by the owning side, so this side writes it
		@OneToMany(mappedBy = "team")
		@OrderColumn(name = "pos")
		List<Player> players = new ArrayList<>();
	}

	@Entity(name = "VePlayer")
	@Table(name = "ve_player")
	@Temporal
	static class Player {
		@Id int id;
		@Version int version;
		@ManyToOne Team team;
	}

	@Entity(name = "VePlainOwner")
	@Table(name = "ve_plain_owner")
	@Temporal
	static class PlainOwner {
		@Id int id;
		@OneToMany
		@JoinColumn(name = "owner_id")
		Set<PlainItem> items = new HashSet<>();
	}

	@Entity(name = "VePlainItem")
	@Table(name = "ve_plain_item")
	@Temporal
	static class PlainItem {
		@Id int id;
	}

	@Entity(name = "VeClub")
	@Table(name = "ve_club")
	@Temporal
	static class Club {
		@Id int id;
		@OneToMany(mappedBy = "club")
		Set<Fan> fans = new HashSet<>();
	}

	@Entity(name = "VeFan")
	@Table(name = "ve_fan")
	@Temporal
	static class Fan {
		@Id int id;
		@Version int version;
		@ManyToOne Club club;
	}
}
