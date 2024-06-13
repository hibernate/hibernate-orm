/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.relational;

import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;

import org.hibernate.HibernateException;
import org.hibernate.Incubating;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.mapping.AggregateColumn;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.DenormalizedTable;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UserDefinedArrayType;
import org.hibernate.mapping.UserDefinedObjectType;
import org.hibernate.mapping.UserDefinedType;
import org.hibernate.type.BasicType;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.jdbc.ArrayJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.SqlTypedJdbcType;

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

	private final Map<Identifier, Table> tables = new TreeMap<>();
	private final Map<Identifier, Sequence> sequences = new TreeMap<>();
	private final Map<Identifier, UserDefinedType> udts = new HashMap<>();

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

		if ( log.isDebugEnabled() ) {
			log.debugf(
					"Created database namespace [logicalName=%s, physicalName=%s]",
					name.toString(),
					physicalName.toString()
			);
		}
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

	public void registerTable(Identifier logicalName, Table table) {
		final Table previous = tables.put( logicalName, table );
		if ( previous != null ) {
			log.debugf(
					"Replacing Table registration(%s) : %s -> %s",
					logicalName,
					previous,
					table
			);
		}
	}

	/**
	 * Creates a mapping Table instance.
	 *
	 * @param logicalTableName The logical table name
	 *
	 * @return the created table.
	 */
	public Table createTable(Identifier logicalTableName, Function<Identifier,Table> creator) {
		final Table existing = tables.get( logicalTableName );
		if ( existing != null ) {
			return existing;
		}

		final Identifier physicalTableName = physicalNamingStrategy.toPhysicalTableName( logicalTableName, jdbcEnvironment );
		final Table table = creator.apply( physicalTableName );
		tables.put( logicalTableName, table );

		return table;
	}

	public DenormalizedTable createDenormalizedTable(Identifier logicalTableName, Function<Identifier,DenormalizedTable> creator) {
		final Table existing = tables.get( logicalTableName );
		if ( existing != null ) {
			return (DenormalizedTable) existing;
		}

		final Identifier physicalTableName = physicalNamingStrategy.toPhysicalTableName( logicalTableName, jdbcEnvironment );
		final DenormalizedTable table = creator.apply( physicalTableName );
		tables.put( logicalTableName, table );

		return table;
	}

	public Sequence locateSequence(Identifier name) {
		return sequences.get( name );
	}

	public void registerSequence(Identifier logicalName, Sequence sequence) {
		if ( sequences.containsKey( logicalName ) ) {
			throw new HibernateException( "Sequence was already registered with that name [" + logicalName.toString() + "]" );
		}
		sequences.put( logicalName, sequence );
	}

	public Sequence createSequence(Identifier logicalName, Function<Identifier,Sequence> creator) {
		if ( sequences.containsKey( logicalName ) ) {
			throw new HibernateException( "Sequence was already registered with that name [" + logicalName.toString() + "]" );
		}

		final Identifier physicalName = physicalNamingStrategy.toPhysicalSequenceName( logicalName, jdbcEnvironment );
		final Sequence sequence = creator.apply( physicalName );
		sequences.put( logicalName, sequence );

		return sequence;
	}

	@Incubating
	public Collection<UserDefinedType> getUserDefinedTypes() {
		return udts.values();
	}

	@Incubating
	public List<UserDefinedType> getDependencyOrderedUserDefinedTypes() {
		final var orderedUdts = new LinkedHashMap<Identifier, UserDefinedType>( udts.size() );
		final var udtDependencies = new HashMap<Identifier, Set<Identifier>>( udts.size() );
		for ( var entry : udts.entrySet() ) {
			final var dependencies = new HashSet<Identifier>();
			final UserDefinedType udt = entry.getValue();
			if ( udt instanceof UserDefinedObjectType ) {
				for ( Column udtColumn : ( (UserDefinedObjectType) udt ).getColumns() ) {
					final Type udtColumnType = udtColumn.getValue().getType();
					if ( udtColumnType instanceof BasicType<?> ) {
						final JdbcType jdbcType = ( (BasicType<?>) udtColumnType ).getJdbcType();
						if ( jdbcType instanceof SqlTypedJdbcType ) {
							dependencies.add( Identifier.toIdentifier( ( (SqlTypedJdbcType) jdbcType ).getSqlTypeName() ) );
						}
						else if ( jdbcType instanceof ArrayJdbcType ) {
							final JdbcType elementJdbcType = ( (ArrayJdbcType) jdbcType ).getElementJdbcType();
							if ( elementJdbcType instanceof SqlTypedJdbcType ) {
								dependencies.add( Identifier.toIdentifier( ( (SqlTypedJdbcType) elementJdbcType ).getSqlTypeName() ) );
							}
						}
					}
				}
				if ( dependencies.isEmpty() ) {
					// The UDTs without dependencies are added directly
					orderedUdts.put( udt.getNameIdentifier(), udt );
				}
				else {
					// For the rest we record the direct dependencies
					udtDependencies.put( entry.getKey(), dependencies );
				}
			}
			else if ( udt instanceof UserDefinedArrayType ) {
				final Identifier elementTypeName = Identifier.toIdentifier( ( (UserDefinedArrayType) udt ).getElementTypeName() );
				if ( udts.get( elementTypeName ) instanceof UserDefinedObjectType ) {
					dependencies.add( elementTypeName );
					udtDependencies.put( entry.getKey(), dependencies );
				}
				else {
					// No need to worry about dependency ordering with respect to types we don't know
					orderedUdts.put( udt.getNameIdentifier(), udt );
				}
			}
		}
		// Traverse the dependency sets
		while ( !udtDependencies.isEmpty() ) {
			for ( final var iterator = udtDependencies.entrySet().iterator(); iterator.hasNext(); ) {
				final var entry = iterator.next();
				final Set<Identifier> dependencies = entry.getValue();
				// Remove the already ordered UDTs from the dependencies
				dependencies.removeAll( orderedUdts.keySet() );
				// If the dependencies have become empty
				if ( dependencies.isEmpty() ) {
					// the UDT can be inserted
					orderedUdts.put( entry.getKey(), udts.get( entry.getKey() ) );
					iterator.remove();
				}
			}
		}

		return new ArrayList<>( orderedUdts.values() );
	}

	/**
	 * Returns the object UDT with the specified logical UDT name.
	 *
	 * @param logicalTypeName - the logical name of the UDT
	 *
	 * @return the object UDT with the specified UDT name,
	 *         or null if there is no UDT with the specified
	 *         UDT name.
	 */
	@Incubating
	public UserDefinedObjectType locateUserDefinedType(Identifier logicalTypeName) {
		return (UserDefinedObjectType) udts.get( logicalTypeName );
	}

	/**
	 * Returns the array UDT with the specified logical UDT name.
	 *
	 * @param logicalTypeName - the logical name of the UDT
	 *
	 * @return the array UDT with the specified UDT name,
	 *         or null if there is no UDT with the specified
	 *         UDT name.
	 */
	@Incubating
	public UserDefinedArrayType locateUserDefinedArrayType(Identifier logicalTypeName) {
		return (UserDefinedArrayType) udts.get( logicalTypeName );
	}

	/**
	 * Creates a mapping UDT instance.
	 *
	 * @param logicalTypeName The logical UDT name
	 *
	 * @return the created UDT.
	 */
	@Incubating
	public UserDefinedObjectType createUserDefinedType(Identifier logicalTypeName, Function<Identifier, UserDefinedObjectType> creator) {
		final UserDefinedType existing = udts.get( logicalTypeName );
		if ( existing != null ) {
			return (UserDefinedObjectType) existing;
		}

		final Identifier physicalTableName = physicalNamingStrategy.toPhysicalTypeName( logicalTypeName, jdbcEnvironment );
		final UserDefinedObjectType type = creator.apply( physicalTableName );
		udts.put( logicalTypeName, type );

		return type;
	}
	/**
	 * Creates a mapping UDT instance.
	 *
	 * @param logicalTypeName The logical UDT name
	 *
	 * @return the created UDT.
	 */
	@Incubating
	public UserDefinedArrayType createUserDefinedArrayType(Identifier logicalTypeName, Function<Identifier, UserDefinedArrayType> creator) {
		final UserDefinedType existing = udts.get( logicalTypeName );
		if ( existing != null ) {
			return (UserDefinedArrayType) existing;
		}

		final Identifier physicalTableName = physicalNamingStrategy.toPhysicalTypeName( logicalTypeName, jdbcEnvironment );
		final UserDefinedArrayType type = creator.apply( physicalTableName );
		udts.put( logicalTypeName, type );

		return type;
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
