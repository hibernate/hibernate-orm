/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.internal;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ImplicitIndexNameSource;
import org.hibernate.boot.model.naming.ImplicitUniqueKeyNameSource;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.Table;

import java.util.List;

import static java.util.Collections.emptyList;
import static org.hibernate.boot.model.naming.Identifier.toIdentifier;
import static org.hibernate.internal.util.collections.CollectionHelper.arrayList;

class IndexOrUniqueKeyNameSource implements ImplicitIndexNameSource, ImplicitUniqueKeyNameSource {
	private final MetadataBuildingContext buildingContext;
	private final Table table;
	private final String[] columnNames;
	private final String originalKeyName;

	public IndexOrUniqueKeyNameSource(
			MetadataBuildingContext buildingContext, Table table, String[] columnNames, String originalKeyName) {
		this.buildingContext = buildingContext;
		this.table = table;
		this.columnNames = columnNames;
		this.originalKeyName = originalKeyName;
	}

	@Override
	public MetadataBuildingContext getBuildingContext() {
		return buildingContext;
	}

	@Override
	public Identifier getTableName() {
		return table.getNameIdentifier();
	}

	private List<Identifier> columnNameIdentifiers;

	@Override
	public List<Identifier> getColumnNames() {
		// be lazy about building these
		if ( columnNameIdentifiers == null ) {
			columnNameIdentifiers = toIdentifiers( columnNames );
		}
		return columnNameIdentifiers;
	}

	@Override
	public Identifier getUserProvidedIdentifier() {
		return originalKeyName != null ? toIdentifier( originalKeyName ) : null;
	}

	private List<Identifier> toIdentifiers(String[] names) {
		if ( names == null ) {
			return emptyList();
		}

		final List<Identifier> columnNames = arrayList( names.length );
		for ( String name : names ) {
			columnNames.add( buildingContext.getMetadataCollector().getDatabase().toIdentifier( name ) );
		}
		return columnNames;
	}
}
