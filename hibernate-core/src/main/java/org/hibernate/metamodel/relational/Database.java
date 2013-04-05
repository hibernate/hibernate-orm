/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.metamodel.relational;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.MappingException;
import org.hibernate.dialect.Dialect;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.metamodel.Metadata;

/**
 * Represents a database and manages the named schema/catalog pairs defined within.
 *
 * @author Steve Ebersole
 * @author Gail Badner
 */
public class Database {
	private final Schema.Name implicitSchemaName;

	private final Map<Schema.Name,Schema> schemaMap = new HashMap<Schema.Name, Schema>();
	private final List<AuxiliaryDatabaseObject> auxiliaryDatabaseObjects = new ArrayList<AuxiliaryDatabaseObject>();

	public Database(Metadata.Options options) {
		String schemaName = options.getDefaultSchemaName();
		String catalogName = options.getDefaultCatalogName();
		if ( options.isGloballyQuotedIdentifiers() ) {
			schemaName = StringHelper.quote( schemaName );
			catalogName = StringHelper.quote( catalogName );
		}
		implicitSchemaName = new Schema.Name( schemaName, catalogName );
		makeSchema( implicitSchemaName );
	}

	public Schema getDefaultSchema() {
		return schemaMap.get( implicitSchemaName );
	}

	public Schema locateSchema(Schema.Name name) {
		if ( name.getSchema() == null && name.getCatalog() == null ) {
			return getDefaultSchema();
		}
		Schema schema = schemaMap.get( name );
		if ( schema == null ) {
			schema = makeSchema( name );
		}
		return schema;
	}

	private Schema makeSchema(Schema.Name name) {
		Schema schema;
		schema = new Schema( name );
		schemaMap.put( name, schema );
		return schema;
	}

	public Schema getSchema(Identifier schema, Identifier catalog) {
		return locateSchema( new Schema.Name( schema, catalog ) );
	}

	public Schema getSchema(String schema, String catalog) {
		return locateSchema( new Schema.Name( Identifier.toIdentifier( schema ), Identifier.toIdentifier( catalog ) ) );
	}

	public void addAuxiliaryDatabaseObject(AuxiliaryDatabaseObject auxiliaryDatabaseObject) {
		if ( auxiliaryDatabaseObject == null ) {
			throw new IllegalArgumentException( "Auxiliary database object is null." );
		}
		auxiliaryDatabaseObjects.add( auxiliaryDatabaseObject );
	}

	public Iterable<AuxiliaryDatabaseObject> getAuxiliaryDatabaseObjects() {
		return auxiliaryDatabaseObjects;
	}

	public String[] generateSchemaCreationScript(Dialect dialect) {
		Set<String> exportIdentifiers = new HashSet<String>( 50 );
		List<String> script = new ArrayList<String>( 50 );

		for ( Schema schema : schemaMap.values() ) {
			// TODO: create schema/catalog???
			for ( Table table : schema.getTables() ) {
				addSqlCreateStrings( dialect, exportIdentifiers, script, table );
			}
		}

		for ( Schema schema : schemaMap.values() ) {
			for ( Table table : schema.getTables() ) {

				if ( ! dialect.supportsUniqueConstraintInCreateAlterTable() ) {
					for  ( UniqueKey uniqueKey : table.getUniqueKeys() ) {
						addSqlCreateStrings( dialect, exportIdentifiers, script, uniqueKey );
					}
				}

				for ( Index index : table.getIndexes() ) {
					addSqlCreateStrings( dialect, exportIdentifiers, script, index );
				}

				if ( dialect.hasAlterTable() ) {
					for ( ForeignKey foreignKey : table.getForeignKeys() ) {
						// only add the foreign key if its target is a physical table
						if ( Table.class.isInstance( foreignKey.getTargetTable() ) ) {
							addSqlCreateStrings( dialect, exportIdentifiers, script, foreignKey );
						}
					}
				}

			}
		}

		// TODO: add sql create strings from PersistentIdentifierGenerator.sqlCreateStrings()

		for ( AuxiliaryDatabaseObject auxiliaryDatabaseObject : auxiliaryDatabaseObjects ) {
			if ( auxiliaryDatabaseObject.appliesToDialect( dialect ) ) {
				addSqlCreateStrings( dialect, exportIdentifiers, script, auxiliaryDatabaseObject );
			}
		}

		return ArrayHelper.toStringArray( script );
	}

	public String[] generateDropSchemaScript(Dialect dialect) {
		Set<String> exportIdentifiers = new HashSet<String>( 50 );
		List<String> script = new ArrayList<String>( 50 );


		// drop them in reverse order in case db needs it done that way...
		for ( int i = auxiliaryDatabaseObjects.size() - 1 ; i >= 0 ; i-- ) {
			AuxiliaryDatabaseObject object = auxiliaryDatabaseObjects.get( i );
			if ( object.appliesToDialect( dialect ) ) {
				addSqlDropStrings( dialect, exportIdentifiers, script, object );
			}
		}

		if ( dialect.dropConstraints() ) {
			for ( Schema schema : schemaMap.values() ) {
				for ( Table table : schema.getTables() ) {
					for ( ForeignKey foreignKey : table.getForeignKeys() ) {
						// only include foreign key if the target table is physical
						if ( foreignKey.getTargetTable() instanceof Table ) {
							addSqlDropStrings( dialect, exportIdentifiers, script, foreignKey );
						}
					}
				}
			}
		}

		for ( Schema schema : schemaMap.values() ) {
			for ( Table table : schema.getTables() ) {
				addSqlDropStrings( dialect, exportIdentifiers, script, table );
			}
		}

		// TODO: add sql drop strings from PersistentIdentifierGenerator.sqlCreateStrings()

		// TODO: drop schemas/catalogs???

		return ArrayHelper.toStringArray( script );
	}

	private static void addSqlDropStrings(
			Dialect dialect,
			Set<String> exportIdentifiers,
			List<String> script,
			Exportable exportable) {
		addSqlStrings(
				exportIdentifiers, script, exportable.getExportIdentifier(), exportable.sqlDropStrings( dialect )
		);
	}

	private static void addSqlCreateStrings(
			Dialect dialect,
			Set<String> exportIdentifiers,
			List<String> script,
			Exportable exportable) {
		addSqlStrings(
				exportIdentifiers, script, exportable.getExportIdentifier(), exportable.sqlCreateStrings( dialect )
		);
	}

	private static void addSqlStrings(
			Set<String> exportIdentifiers,
			List<String> script,
			String exportIdentifier,
			String[] sqlStrings) {
		if ( sqlStrings == null ) {
			return;
		}
		if ( exportIdentifiers.contains( exportIdentifier ) ) {
			throw new MappingException(
					"SQL strings added more than once for: " + exportIdentifier
			);
		}
		exportIdentifiers.add( exportIdentifier );
		script.addAll( Arrays.asList( sqlStrings ) );
	}
}
