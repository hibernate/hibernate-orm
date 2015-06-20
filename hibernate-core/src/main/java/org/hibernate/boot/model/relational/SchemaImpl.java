/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
import org.hibernate.mapping.Table;

/**
 * @author Steve Ebersole
 */
public class SchemaImpl implements Schema {
	private static final CoreMessageLogger log = CoreLogging.messageLogger( SchemaImpl.class );

	private final Database database;
	private final Name name;

	private final Name physicalName;

	private Map<Identifier, Table> tables = new TreeMap<Identifier, Table>();
	private Map<Identifier, Sequence> sequences = new TreeMap<Identifier, Sequence>();

	public SchemaImpl(Database database, Name name) {
		this.database = database;
		this.name = name;

		final Identifier physicalCatalogIdentifier = database.getPhysicalNamingStrategy()
				.toPhysicalCatalogName( name.getCatalog(), database.getJdbcEnvironment() );
		final Identifier physicalSchemaIdentifier = database.getPhysicalNamingStrategy()
				.toPhysicalCatalogName( name.getSchema(), database.getJdbcEnvironment() );
		this.physicalName = new Name( physicalCatalogIdentifier, physicalSchemaIdentifier );
	}

	@Override
	public Name getName() {
		return name;
	}

	@Override
	public Name getPhysicalName() {
		return physicalName;
	}

	@Override
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
	@Override
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
	@Override
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

	@Override
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

	@Override
	public Sequence locateSequence(Identifier name) {
		return sequences.get( name );
	}

	@Override
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

		final SchemaImpl that = (SchemaImpl) o;
		return EqualsHelper.equals( this.name, that.name );
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public Iterable<Sequence> getSequences() {
		return sequences.values();
	}
}
