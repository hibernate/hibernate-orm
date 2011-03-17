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

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a named schema/catalog pair and manages objects defined within.
 *
 * @author Steve Ebersole
 */
public class Schema {
	public static class Name {
		private final Identifier schema;
		private final Identifier catalog;

		public Name(Identifier schema, Identifier catalog) {
			this.schema = schema;
			this.catalog = catalog;
		}

		public Name(String schema, String catalog) {
			this( Identifier.toIdentifier( schema ), Identifier.toIdentifier( catalog ) );
		}

		public Identifier getSchema() {
			return schema;
		}

		public Identifier getCatalog() {
			return catalog;
		}
	}

	private final Name name;

	public Schema(Name name) {
		this.name = name;
	}

	public Schema(Identifier schema, Identifier catalog) {
		this( new Name( schema, catalog ) );
	}

	public Name getName() {
		return name;
	}

	private Map<Identifier,Table> tables = new HashMap<Identifier, Table>();

	public Table getTable(Identifier name) {
		return tables.get( name );
	}

	public Table createTable(Identifier name) {
		Table table = new Table( this, name );
		tables.put( name, table );
		return table;
	}

	private Map<String,InLineView> inLineViews = new HashMap<String, InLineView>();

	public InLineView getInLineView(String logicalName) {
		return inLineViews.get( logicalName );
	}

	public InLineView createInLineView(String logicalName, String subSelect) {
		InLineView inLineView = new InLineView( this, logicalName, subSelect );
		inLineViews.put( logicalName, inLineView );
		return inLineView;
	}
}
