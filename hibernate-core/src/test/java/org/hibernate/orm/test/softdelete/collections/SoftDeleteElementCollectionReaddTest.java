/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.softdelete.collections;

import java.util.LinkedHashSet;
import java.util.Set;

import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;
import org.hibernate.testing.orm.junit.DomainModel;
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

@DomainModel( annotatedClasses = SoftDeleteElementCollectionReaddTest.SoftDeleteCollectionOwner.class )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-20585" )
public class SoftDeleteElementCollectionReaddTest {
	@Test
	void readdingPreviouslySoftDeletedElementCollectionEntry(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final SoftDeleteCollectionOwner owner = new SoftDeleteCollectionOwner( 1L );
			owner.getMobiles().addAll( Set.of( "1", "2" ) );
			session.persist( owner );
		} );

		scope.inTransaction( session -> {
			final SoftDeleteCollectionOwner owner = session.get( SoftDeleteCollectionOwner.class, 1L );
			owner.getMobiles().clear();
			owner.getMobiles().addAll( Set.of( "2", "3" ) );
		} );

		scope.inTransaction( session -> {
			final SoftDeleteCollectionOwner owner = session.get( SoftDeleteCollectionOwner.class, 1L );
			owner.getMobiles().clear();
			owner.getMobiles().addAll( Set.of( "1", "3" ) );
		} );

		scope.inTransaction( session -> {
			final SoftDeleteCollectionOwner owner = session.get( SoftDeleteCollectionOwner.class, 1L );
			assertThat( owner.getMobiles() ).containsExactlyInAnyOrder( "1", "3" );
		} );
	}

	@Entity( name = "SoftDeleteCollectionOwner" )
	@Table( name = "soft_delete_collection_owner" )
	static class SoftDeleteCollectionOwner {
		@Id
		private Long id;

		@ElementCollection
		@CollectionTable( name = "soft_delete_collection_owner_mobile", joinColumns = @JoinColumn( name = "owner_id" ) )
		@SoftDelete( columnName = "deleted_at", strategy = SoftDeleteType.TIMESTAMP )
		private Set<String> mobiles = new LinkedHashSet<>();

		SoftDeleteCollectionOwner() {
		}

		SoftDeleteCollectionOwner(Long id) {
			this.id = id;
		}

		Set<String> getMobiles() {
			return mobiles;
		}
	}
}
