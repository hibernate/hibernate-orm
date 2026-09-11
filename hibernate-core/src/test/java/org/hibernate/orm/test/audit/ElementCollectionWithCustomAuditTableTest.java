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
		scope.inSession( session -> {
			// Get all table names
			final List<String> tableNames = session.createNativeQuery(
					"""
					select table_name
					from information_schema.tables
					where table_schema = 'PUBLIC'
					order by table_name
					""",
					String.class
			).getResultList();

			// Get columns in the entity audit table
			final List<String> ownerLogColumns = session.createNativeQuery(
					"""
					select column_name
					from information_schema.columns
					where table_schema = 'PUBLIC'
					and table_name = 'OWNER_LOG'
					order by ordinal_position
					""",
					String.class
			).getResultList();

			// Element collection should have its own audit table
			assertThat( tableNames )
					.as( "Element collection audit table should be created" )
					.contains( "OWNER_NAMES_AUD" );

			// Entity audit table should NOT contain element collection columns
			assertThat( ownerLogColumns )
					.as( "Entity audit table should only have entity columns" )
					.containsExactlyInAnyOrder( "REVTYPE", "ID", "REV" )
					.doesNotContain( "OWNER_ID", "NAMES" );
		} );
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
