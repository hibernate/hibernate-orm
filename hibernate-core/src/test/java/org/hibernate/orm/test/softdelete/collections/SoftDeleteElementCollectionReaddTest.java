/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.softdelete.collections;

import java.util.LinkedHashSet;
import java.util.Set;

import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;
import org.hibernate.mapping.Index;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel( annotatedClasses = {
		SoftDeleteElementCollectionReaddTest.TimestampSoftDeleteCollectionOwner.class,
		SoftDeleteElementCollectionReaddTest.ActiveSoftDeleteCollectionOwner.class,
		SoftDeleteElementCollectionReaddTest.DeletedSoftDeleteCollectionOwner.class
} )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-20585" )
public class SoftDeleteElementCollectionReaddTest {
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
	void booleanSoftDeleteCollectionTablesUseUniqueIndexWithLiveRowExpression(DomainModelScope scope) {
		assertLiveRowIndex( scope, ActiveSoftDeleteCollectionOwner.class, "active", "true" );
		assertLiveRowIndex( scope, DeletedSoftDeleteCollectionOwner.class, "deleted", "false" );
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

	private static void assertLiveRowIndex(
			DomainModelScope scope,
			Class<?> collectionOwner,
			String softDeleteColumnName,
			String liveRowLiteral) {
		final org.hibernate.mapping.Table table = scope.getDomainModel()
				.getCollectionBinding( collectionOwner.getName() + ".mobiles" )
				.getCollectionTable();

		assertThat( table.getUniqueKeys() ).isEmpty();
		assertThat( table.getIndexes().values() )
				.singleElement()
				.satisfies( index -> assertLiveRowIndex( index, softDeleteColumnName, liveRowLiteral ) );
	}

	private static void assertLiveRowIndex(Index index, String softDeleteColumnName, String liveRowLiteral) {
		assertThat( index.isUnique() ).isTrue();
		assertThat( index.getSelectables() ).hasSize( 3 );
		assertThat( index.getSelectables().get( 2 ).getText() )
				.contains( "case when", softDeleteColumnName, liveRowLiteral );
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
