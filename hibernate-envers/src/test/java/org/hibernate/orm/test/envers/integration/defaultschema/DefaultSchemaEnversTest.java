/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.defaultschema;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.boot.Metadata;
import org.hibernate.envers.AuditJoinTable;
import org.hibernate.envers.AuditTable;
import org.hibernate.envers.Audited;
import org.hibernate.envers.CollectionAuditTable;
import org.hibernate.mapping.Table;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;

import static org.assertj.core.api.Assertions.assertThat;

@EnversTest
@DomainModel(annotatedClasses = {
		DefaultSchemaEnversTest.AuditedEntity.class,
		DefaultSchemaEnversTest.RelatedEntity.class,
		DefaultSchemaEnversTest.ExplicitAuditEntity.class
})
@SessionFactory(exportSchema = false)
public class DefaultSchemaEnversTest {
	private static final String PACKAGE_SCHEMA = "audit_schema";
	private static final String ENTITY_SCHEMA = "entity_schema";
	private static final String EXPLICIT_SCHEMA = "explicit_audit_schema";

	@Test
	void appliesPackageDefaultSchemaToAuditTable(DomainModelScope scope) {
		final Metadata domainModel = scope.getDomainModel();

		assertThat( domainModel.getEntityBinding( AuditedEntity.class.getName() + "_AUD" ).getTable().getSchema() )
				.isEqualTo( PACKAGE_SCHEMA );
		assertThat( domainModel.getEntityBinding( ExplicitAuditEntity.class.getName() + "_AUD" ).getTable().getSchema() )
				.isEqualTo( EXPLICIT_SCHEMA );
	}

	@Test
	void appliesPackageDefaultSchemaToAuditCollectionTables(DomainModelScope scope) {
		final Metadata domainModel = scope.getDomainModel();

		assertThat( findTable( domainModel, "audited_entity_tags_AUD" ).getSchema() ).isEqualTo( PACKAGE_SCHEMA );
		assertThat( findTable( domainModel, "audited_entity_relations_aud" ).getSchema() ).isEqualTo( PACKAGE_SCHEMA );
	}

	private static Table findTable(Metadata domainModel, String name) {
		for ( var namespace : domainModel.getDatabase().getNamespaces() ) {
			for ( Table table : namespace.getTables() ) {
				if ( name.equals( table.getName() ) ) {
					return table;
				}
			}
		}
		throw new AssertionError( "Could not find table " + name );
	}

	@Entity(name = "DefaultSchemaAuditedEntity")
	@jakarta.persistence.Table(name = "audited_entity", schema = ENTITY_SCHEMA)
	@Audited
	@AuditTable(value = "audited_entity_aud")
	public static class AuditedEntity {
		@Id
		private Integer id;

		private String name;

		@ElementCollection
		@CollectionTable(name = "audited_entity_tags", schema = ENTITY_SCHEMA)
		@CollectionAuditTable(name = "audited_entity_tags")
		private Set<String> tags = new HashSet<>();

		@ManyToMany
		@JoinTable(name = "audited_entity_relations", schema = ENTITY_SCHEMA)
		@AuditJoinTable(name = "audited_entity_relations_aud")
		private Set<RelatedEntity> related = new HashSet<>();
	}

	@Entity(name = "DefaultSchemaRelatedEntity")
	@jakarta.persistence.Table(name = "related_entity", schema = ENTITY_SCHEMA)
	@Audited
	public static class RelatedEntity {
		@Id
		private Integer id;
	}

	@Entity(name = "DefaultSchemaExplicitAuditEntity")
	@jakarta.persistence.Table(name = "explicit_audited_entity", schema = ENTITY_SCHEMA)
	@Audited
	@AuditTable(value = "explicit_audited_entity_aud", schema = EXPLICIT_SCHEMA)
	public static class ExplicitAuditEntity {
		@Id
		private Integer id;
	}
}
