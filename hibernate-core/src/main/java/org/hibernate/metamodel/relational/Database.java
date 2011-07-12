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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a database and manages the named schema/catalog pairs defined within.
 *
 * @author Steve Ebersole
 */
public class Database {
	private Map<Schema.Name,Schema> schemaMap = new HashMap<Schema.Name, Schema>();
	private final List<AuxiliaryDatabaseObject> auxiliaryDatabaseObjects = new ArrayList<AuxiliaryDatabaseObject>();

	public Schema getSchema(Schema.Name name) {
		Schema schema = schemaMap.get( name );
		if ( schema == null ) {
			schema = new Schema( name );
			schemaMap.put( name, schema );
		}
		return schema;
	}

	public Schema getSchema(Identifier schema, Identifier catalog) {
		return getSchema( new Schema.Name( schema, catalog ) );
	}

	public Schema getSchema(String schema, String catalog) {
		return getSchema( new Schema.Name( Identifier.toIdentifier( schema ), Identifier.toIdentifier( catalog ) ) );
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
}
