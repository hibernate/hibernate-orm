/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.temporal.audit.auditoverrides;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.SharedSessionContract;
import org.hibernate.annotations.AuditOverride;
import org.hibernate.annotations.AuditOverrides;
import org.hibernate.annotations.Audited;
import org.hibernate.cfg.StateManagementSettings;
import org.hibernate.temporal.spi.ChangesetIdentifierSupplier;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


@SessionFactory
@DomainModel(annotatedClasses = {
		AuditOverrideCollectionTableTest.EntityWithOverrides.class,

		AuditOverrideCollectionTableTest.Other.class,
		AuditOverrideCollectionTableTest.EntityWithCollection.class,

		AuditOverrideCollectionTableTest.SubEntity.class,

})
@ServiceRegistry(settings = @Setting(name = StateManagementSettings.CHANGESET_ID_SUPPLIER,
		value = "org.hibernate.temporal.audit.AuditEntityTest$TxIdSupplier"))
public class AuditOverrideCollectionTableTest {
	private static int currentTxId;

	public static class TxIdSupplier implements ChangesetIdentifierSupplier<Integer> {
		@Override
		public Integer generateIdentifier(SharedSessionContract session) {
			return ++currentTxId;
		}
	}

	/**
	 * Multiple cases within a hierarchy of an entity and a @MappedSuperClass
	 * Case 1: firstCollection is initially excluded and then included again via an @AuditOverride
	 * Case 2: secondCollection is initially included and then excluded via an @AuditOverride
	 * Case 3 thirdCollection is initially included and then the audit table name is overridden via an @AuditOverride
	 *
	 */

	@MappedSuperclass
	@Audited
	static class MSCWithExcludedCollectionProperty {
		@Id
		long id;

		@ElementCollection
		@Audited.Excluded
		List<String> firstCollection;

		@ElementCollection
		List<String> secondCollection;

		@ElementCollection
		List<String> thirdCollection;
	}

	@Entity
	@Table(name = "EntityWithOverrides")
	@AuditOverrides(
			{@AuditOverride(name = "firstCollection", isAudited = true),
					@AuditOverride(name = "secondCollection", isAudited = false),
			@AuditOverride(name = "thirdCollection", collectionTable = @Audited.CollectionTable( catalog = "cat", schema = "schema", name = "custom_audited_join_table_name" ))} //TODO schema and catalog?
	)
	static class EntityWithOverrides extends MSCWithExcludedCollectionProperty{
	}

	@Test
	public void elementCollection(DomainModelScope domainModelScope) {
		var tables = domainModelScope.getDomainModel().collectTableMappings();
		var tableNames = tables.stream().map( org.hibernate.mapping.Table::getName ).collect( Collectors.toSet() );
		assertTrue( tableNames.contains( "AuditOverrideCollectionTableTest$EntityWithOverrides_firstCollection" ) );
		assertTrue( tableNames.contains( "AuditOverrideCollectionTableTest$EntityWithOverrides_firstCollection_AUD" ) );
		assertTrue( tableNames.contains( "AuditOverrideCollectionTableTest$EntityWithOverrides_secondCollection" ) );
		assertFalse( tableNames.contains( "AuditOverrideCollectionTableTest$EntityWithOverrides_secondCollection_AUD" ) );
		assertTrue( tables.contains( createTableObject( "cat", "schema", "custom_audited_join_table_name" ) ) );
	}

	/**
	 * Hierarchy with multiple Entities and multiple overrides of the audited join table
	 *
	 */

	@Entity
	static class Other {
		@Id
		long id;

		String otherProp;
	}

	@MappedSuperclass
	@Audited
	@Audited.Table(name = "EO_AUD")
	static class MSCWithCollection {
		@Id
		long id;

		@OneToMany //TODO @Elementcollection, manyToOne ?, ManyToMany ?
		@JoinColumn(name = "department_id")
		@Audited.CollectionTable( name = "custom_audit_collection_table" )
		List<Other> auditedCollection;
	}

	@Entity
	@Table(name = "EntityWithOverrides")
	@AuditOverrides(@AuditOverride(name = "auditedCollection", collectionTable = @Audited.CollectionTable( name = "overridden_aud")))
	static class EntityWithCollection extends MSCWithCollection{

	}

	@Entity
	@AuditOverrides(@AuditOverride(name = "auditedCollection", collectionTable = @Audited.CollectionTable( name = "double_overridden_aud", catalog = "mycatalog", schema = "myschema"  )))
	static class SubEntity extends EntityWithCollection{

	}

	@Test
	public void elementCollection2(DomainModelScope domainModelScope) {
		var tables = domainModelScope.getDomainModel().collectTableMappings();
		var tableNames = tables.stream().map( org.hibernate.mapping.Table::getName ).collect( Collectors.toSet() );
		assertFalse( tableNames.contains( "overridden_aud" ) );
		assertTrue( tables.contains( createTableObject( "mycatalog", "myschema", "double_overridden_aud" ) ) );
	}

	private static org.hibernate.mapping.Table createTableObject(String catalog, String schema, String tableName) {
		var table = new org.hibernate.mapping.Table();
		table.setCatalog( catalog );
		table.setSchema( schema );
		table.setName( tableName );
		return table;
	}

	private static void assertTable(Collection<org.hibernate.mapping.Table> tables, String tableName, Consumer<org.hibernate.mapping.Table> consumer) {
		var tableFound = false;
		for ( var table : tables ) {
			if ( table.getName().equals( tableName ) ) {
				tableFound = true;
				consumer.accept( table );
			}
		}
		assertTrue( tableFound, () -> "Table %s not found. Available tables: %s".formatted( tableName, tables ) );
	}

}
