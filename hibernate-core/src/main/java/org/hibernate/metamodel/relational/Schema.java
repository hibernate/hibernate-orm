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
	public static final String EXPORTABLE_SCHEMA = "exportable_schema";
	
	private final Name name;
	private Map<String, InLineView> inLineViews = new HashMap<String, InLineView>();
	private Map<Identifier, Table> tables = new HashMap<Identifier, Table>();
	private final Map<Identifier, Sequence> sequences = new HashMap<Identifier, Sequence>();

	public Schema(Name name) {
		this.name = name;
	}

	public Schema(Identifier schema, Identifier catalog) {
		this( new Name( schema, catalog ) );
	}

	public Name getName() {
		return name;
	}

	public Table locateTable(Identifier name) {
		return tables.get( name );
	}

	public Table createTable(Identifier name) {
		Table table = new Table( this, name );
		tables.put( name, table );
		return table;
	}

	public Table locateOrCreateTable(Identifier name) {
		final Table existing = locateTable( name );
		if ( existing == null ) {
			return createTable( name );
		}
		return existing;
	}

	public Iterable<Table> getTables() {
		return tables.values();
	}

	public Sequence locateSequence(Identifier name) {
		return sequences.get( name );
	}

	public Sequence createSequence(Identifier name) {
		Sequence sequence = new Sequence( this, name.getName() );
		sequences.put( name, sequence );
		return sequence;
	}

	public Sequence createSequence(Identifier name, int initialValue, int incrementSize) {
		Sequence sequence = new Sequence( this, name.getName(), initialValue, initialValue );
		sequences.put( name, sequence );
		return sequence;
	}

	public Sequence locateOrCreateSequence(Identifier name) {
		final Sequence existing = locateSequence( name );
		if ( existing == null ) {
			return createSequence( name );
		}
		return existing;
	}

	public Sequence locateOrCreateSequence(Identifier name, int initialValue, int incrementSize) {
		final Sequence existing = locateSequence( name );
		if ( existing == null ) {
			return createSequence( name, initialValue, incrementSize );
		}
		return existing;
	}

	public Iterable<Sequence> getSequences() {
		return sequences.values();
	}

	public InLineView getInLineView(String logicalName) {
		return inLineViews.get( logicalName );
	}

	public InLineView createInLineView(String logicalName, String subSelect) {
		InLineView inLineView = new InLineView( this, logicalName, subSelect );
		inLineViews.put( logicalName, inLineView );
		return inLineView;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "Schema" );
		sb.append( "{name=" ).append( name );
		sb.append( '}' );
		return sb.toString();
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		Schema schema = (Schema) o;

		if ( name != null ? !name.equals( schema.name ) : schema.name != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		return name != null ? name.hashCode() : 0;
	}

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

		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder();
			sb.append( "Name" );
			sb.append( "{schema=" ).append( schema );
			sb.append( ", catalog=" ).append( catalog );
			sb.append( '}' );
			return sb.toString();
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}

			Name name = (Name) o;

			if ( catalog != null ? !catalog.equals( name.catalog ) : name.catalog != null ) {
				return false;
			}
			if ( schema != null ? !schema.equals( name.schema ) : name.schema != null ) {
				return false;
			}

			return true;
		}

		@Override
		public int hashCode() {
			int result = schema != null ? schema.hashCode() : 0;
			result = 31 * result + ( catalog != null ? catalog.hashCode() : 0 );
			return result;
		}
	}
}
