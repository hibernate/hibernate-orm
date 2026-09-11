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
 * Test for HHH-20853: @Audited.CollectionTable should allow customizing element collection audit table names
 *
 * @author Chris Cranford
 */
@DomainModel(annotatedClasses = {
		ElementCollectionWithCollectionAuditTableTest.Product.class,
		ElementCollectionWithCollectionAuditTableTest.RevisionInfo.class
})
@SessionFactory
public class ElementCollectionWithCollectionAuditTableTest {

	@Test
	public void testElementCollectionWithCustomAuditTableName(SessionFactoryScope scope) {
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

			// Custom collection audit table should be created
			assertThat( tableNames )
					.as( "Custom collection audit table should be created" )
					.contains( "PRODUCT_TAGS_AUDIT_LOG" )
					.doesNotContain( "PRODUCT_TAGS_AUD" ); // Default name should not exist
		} );
	}

	@Entity(name = "Product")
	@Audited
	@Audited.Table(name = "product_log")
	@Table(name = "product")
	public static class Product {
		@Id
		@Column(name = "id")
		private Long id;

		@ElementCollection
		@Audited
		@Audited.CollectionTable(name = "product_tags_audit_log")
		@CollectionTable(name = "product_tags")
		private List<String> tags;

		public Product() {
		}

		public Product(Long id) {
			this.id = id;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public List<String> getTags() {
			return tags;
		}

		public void setTags(List<String> tags) {
			this.tags = tags;
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
