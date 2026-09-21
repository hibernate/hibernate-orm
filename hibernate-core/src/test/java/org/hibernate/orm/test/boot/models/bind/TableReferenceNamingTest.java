/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.bind;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.annotations.Subselect;
import org.hibernate.annotations.View;
import org.hibernate.boot.mapping.internal.relational.InLineView;
import org.hibernate.boot.mapping.internal.relational.PhysicalTable;
import org.hibernate.boot.mapping.internal.relational.PhysicalTableReference;
import org.hibernate.boot.mapping.internal.relational.PhysicalView;
import org.hibernate.boot.mapping.internal.relational.SecondaryTable;
import org.hibernate.boot.mapping.internal.relational.TableReference;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.boot.model.naming.spi.PhysicalNamingContext;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.relational.naming.spi.LogicalName;
import org.hibernate.relational.naming.spi.PhysicalName;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.testing.orm.junit.SettingProvider;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/// Binding references retain source identity and reuse finalized physical names.
///
/// @author Steve Ebersole
@ServiceRegistry(settingProviders = @SettingProvider(
		settingName = AvailableSettings.PHYSICAL_NAMING_STRATEGY,
		provider = TableReferenceNamingTest.StrategyProvider.class))
class TableReferenceNamingTest {
	@Test
	void referencesReuseResolvedNames(ServiceRegistryScope scope) {
		BindingTestingHelper.checkDomainModel( context -> {
			final var state = context.getBindingState();
			final var strategy = (CountingStrategy) state.getDatabase().getPhysicalNamingStrategy();
			final PhysicalTable primary = state.getTableByName( name( "records" ) );
			final SecondaryTable secondary = state.getTableByName( name( "details" ) );
			final PhysicalTable implicit = state.getTableByName( name( "ImplicitRecord" ) );
			final PhysicalView view = state.getTableByName( name( "report" ) );
			final InLineView inline = state.getTableByName( name( "InlineRecord" ) );
			assertTrue( primary.logicalName().isExplicit() );
			assertFalse( implicit.logicalName().isExplicit() );
			assertNull( implicit.getLogicalSchemaName() );
			assertEquals( "strategy_schema", implicit.getPhysicalSchemaName().getText() );
			assertNull( view.getLogicalSchemaName() );
			assertEquals( "strategy_schema", view.getPhysicalSchemaName().getText() );
			assertTrue( secondary.getLogicalSchemaName().isExplicit() );
			assertEquals( "app", secondary.getLogicalSchemaName().getText() );
			assertEquals( "p_app", secondary.getPhysicalSchemaName().getText() );
			assertSame( primary, state.getTableByName( name( "RECORDS" ) ) );
			assertNull( state.getTableByName( name( "p_records" ) ) );
			assertNull( state.getTableByName( new LogicalName( "records", true, true ) ) );
			assertNotNull( inline );
			assertFalse( inline.exportable() );
			assertInstanceOf( org.hibernate.mapping.InlineView.class, inline.binding() );
			assertFalse( org.hibernate.boot.model.relational.Exportable.class.isInstance( inline.binding() ) );
			assertInstanceOf( org.hibernate.mapping.DatabaseView.class, view.binding() );
			assertEquals( "p_report", view.physicalName().getText() );
			assertEquals( view.binding().getName(), view.physicalName().getText() );
			assertSame( view.binding().getPhysicalName().objectName(), view.physicalName() );
			assertFalse( strategy.tableCalls.containsKey( name( "InlineRecord" ) ) );
			assertEquals( 1, strategy.schemaCalls.get( name( "app" ) ) );

			final var collection = context.getMetadataCollector().getCollectionBinding( Record.class.getName() + ".tags" );
			final PhysicalTable tags = state.getTableByBinding( collection.getCollectionTable() );
			for ( PhysicalTableReference reference : List.of( primary, secondary, implicit, tags ) ) {
				assertInstanceOf( org.hibernate.mapping.PhysicalTable.class, reference.binding() );
				assertEquals( reference.binding().getName(), reference.getPhysicalTableName().getText() );
				assertSame( ((org.hibernate.mapping.NamedTable) reference.binding()).getPhysicalName().objectName(), reference.getPhysicalTableName() );
				assertEquals( 1, strategy.tableCalls.get( reference.logicalName() ) );
			}
			assertEquals( 1, strategy.tableCalls.get( view.logicalName() ) );
		}, scope.getRegistry(), Record.class, ImplicitRecord.class, Report.class, InlineRecord.class );
	}

	@Test
	void registryKeepsQuotedAndUnquotedKeysSeparate(ServiceRegistryScope scope) {
		BindingTestingHelper.checkDomainModel( context -> {
			final var state = context.getBindingState();
			final TableReference primary = state.getTableByName( name( "shared" ) );
			final TableReference secondary = state.getTableByName( new LogicalName( "shared", true, true ) );
			assertNotNull( primary );
			assertNotNull( secondary );
			assertNotSame( primary, secondary );
			assertSame( primary, state.getTableByName( name( "SHARED" ) ) );
			assertNull( state.getTableByName( new LogicalName( "SHARED", true, true ) ) );
			assertFalse( primary.binding().isQuoted() );
			assertTrue( secondary.binding().isQuoted() );
		}, scope.getRegistry(), QuotedSecondary.class );
	}

	private static LogicalName name(String text) {
		return new LogicalName( text, false, true );
	}

	public static class StrategyProvider implements SettingProvider.Provider<CountingStrategy> {
		@Override
		public CountingStrategy getSetting() {
			return new CountingStrategy();
		}
	}

	public static class CountingStrategy extends PhysicalNamingStrategyStandardImpl {
		final Map<LogicalName, Integer> tableCalls = new HashMap<>();
		final Map<LogicalName, Integer> schemaCalls = new HashMap<>();

		@Override
		public PhysicalName toPhysicalTableName(LogicalName name, PhysicalNamingContext context) {
			tableCalls.merge( name, 1, Integer::sum );
			return context.getPhysicalNameFactory().create( "p_" + name.getText(), name.isQuoted() );
		}

		@Override
		public PhysicalName toPhysicalSchemaName(LogicalName name, PhysicalNamingContext context) {
			if ( name == null ) {
				return context.getPhysicalNameFactory().create( "strategy_schema", false );
			}
			schemaCalls.merge( name, 1, Integer::sum );
			return context.getPhysicalNameFactory().create( "p_" + name.getText(), name.isQuoted() );
		}
	}

	@Entity(name = "Record")
	@Table(name = "records", schema = "app")
	@jakarta.persistence.SecondaryTable(name = "details", schema = "app")
	static class Record {
		@Id Integer id;
		@Column(table = "details") String detail;
		@ElementCollection
		@CollectionTable(name = "tags")
		List<String> tags;
	}

	@Entity(name = "ImplicitRecord")
	static class ImplicitRecord {
		@Id Integer id;
	}

	@Entity(name = "Report")
	@Table(name = "report")
	@View(query = "select id from records")
	static class Report {
		@Id Integer id;
	}

	@Entity(name = "InlineRecord")
	@Subselect("select id from records")
	static class InlineRecord {
		@Id Integer id;
	}

	@Entity(name = "QuotedSecondary")
	@Table(name = "shared")
	@jakarta.persistence.SecondaryTable(name = "`shared`")
	static class QuotedSecondary {
		@Id Integer id;
	}
}
