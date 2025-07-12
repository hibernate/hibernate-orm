/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.relational;

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
		this.physicalName = physicalName( name, physicalNamingStrategy, jdbcEnvironment );

		if ( log.isTraceEnabled() ) {
			log.tracef(
					"Created database namespace [logicalName=%s, physicalName=%s]",
					name.toString(),
					physicalName.toString()
			);
		}
	}

	private static Name physicalName(Name name, PhysicalNamingStrategy physicalNaming, JdbcEnvironment environment) {
		return new Name(
				physicalNaming.toPhysicalCatalogName( name.catalog(), environment ),
				physicalNaming.toPhysicalSchemaName( name.schema(), environment )
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
		else {
			final Identifier physicalTableName =
					physicalNamingStrategy.toPhysicalTableName( logicalTableName, jdbcEnvironment );
			final Table table = creator.apply( physicalTableName );
			tables.put( logicalTableName, table );
			return table;
		}
	}

	public DenormalizedTable createDenormalizedTable(Identifier logicalTableName, Function<Identifier,DenormalizedTable> creator) {
		final Table existing = tables.get( logicalTableName );
		if ( existing != null ) {
			return (DenormalizedTable) existing;
		}
		else {
			final Identifier physicalTableName =
					physicalNamingStrategy.toPhysicalTableName( logicalTableName, jdbcEnvironment );
			final DenormalizedTable table = creator.apply( physicalTableName );
			tables.put( logicalTableName, table );
			return table;
		}
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
			if ( udt instanceof UserDefinedObjectType userDefinedTypes ) {
				for ( Column udtColumn : userDefinedTypes.getColumns() ) {
					final Type udtColumnType = udtColumn.getValue().getType();
					if ( udtColumnType instanceof BasicType<?> basicType ) {
						final JdbcType jdbcType = basicType.getJdbcType();
						if ( jdbcType instanceof SqlTypedJdbcType sqlTypedJdbcType ) {
							dependencies.add( Identifier.toIdentifier( sqlTypedJdbcType.getSqlTypeName() ) );
						}
						else if ( jdbcType instanceof ArrayJdbcType arrayJdbcType ) {
							final JdbcType elementJdbcType = arrayJdbcType.getElementJdbcType();
							if ( elementJdbcType instanceof SqlTypedJdbcType sqlTypedJdbcType ) {
								dependencies.add( Identifier.toIdentifier( sqlTypedJdbcType.getSqlTypeName() ) );
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
			else if ( udt instanceof UserDefinedArrayType userDefinedTypes ) {
				final Identifier elementTypeName = Identifier.toIdentifier( userDefinedTypes.getElementTypeName() );
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
		else {
			final Identifier physicalTableName =
					physicalNamingStrategy.toPhysicalTypeName( logicalTypeName, jdbcEnvironment );
			final UserDefinedObjectType type = creator.apply( physicalTableName );
			udts.put( logicalTypeName, type );
			return type;
		}
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
		else {
			final Identifier physicalTableName =
					physicalNamingStrategy.toPhysicalTypeName( logicalTypeName, jdbcEnvironment );
			final UserDefinedArrayType type = creator.apply( physicalTableName );
			udts.put( logicalTypeName, type );
			return type;
		}
	}

	@Override
	public String toString() {
		return "Schema" + "{name=" + name + '}';
	}

	@Override
	public boolean equals(Object object) {
		if ( this == object ) {
			return true;
		}
		if ( !(object instanceof Namespace that) ) {
			return false;
		}
		return Objects.equals( this.name, that.name );
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	public Iterable<Sequence> getSequences() {
		return sequences.values();
	}

	public record Name(Identifier catalog, Identifier schema) implements Comparable<Name> {

		@Deprecated(since = "7")
		public Identifier getCatalog() {
			return catalog;
		}

		@Deprecated(since = "7")
		public Identifier getSchema() {
			return schema;
		}

		@Override
		public int compareTo(Name that) {
			// per Comparable, the incoming Name cannot be null.
			// However, its catalog/schema might be so we need to account for that.
			final int catalogCheck = compare( this.catalog(), that.catalog() );
			return catalogCheck != 0 ? catalogCheck : compare( this.schema(), that.schema() );
		}

		private static int compare(Identifier first, Identifier second) {
			if ( first == null && second == null ) {
				return 0;
			}
			else if ( first == null ) {
				return 1;
			}
			else if ( second == null ) {
				return -1;
			}
			else {
				return first.compareTo( second );
			}
		}
	}

}
