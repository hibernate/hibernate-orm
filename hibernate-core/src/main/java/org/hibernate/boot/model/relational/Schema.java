/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
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
package org.hibernate.boot.model.relational;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import org.hibernate.HibernateException;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.compare.EqualsHelper;
import org.hibernate.mapping.DenormalizedTable;
import org.hibernate.mapping.Table;

/**
 * Represents a named schema/catalog pair and manages objects defined within.
 *
 * @author Steve Ebersole
 */
public class Schema {
	private static final CoreMessageLogger log = CoreLogging.messageLogger( Schema.class );

	private final Database database;
	private final Name name;

	private final Name physicalName;

	private Map<Identifier, Table> tables = new TreeMap<Identifier, Table>();
	private Map<Identifier, Sequence> sequences = new TreeMap<Identifier, Sequence>();

	public Schema(Database database, Name name) {
		this.database = database;
		this.name = name;

		final Identifier physicalCatalogIdentifier = database.getPhysicalNamingStrategy()
				.toPhysicalCatalogName( name.getCatalog(), database.getJdbcEnvironment() );
		final Identifier physicalSchemaIdentifier = database.getPhysicalNamingStrategy()
				.toPhysicalCatalogName( name.getSchema(), database.getJdbcEnvironment() );
		this.physicalName = new Name( physicalCatalogIdentifier, physicalSchemaIdentifier );
	}

	public Name getName() {
		return name;
	}

	public Name getPhysicalName() {
		return physicalName;
	}

	public Collection<Table> getTables() {
		return tables.values();
	}

	/**
	 * Returns the table with the specified logical table name.
	 *
	 * @param logicalTableName - the logical name of the table
	 *
	 * @return the table with the specified table name,
	 *         or null if there is no table with the specified
	 *         table name.
	 */
	public Table locateTable(Identifier logicalTableName) {
		return tables.get( logicalTableName );
	}

	/**
	 * Creates a mapping Table instance.
	 *
	 * @param logicalTableName The logical table name
	 *
	 * @return the created table.
	 */
	public Table createTable(Identifier logicalTableName, boolean isAbstract) {
		final Table existing = tables.get( logicalTableName );
		if ( existing != null ) {
			return existing;
		}

		final Identifier physicalTableName = database.getPhysicalNamingStrategy().toPhysicalTableName( logicalTableName, database.getJdbcEnvironment() );
		Table table = new Table( this, physicalTableName, isAbstract );
		tables.put( logicalTableName, table );
		return table;
	}

	public DenormalizedTable createDenormalizedTable(Identifier logicalTableName, boolean isAbstract, Table includedTable) {
		final Table existing = tables.get( logicalTableName );
		if ( existing != null ) {
			// for now assume it is
			return (DenormalizedTable) existing;
		}

		final Identifier physicalTableName = database.getPhysicalNamingStrategy().toPhysicalTableName( logicalTableName, database.getJdbcEnvironment() );
		DenormalizedTable table = new DenormalizedTable( this, physicalTableName, isAbstract, includedTable );
		tables.put( logicalTableName, table );
		return table;
	}

	public Sequence locateSequence(Identifier name) {
		return sequences.get( name );
	}

	public Sequence createSequence(Identifier logicalName, int initialValue, int increment) {
		if ( sequences.containsKey( logicalName ) ) {
			throw new HibernateException( "Sequence was already registered with that name [" + logicalName.toString() + "]" );
		}

		final Identifier physicalName = database.getPhysicalNamingStrategy().toPhysicalSequenceName( logicalName, database.getJdbcEnvironment() );

		Sequence sequence = new Sequence(
				this.physicalName.getCatalog(),
				this.physicalName.getSchema(),
				physicalName,
				initialValue,
				increment
		);
		sequences.put( logicalName, sequence );
		return sequence;
	}

	@Override
	public String toString() {
		return "Schema" + "{name=" + name + '}';
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		final Schema that = (Schema) o;
		return EqualsHelper.equals( this.name, that.name );
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	public Iterable<Sequence> getSequences() {
		return sequences.values();
	}

	public static class Name implements Comparable<Name> {
		private final Identifier catalog;
		private final Identifier schema;

		public Name(Identifier catalog, Identifier schema) {
			this.schema = schema;
			this.catalog = catalog;
		}

		public Identifier getCatalog() {
			return catalog;
		}

		public Identifier getSchema() {
			return schema;
		}

		@Override
		public String toString() {
			return "Name" + "{catalog=" + catalog + ", schema=" + schema + '}';
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}

			final Name that = (Name) o;

			return EqualsHelper.equals( this.catalog, that.catalog )
					&& EqualsHelper.equals( this.schema, that.schema );
		}

		@Override
		public int hashCode() {
			int result = catalog != null ? catalog.hashCode() : 0;
			result = 31 * result + (schema != null ? schema.hashCode() : 0);
			return result;
		}

		@Override
		public int compareTo(Name that) {
			// per Comparable, the incoming Name cannot be null.  However, its catalog/schema might be
			// so we need to account for that.
			int catalogCheck = ComparableHelper.compare( this.getCatalog(), that.getCatalog() );
			if ( catalogCheck != 0 ) {
				return catalogCheck;
			}

			return ComparableHelper.compare( this.getSchema(), that.getSchema() );
		}
	}

	public static class ComparableHelper {
		public static <T extends Comparable<T>> int compare(T first, T second) {
			if ( first == null ) {
				if ( second == null ) {
					return 0;
				}
				else {
					return 1;
				}
			}
			else {
				if ( second == null ) {
					return -1;
				}
				else {
					return first.compareTo( second );
				}
			}
		}
	}
}
