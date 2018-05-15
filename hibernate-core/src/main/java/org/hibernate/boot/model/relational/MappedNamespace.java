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
import java.util.UUID;

import org.hibernate.HibernateException;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.mapping.DenormalizedTable;
import org.hibernate.mapping.Table;
import org.hibernate.naming.Identifier;
import org.hibernate.naming.NamespaceName;
import org.hibernate.naming.spi.RelationalNamespace;

/**
 * Represents a namespace (named schema/catalog pair) with a Database and manages objects defined within.
 *
 * @author Steve Ebersole
 */
public class MappedNamespace implements RelationalNamespace<MappedTable, MappedSequence> {
	private static final CoreMessageLogger log = CoreLogging.messageLogger( MappedNamespace.class );

	private final NamespaceName name;

	private Map<Identifier, MappedTable> tables = new TreeMap<>();
	private Map<Identifier, MappedSequence> sequences = new TreeMap<>();

	public MappedNamespace(NamespaceName name) {
		this.name = name;

		log.debugf( "Created database namespace [logicalName=%s]", name.toString() );
	}

	@Override
	public Identifier getCatalogName() {
		return getName().getCatalog();
	}

	@Override
	public Identifier getSchemaName() {
		return getName().getSchema();
	}

	@Override
	public NamespaceName getName() {
		return name;
	}

	@Override
	public MappedTable getTable(UUID tableUid) {
		for ( MappedTable table : tables.values() ) {
			if ( tableUid.equals( table.getUid() ) ) {
				return table;
			}
		}

		throw new HibernateException(
				"Could not locate Table by uid [" + tableUid + "] in namespace [" + this + ']'
		);
	}

	public Collection<MappedTable> getTables() {
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
	public MappedTable locateTable(Identifier logicalTableName) {
		return tables.get( logicalTableName );
	}

	/**
	 * Creates a mapping Table instance.
	 *
	 * @param logicalTableName The logical table name
	 *
	 * @return the created table.
	 */
	public MappedTable createTable(Identifier logicalTableName, boolean isAbstract) {
		final MappedTable existing = tables.get( logicalTableName );
		if ( existing != null ) {
			return existing;
		}

		Table table = new Table( this, logicalTableName, isAbstract );
		tables.put( logicalTableName, table );
		return table;

	}

	public DenormalizedMappedTable createDenormalizedTable(Identifier logicalTableName, boolean isAbstract, MappedTable includedTable) {
		final MappedTable existing = tables.get( logicalTableName );
		if ( existing != null ) {
			// for now assume it is
			return (DenormalizedMappedTable) existing;
		}

		DenormalizedTable table = new DenormalizedTable( this, logicalTableName, isAbstract, includedTable );
		tables.put( logicalTableName, table );
		return table;
	}

	public MappedSequence locateSequence(Identifier name) {
		return sequences.get( name );
	}

	public MappedSequence createSequence(Identifier logicalName, int initialValue, int increment) {
		if ( sequences.containsKey( logicalName ) ) {
			throw new HibernateException( "Sequence was already registered with that name [" + logicalName.toString() + "]" );
		}

		Sequence sequence = new Sequence(
				getName().getCatalog(),
				getName().getSchema(),
				logicalName,
				initialValue,
				increment
		);
		sequences.put( logicalName, sequence );
		return sequence;
	}

	@Override
	public String toString() {
		return "Schema" + "{name=" + getName() + '}';
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		final MappedNamespace that = (MappedNamespace) o;
		return Objects.equals( this.name, that.name );
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	public Collection<MappedSequence> getSequences() {
		return sequences.values();
	}
}
