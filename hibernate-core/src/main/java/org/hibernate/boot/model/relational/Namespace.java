/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.relational;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import org.hibernate.HibernateException;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.mapping.DenormalizedTable;
import org.hibernate.mapping.Table;

/**
 * Represents a namespace (named schema/catalog pair) with a Database and manages objects defined within.
 *
 * @author Steve Ebersole
 */
public class Namespace {
	private static final CoreMessageLogger log = CoreLogging.messageLogger( Namespace.class );

	private final PhysicalNamingStrategy physicalNamingStrategy;
	private final JdbcEnvironment jdbcEnvironment;
	private final Name name;
	private final Name physicalName;

	private Map<Identifier, Table> tables = new TreeMap<Identifier, Table>();
	private Map<Identifier, Sequence> sequences = new TreeMap<Identifier, Sequence>();

	public Namespace(PhysicalNamingStrategy physicalNamingStrategy, JdbcEnvironment jdbcEnvironment, Name name) {
		this.physicalNamingStrategy = physicalNamingStrategy;
		this.jdbcEnvironment = jdbcEnvironment;
		this.name = name;

		this.physicalName = new Name(
				physicalNamingStrategy
						.toPhysicalCatalogName( name.getCatalog(), jdbcEnvironment ),
				physicalNamingStrategy
						.toPhysicalSchemaName( name.getSchema(), jdbcEnvironment )
		);

		log.debugf(
				"Created database namespace [logicalName=%s, physicalName=%s]",
				name.toString(),
				physicalName.toString()
		);
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

		final Identifier physicalTableName = physicalNamingStrategy.toPhysicalTableName( logicalTableName, jdbcEnvironment );
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

		final Identifier physicalTableName = physicalNamingStrategy.toPhysicalTableName( logicalTableName, jdbcEnvironment );
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

		final Identifier physicalName = physicalNamingStrategy.toPhysicalSequenceName( logicalName, jdbcEnvironment );

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

		final Namespace that = (Namespace) o;
		return Objects.equals( this.name, that.name );
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

			return Objects.equals( this.catalog, that.catalog )
					&& Objects.equals( this.schema, that.schema );
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
