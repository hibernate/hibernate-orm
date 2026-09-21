/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Set;

import org.hibernate.boot.serial.MetadataSerialization;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.mapping.DenormalizedTable;
import org.hibernate.mapping.NamedTable;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.SecondaryTable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/// Restoration reaches named tables through mapping links even without namespace registration.
///
/// @author Steve Ebersole
@DomainModel(annotatedClasses = { TableNameArchiveTest.Record.class, TableNameArchiveTest.Base.class, TableNameArchiveTest.Child.class })
@ServiceRegistry(settings = @Setting(name = MappingSettings.METADATA_SERIALIZATION_ENABLED, value = "true"))
class TableNameArchiveTest {
	@Test
	void restoresTablesOutsideNamespaceRegistry(DomainModelScope scope) {
		var metadata = scope.getDomainModel();
		final var registry = metadata.getMappingResolutionOptions().getServiceRegistry();
		metadata.getDatabase().getNamespaces().forEach( namespace -> namespace.getTables().clear() );
		final var bytes = new ByteArrayOutputStream();
		MetadataSerialization.serialize( metadata ).writeTo( bytes );
		for ( int i = 0; i < 2; i++ ) {
			metadata = (org.hibernate.boot.spi.MetadataImplementor) MetadataSerialization.read( new ByteArrayInputStream( bytes.toByteArray() ) )
					.restore( registry ).getMetadata();
			final var record = metadata.getEntityBinding( Record.class.getName() );
			assertNotNull( ((NamedTable) record.getTable()).getPhysicalName() );
			assertNotNull( ((NamedTable) record.getJoins().get( 0 ).getTable()).getPhysicalName() );
			final var collection = metadata.getCollectionBinding( Record.class.getName() + ".tags" );
			assertNotNull( ((NamedTable) collection.getCollectionTable()).getPhysicalName() );
			final var base = metadata.getEntityBinding( Base.class.getName() );
			final var child = (DenormalizedTable) metadata.getEntityBinding( Child.class.getName() ).getTable();
			assertNotNull( child.getPhysicalName() );
			assertNotNull( ((NamedTable) child.getIncludedTable()).getPhysicalName() );
			assertSame( base.getTable(), child.getIncludedTable() );
			assertSame( base.getTable().getPrimaryKey(), child.getPrimaryKey() );
		}
	}

	@Test
	void filterAliasesRestorePhysicalNamesWithoutRetainingPolicies(DomainModelScope scope) {
		final var entity = scope.getDomainModel().getEntityBinding( Record.class.getName() );
		final var sourceName = ((NamedTable) entity.getTable()).getPhysicalName();
		final var factory = new org.hibernate.relational.naming.spi.PhysicalName.Factory( (text, quoted) -> "restored:" + text );
		final var expected = org.hibernate.relational.naming.internal.QualifiedPhysicalNameSnapshot.from( sourceName ).restore( factory );
		final var context = org.mockito.Mockito.mock( org.hibernate.boot.model.relational.SqlStringGenerationContext.class );
		org.mockito.Mockito.when( context.getPhysicalNameFactory() ).thenReturn( factory );
		org.mockito.Mockito.when( context.format( expected ) ).thenReturn( "restored_table" );
		final var filter = new org.hibernate.mapping.FilterConfiguration( "filter", "id > 0", true, null, null, entity );
		final var restored = (org.hibernate.mapping.FilterConfiguration) org.hibernate.internal.util.SerializationHelper.clone( filter );
		assertEquals( "restored_table", restored.getAliasTableMap( null, context ).get( null ) );

		final var inlineEntity = org.mockito.Mockito.mock( org.hibernate.mapping.RootClass.class );
		org.mockito.Mockito.when( inlineEntity.getTable() ).thenReturn( new org.hibernate.mapping.InlineView( "orm",
				new org.hibernate.relational.naming.spi.LogicalName( "report", false, false ), "select id from records" ) );
		final var inlineFilter = new org.hibernate.mapping.FilterConfiguration( "filter", "id > 0", true, null, null, inlineEntity );
		final var restoredInline = (org.hibernate.mapping.FilterConfiguration) org.hibernate.internal.util.SerializationHelper.clone( inlineFilter );
		assertEquals( "( select id from records )", restoredInline.getAliasTableMap( null, context ).get( null ) );
	}

	@Entity(name = "ArchiveRecord")
	@SecondaryTable(name = "archive_details")
	static class Record {
		@Id Integer id;
		@Column(table = "archive_details") String detail;
		@ElementCollection Set<String> tags;
	}

	@Entity(name = "ArchiveBase")
	@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
	static abstract class Base {
		@Id Integer id;
	}

	@Entity(name = "ArchiveChild")
	static class Child extends Base {
		String description;
	}
}
