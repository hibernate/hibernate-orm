package org.hibernate.orm.test.softdelete.collections;

import java.util.LinkedHashSet;
import java.util.Set;

import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DomainModel( annotatedClasses = {
		SoftDeleteElementCollectionReaddTest.TimestampSoftDeleteCollectionOwner.class,
		SoftDeleteElementCollectionReaddTest.ActiveSoftDeleteCollectionOwner.class,
		SoftDeleteElementCollectionReaddTest.DeletedSoftDeleteCollectionOwner.class
} )
@SessionFactory
@ServiceRegistry( settings = {
		@Setting( name = "hibernate.hbm2ddl.halt_on_error", value = "true" ),
		@Setting( name = "jakarta.persistence.schema-generation.database.action", value = "create" )
} )
@Jira( "https://hibernate.atlassian.net/browse/HHH-20585" )
public class SoftDeleteElementCollectionReaddTest {
	@AfterAll
	void dropSchema(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().dropMappedObjects( false );
	}

	@Test
	void readdingPreviouslyTimestampSoftDeletedElementCollectionEntry(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final TimestampSoftDeleteCollectionOwner owner = new TimestampSoftDeleteCollectionOwner( 1L );
			owner.getMobiles().addAll( Set.of( "1", "2" ) );
			session.persist( owner );
		} );

		scope.inTransaction( session -> {
			final TimestampSoftDeleteCollectionOwner owner = session.get( TimestampSoftDeleteCollectionOwner.class, 1L );
			owner.getMobiles().clear();
			owner.getMobiles().addAll( Set.of( "2", "3" ) );
		} );

		scope.inTransaction( session -> {
			final TimestampSoftDeleteCollectionOwner owner = session.get( TimestampSoftDeleteCollectionOwner.class, 1L );
			owner.getMobiles().clear();
			owner.getMobiles().addAll( Set.of( "1", "3" ) );
		} );

		scope.inTransaction( session -> {
			final TimestampSoftDeleteCollectionOwner owner = session.get( TimestampSoftDeleteCollectionOwner.class, 1L );
			assertThat( owner.getMobiles() ).containsExactlyInAnyOrder( "1", "3" );
		} );
	}

	@Test
	void timestampSoftDeleteCollectionKeyFollowsDialectSupport(DomainModelScope scope) {
		final var table = scope.getDomainModel()
				.getCollectionBinding( TimestampSoftDeleteCollectionOwner.class.getName() + ".mobiles" )
				.getCollectionTable();
		if ( supportsLiveRowUniqueness( scope ) ) {
			assertThat( table.getUniqueKeys().values() )
					.singleElement()
					.satisfies( key -> assertThat( key.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactlyInAnyOrder( "owner_id", "mobiles", "deleted_at" ) );
		}
		else {
			assertNoImplicitUniqueConstraint( scope, TimestampSoftDeleteCollectionOwner.class );
		}
	}

	@Test
	void timestampTableEnforcesOnlySupportedLiveRowUniqueness(SessionFactoryScope scope, DomainModelScope model) {
		scope.inTransaction( session -> {
			final var owner = new TimestampSoftDeleteCollectionOwner( 2L );
			owner.getMobiles().add( "duplicate" );
			session.persist( owner );
		} );
		final String insert = "insert into soft_delete_collection_owner_mobile (owner_id, mobiles, deleted_at)"
				+ " values (2, 'duplicate', null)";
		if ( supportsLiveRowUniqueness( model ) ) {
			assertThrows( ConstraintViolationException.class,
					() -> scope.inTransaction( session -> session.createNativeMutationQuery( insert ).executeUpdate() ) );
		}
		else {
			scope.inTransaction( session -> session.createNativeMutationQuery( insert ).executeUpdate() );
		}
		scope.inTransaction( session -> assertThat( session.createNativeQuery(
				"select count(*) from soft_delete_collection_owner_mobile where owner_id = 2", Long.class )
				.getSingleResult() ).isEqualTo( supportsLiveRowUniqueness( model ) ? 1L : 2L ) );
	}

	private static boolean supportsLiveRowUniqueness(DomainModelScope scope) {
		return scope.getDomainModel().getDatabase().getDialect().getUniqueDelegate().supportsNullsNotDistinct();
	}

	@Test
	void booleanSoftDeleteCollectionTablesHaveNoImplicitUniqueConstraint(DomainModelScope scope) {
		assertNoImplicitUniqueConstraint( scope, ActiveSoftDeleteCollectionOwner.class );
		assertNoImplicitUniqueConstraint( scope, DeletedSoftDeleteCollectionOwner.class );
	}

	@Test
	void reremovingPreviouslyActiveSoftDeletedElementCollectionEntry(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final ActiveSoftDeleteCollectionOwner owner = new ActiveSoftDeleteCollectionOwner( 1L );
			owner.getMobiles().addAll( Set.of( "1", "2" ) );
			session.persist( owner );
		} );

		scope.inTransaction( session -> {
			final ActiveSoftDeleteCollectionOwner owner = session.get( ActiveSoftDeleteCollectionOwner.class, 1L );
			owner.getMobiles().clear();
			owner.getMobiles().addAll( Set.of( "2", "3" ) );
		} );

		scope.inTransaction( session -> {
			final ActiveSoftDeleteCollectionOwner owner = session.get( ActiveSoftDeleteCollectionOwner.class, 1L );
			owner.getMobiles().clear();
			owner.getMobiles().addAll( Set.of( "1", "3" ) );
		} );

		scope.inTransaction( session -> {
			final ActiveSoftDeleteCollectionOwner owner = session.get( ActiveSoftDeleteCollectionOwner.class, 1L );
			owner.getMobiles().clear();
			owner.getMobiles().add( "3" );
		} );

		scope.inTransaction( session -> {
			final ActiveSoftDeleteCollectionOwner owner = session.get( ActiveSoftDeleteCollectionOwner.class, 1L );
			assertThat( owner.getMobiles() ).containsExactly( "3" );
		} );
	}

	@Test
	void reremovingPreviouslyDeletedSoftDeletedElementCollectionEntry(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final DeletedSoftDeleteCollectionOwner owner = new DeletedSoftDeleteCollectionOwner( 1L );
			owner.getMobiles().addAll( Set.of( "1", "2" ) );
			session.persist( owner );
		} );

		scope.inTransaction( session -> {
			final DeletedSoftDeleteCollectionOwner owner = session.get( DeletedSoftDeleteCollectionOwner.class, 1L );
			owner.getMobiles().clear();
			owner.getMobiles().addAll( Set.of( "2", "3" ) );
		} );

		scope.inTransaction( session -> {
			final DeletedSoftDeleteCollectionOwner owner = session.get( DeletedSoftDeleteCollectionOwner.class, 1L );
			owner.getMobiles().clear();
			owner.getMobiles().addAll( Set.of( "1", "3" ) );
		} );

		scope.inTransaction( session -> {
			final DeletedSoftDeleteCollectionOwner owner = session.get( DeletedSoftDeleteCollectionOwner.class, 1L );
			owner.getMobiles().clear();
			owner.getMobiles().add( "3" );
		} );

		scope.inTransaction( session -> {
			final DeletedSoftDeleteCollectionOwner owner = session.get( DeletedSoftDeleteCollectionOwner.class, 1L );
			assertThat( owner.getMobiles() ).containsExactly( "3" );
		} );
	}

	private static void assertNoImplicitUniqueConstraint(DomainModelScope scope, Class<?> collectionOwner) {
		final org.hibernate.mapping.Table table = scope.getDomainModel()
				.getCollectionBinding( collectionOwner.getName() + ".mobiles" )
				.getCollectionTable();
		assertThat( table.hasPrimaryKey() ).isFalse();
		assertThat( table.getUniqueKeys() ).isEmpty();
		assertThat( table.getIndexes() ).isEmpty();
	}

	@Entity( name = "SoftDeleteCollectionOwner" )
	@Table( name = "soft_delete_collection_owner" )
	static class TimestampSoftDeleteCollectionOwner {
		@Id
		private Long id;

		@ElementCollection
		@CollectionTable( name = "soft_delete_collection_owner_mobile", joinColumns = @JoinColumn( name = "owner_id" ) )
		@SoftDelete( columnName = "deleted_at", strategy = SoftDeleteType.TIMESTAMP )
		private Set<String> mobiles = new LinkedHashSet<>();

		TimestampSoftDeleteCollectionOwner() {
		}

		TimestampSoftDeleteCollectionOwner(Long id) {
			this.id = id;
		}

		Set<String> getMobiles() {
			return mobiles;
		}
	}

	@Entity( name = "ActiveSoftDeleteCollectionOwner" )
	@Table( name = "active_soft_delete_collection_owner" )
	static class ActiveSoftDeleteCollectionOwner {
		@Id
		private Long id;

		@ElementCollection
		@CollectionTable( name = "active_soft_delete_collection_owner_mobile", joinColumns = @JoinColumn( name = "owner_id" ) )
		@SoftDelete( strategy = SoftDeleteType.ACTIVE )
		private Set<String> mobiles = new LinkedHashSet<>();

		ActiveSoftDeleteCollectionOwner() {
		}

		ActiveSoftDeleteCollectionOwner(Long id) {
			this.id = id;
		}

		Set<String> getMobiles() {
			return mobiles;
		}
	}

	@Entity( name = "DeletedSoftDeleteCollectionOwner" )
	@Table( name = "deleted_soft_delete_collection_owner" )
	static class DeletedSoftDeleteCollectionOwner {
		@Id
		private Long id;

		@ElementCollection
		@CollectionTable( name = "deleted_soft_delete_collection_owner_mobile", joinColumns = @JoinColumn( name = "owner_id" ) )
		@SoftDelete( strategy = SoftDeleteType.DELETED )
		private Set<String> mobiles = new LinkedHashSet<>();

		DeletedSoftDeleteCollectionOwner() {
		}

		DeletedSoftDeleteCollectionOwner(Long id) {
			this.id = id;
		}

		Set<String> getMobiles() {
			return mobiles;
		}
	}
}
