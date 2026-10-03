/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.audit;

import java.util.List;

import java.time.Instant;

import org.hibernate.annotations.Audited;
import org.hibernate.annotations.Changelog;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for HHH-20853: @Audited.Table on entity should not be reused as audit table for @ElementCollection
 *
 * @author Chris Cranford
 */
@DomainModel(annotatedClasses = {
		ElementCollectionWithCustomAuditTableTest.Owner.class,
		ElementCollectionWithCustomAuditTableTest.RevisionInfo.class
})
@SessionFactory
public class ElementCollectionWithCustomAuditTableTest {

	@Test
	public void testElementCollectionHasSeparateAuditTable(SessionFactoryScope scope) {
		// Element collection should have its own separate audit table
		final var collection = scope.getMetadataImplementor().getCollectionBinding(
				Owner.class.getName() + ".names"
		);
		final var collectionAuditTable = collection.getAuxiliaryTable();

		assertThat( collectionAuditTable )
				.as( "Element collection should have its own audit table" )
				.isNotNull();

		assertThat( collectionAuditTable.getName() )
				.as( "Element collection audit table should have default name" )
				.isEqualTo( "owner_names_AUD" );

		// Entity audit table should NOT contain element collection columns
		final var ownerEntity = scope.getMetadataImplementor().getEntityBinding( Owner.class.getName() );
		final var entityAuditTable = ownerEntity.getAuxiliaryTable();

		assertThat( entityAuditTable.getName() )
				.as( "Entity audit table should use custom name" )
				.isEqualTo( "owner_log" );

		// Verify entity audit table only has entity columns (not collection columns)
		final var entityAuditColumnNames = entityAuditTable.getColumns().stream()
				.map( col -> col.getName().toUpperCase() )
				.toList();

		assertThat( entityAuditColumnNames )
				.as( "Entity audit table should only have entity columns" )
				.contains( "REVTYPE", "ID", "REV" )
				.doesNotContain( "OWNER_ID", "NAMES" );
	}

	@Entity(name = "Owner")
	@Audited
	@Audited.Table(name = "owner_log")
	@Table(name = "owner")
	public static class Owner {
		@Id
		@Column(name = "id")
		private Long id;

		@ElementCollection
		@Audited
		@CollectionTable(name = "owner_names")
		private List<String> names;

		public Owner() {
		}

		public Owner(Long id) {
			this.id = id;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public List<String> getNames() {
			return names;
		}

		public void setNames(List<String> names) {
			this.names = names;
		}
	}

	@Entity(name = "RevisionInfo")
	@Table(name = "REVINFO")
	@Changelog
	public static class RevisionInfo {
		@Id
		@GeneratedValue
		@Changelog.ChangesetId
		@Column(name = "REV")
		Long id;

		@Changelog.Timestamp
		@Column(name = "REVTSTMP")
		Instant timestamp;
	}
}
