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
package org.hibernate.metamodel.spi.relational;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.internal.util.StringHelper;

/**
 * Represents a database and manages the named schema/catalog pairs defined within.
 *
 * @author Steve Ebersole
 * @author Gail Badner
 */
public class Database {
	public static interface Defaults {
		String getDefaultSchemaName();
		String getDefaultCatalogName();
		boolean isGloballyQuotedIdentifiers();
	}

	private final Schema.Name implicitSchemaName;
	private final JdbcEnvironment jdbcEnvironment;

	private final Map<Schema.Name,Schema> schemaMap = new HashMap<Schema.Name, Schema>();
	private final List<AuxiliaryDatabaseObject> auxiliaryDatabaseObjects = new ArrayList<AuxiliaryDatabaseObject>();
	private final List<InitCommand> initCommands = new ArrayList<InitCommand>();

	public Database(Defaults options, JdbcEnvironment jdbcEnvironment) {
		String schemaName = options.getDefaultSchemaName();
		String catalogName = options.getDefaultCatalogName();
		if ( options.isGloballyQuotedIdentifiers() ) {
			schemaName = StringHelper.quote( schemaName );
			catalogName = StringHelper.quote( catalogName );
		}
		this.implicitSchemaName = new Schema.Name( catalogName, schemaName );
		makeSchema( implicitSchemaName );
		this.jdbcEnvironment = jdbcEnvironment;
	}

	public JdbcEnvironment getJdbcEnvironment() {
		return jdbcEnvironment;
	}

	public Iterable<Schema> getSchemas() {
		return schemaMap.values();
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

	public Schema getSchemaFor(ObjectName objectName) {
		return getSchema( objectName.getCatalog(), objectName.getSchema() );
	}

	private Schema makeSchema(Schema.Name name) {
		Schema schema;
		schema = new Schema( name );
		schemaMap.put( name, schema );
		return schema;
	}

	public Schema getSchema(Identifier catalog, Identifier schema) {
		return locateSchema( new Schema.Name( catalog, schema ) );
	}

	public Schema getSchema(String catalog, String schema) {
		return locateSchema( new Schema.Name( Identifier.toIdentifier( catalog ), Identifier.toIdentifier( schema ) ) );
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

	public void addInitCommand(InitCommand initCommand) {
		initCommands.add( initCommand );
	}

	public Iterable<InitCommand> getInitCommands() {
		return initCommands;
	}
}
