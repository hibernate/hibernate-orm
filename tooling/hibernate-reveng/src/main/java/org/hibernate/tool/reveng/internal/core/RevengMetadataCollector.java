/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.core;

import java.util.Objects;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.mapping.PhysicalTable;
import org.hibernate.relational.naming.spi.QualifiedPhysicalName;
import org.hibernate.relational.naming.spi.LogicalName;
import org.hibernate.relational.naming.spi.PhysicalName;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Table;
import org.hibernate.tool.reveng.api.core.TableIdentifier;
import org.hibernate.tool.reveng.internal.util.TableNameQualifier;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class RevengMetadataCollector {

	public PhysicalName columnName(String spelling) { return physicalName( spelling, physicalNameFactory ); }

	public PhysicalName observedColumnName(String spelling, boolean requestedQuoting) {
		final var policy = physicalNameFactory.getComparisonPolicy();
		return physicalNameFactory.create( spelling, requestedQuoting
				|| !policy.toDatabaseName( spelling, false ).equals( spelling ) );
	}

	public org.hibernate.mapping.Column findObservedColumn(Table table, String name) {
		return table.getColumnByDatabaseName( name, physicalNameFactory.getComparisonPolicy() );
	}

	private MetadataBuildingContext metadataBuildingContext = null;
	private String defaultCatalog;
	private String defaultSchema;
	private final PhysicalName.Factory physicalNameFactory;
	private final Map<TableIdentifier, Table> tables;
	private Map<String, List<ForeignKey>> oneToManyCandidates;
	private final Map<TableIdentifier, String> suggestedIdentifierStrategies;

	public RevengMetadataCollector(MetadataBuildingContext metadataBuildingContext, String defaultCatalog, String defaultSchema) {
		this( metadataBuildingContext.getMetadataCollector().getDatabase()
				.getJdbcEnvironment().getIdentifierHelper().getPhysicalNameFactory() );
		this.metadataBuildingContext = metadataBuildingContext;
		this.defaultCatalog = defaultCatalog;
		this.defaultSchema = defaultSchema;
		metadataBuildingContext.getMetadataCollector().getDatabase().setPhysicalImplicitNamespaceName(
				new org.hibernate.boot.model.relational.PhysicalNamespaceName(
						physicalName( defaultCatalog, physicalNameFactory ),
						physicalName( defaultSchema, physicalNameFactory ) ) );
	}

	public RevengMetadataCollector(PhysicalName.Factory physicalNameFactory) {
		this.physicalNameFactory = Objects.requireNonNull( physicalNameFactory );
		this.tables = new HashMap<>();
		this.suggestedIdentifierStrategies = new HashMap<>();
	}

	public Iterator<Table> iterateTables() {
		return tables.values().iterator();
	}

	// TableIdentifier's catalog, schema and name should be quoted
	public Table addTable(TableIdentifier tableIdentifier) {
		Table result;
		String catalog = tableIdentifier.getCatalog();
		String schema = tableIdentifier.getSchema();
		String name = tableIdentifier.getName();
		if (tables.containsKey(tableIdentifier)) {
			throw new RuntimeException(
					"Attempt to add a double entry for table: " +
					TableNameQualifier.qualify(catalog, schema, name));
		}
		InFlightMetadataCollector metadataCollector = getMetadataCollector();
		if (metadataCollector != null) {
			final var database = metadataCollector.getDatabase();
			final var factory = database.getJdbcEnvironment().getIdentifierHelper().getPhysicalNameFactory();
			final var physicalCatalog = physicalName( withoutDefault( catalog, defaultCatalog ), factory );
			final var physicalSchema = physicalName( withoutDefault( schema, defaultSchema ), factory );
			final var physicalName = physicalName( name, factory );
			final var namespace = database.locatePhysicalNamespace( physicalCatalog, physicalSchema );
			final var lookupName = new LogicalName(
					physicalName.getText(), physicalName.isQuoted(), true );
			final var existing = namespace.locateTable( lookupName );
			if ( existing != null ) {
				result = existing;
			}
			else {
				result = new PhysicalTable( "Hibernate Tools",
						new QualifiedPhysicalName( physicalCatalog, physicalSchema, physicalName ), false );
				namespace.registerTable( lookupName, result );
			}
		}
		else {
			result = createTable(catalog, schema, name);
		}

		tables.put(tableIdentifier, result);
		return result;
	}

	public Table getTable(TableIdentifier tableIdentifier) {
		return tables.get(tableIdentifier);
	}

	public Collection<Table> getTables() {
		return tables.values();
	}

	public void setOneToManyCandidates(Map<String, List<ForeignKey>> oneToManyCandidates) {
		this.oneToManyCandidates = oneToManyCandidates;
	}

	public Map<String, List<ForeignKey>> getOneToManyCandidates() {
		return oneToManyCandidates;
	}

	public String getSuggestedIdentifierStrategy(String catalog, String schema, String name) {
		return suggestedIdentifierStrategies.get(TableIdentifier.create(catalog, schema, name));
	}

	public void addSuggestedIdentifierStrategy(String catalog, String schema, String name, String idstrategy) {
		suggestedIdentifierStrategies.put(TableIdentifier.create(catalog, schema, name), idstrategy);
	}

	private static String withoutDefault(String name, String defaultName) {
		final var identifier = Identifier.toIdentifier( name );
		return identifier != null && identifier.getText().equals( defaultName ) ? null : name;
	}

	private static PhysicalName physicalName(
			String name, PhysicalName.Factory factory) {
		final var identifier = Identifier.toIdentifier( name );
		return identifier == null ? null : factory.create( identifier.getText(), identifier.isQuoted() );
	}

	private Table createTable(String catalog, String schema, String name) {
		return new PhysicalTable( "Hibernate Tools",
				new QualifiedPhysicalName(
						physicalName( catalog, physicalNameFactory ),
						physicalName( schema, physicalNameFactory ),
						physicalName( name, physicalNameFactory ) ), false );
	}

	private InFlightMetadataCollector getMetadataCollector() {
		InFlightMetadataCollector result = null;
		if (metadataBuildingContext != null) {
			result = metadataBuildingContext.getMetadataCollector();
		}
		return result;
	}

}
