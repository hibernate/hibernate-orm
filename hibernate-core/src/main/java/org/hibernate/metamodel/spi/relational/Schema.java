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

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a named schema/catalog pair and manages objects defined within.
 *
 * @author Steve Ebersole
 */
public class Schema {
	private final Name name;
	private Map<String, InLineView> inLineViews = new HashMap<String, InLineView>();
	private Map<String, Table> tables = new HashMap<String, Table>();

	public Schema(Name name) {
		this.name = name;
	}

	public Schema(Identifier schema, Identifier catalog) {
		this( new Name( schema, catalog ) );
	}

	public Name getName() {
		return name;
	}

	/**
	 * Returns the table with the specified logical table name.
	 * .
	 * @param logicalTableName
	 *
	 * @return the table with the specified logical table name,
	 *         or null if there is no table with the specified
	 *         logical table name.
	 */
	public Table locateTable(String logicalTableName) {
		return tables.get( logicalTableName );
	}

	/**
	 * Creates a {@link Table} with the specified logical name. If
	 * {@code isPhysicalName} is true, then {@code name} is also
	 * the phycical table name.
	 *
	 * @param name - the name of the table
	 * @param isPhysicalName - true, if the name is known to be the
	 *        physical name.
	 * @return the created table.
	 */
	public Table createTable(Identifier name, boolean isPhysicalName) {
		Table table = new Table( this, name, isPhysicalName );
		tables.put( table.getLogicalName(), table );
		return table;
	}

	/* package-protected */
	void remapLogicalTableName(String oldName) {
		Table table = tables.remove( oldName );
		if ( table == null ) {
			throw new IllegalStateException(
					String.format(
							"Schema (%s) does not contain a table with logical name (%s) to remap.",
							name,
							oldName
					)
			);
		}
		tables.put( table.getLogicalName(), table );
	}

	/**
	 * Locates the table with the specified name; if none is found,
	 * a table with the specified name is created.
	 * <p/>
	 * The value for {@code isPhysicalTableName} is ignored if a table
	 * is located with the specified name, and that name is defined
	 * as the physical table name (indicated by a non-null value returned
	 * by {@link Table#getTableName()}.
	 *
	 * @param name - the name
	 * @param isPhysicalTableName - true, if the table is known to be
	 *                              the physical table name.
	 * @return the located or created table.
	 */
	public Table locateOrCreateTable(String name, boolean isPhysicalTableName) {
		final Table existing = locateTable( name );
		Identifier tableIdentifier = Identifier.toIdentifier( name );
		if ( existing == null ) {
			return createTable( tableIdentifier, isPhysicalTableName );
		}
		else if ( isPhysicalTableName && existing.getTableName() == null ) {
			existing.setPhysicalName( tableIdentifier );
		}
		return existing;
	}

	public Iterable<Table> getTables() {
		return tables.values();
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
