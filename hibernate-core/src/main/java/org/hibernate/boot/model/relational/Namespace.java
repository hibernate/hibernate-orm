/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.relational;

import org.hibernate.mapping.NamedTable;
import org.hibernate.relational.naming.spi.PhysicalName;
import org.hibernate.boot.model.naming.internal.PhysicalNamingStrategyHelper;

import java.io.Serializable;

import org.hibernate.boot.model.relational.internal.PhysicalNamespaceSnapshot;
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

import org.hibernate.relational.naming.spi.LogicalName;

import org.hibernate.HibernateException;
import org.hibernate.Incubating;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
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

import static org.hibernate.boot.BootLogging.BOOT_LOGGER;

/**
 * Represents a namespace (named schema/catalog pair) with a Database and manages objects defined within.
 *
 * @author Steve Ebersole
 */
public class Namespace implements Serializable {

	private transient PhysicalNamingStrategy physicalNamingStrategy;
	private transient JdbcEnvironment jdbcEnvironment;
	private final LogicalNamespaceName name;
	private transient PhysicalNamespaceName physicalName;
	private final PhysicalNamespaceSnapshot physicalNameSnapshot;

	private final Map<LogicalName, Table> tables = new TreeMap<>();
	private final Map<LogicalName, Sequence> sequences = new TreeMap<>();
	private final Map<Identifier, UserDefinedType> udts = new HashMap<>();

	public Namespace(PhysicalNamingStrategy physicalNamingStrategy, JdbcEnvironment jdbcEnvironment, LogicalNamespaceName name) {
		this( physicalNamingStrategy, jdbcEnvironment, name, physicalName( name, physicalNamingStrategy, jdbcEnvironment ) );
	}

	Namespace(PhysicalNamingStrategy physicalNamingStrategy, JdbcEnvironment jdbcEnvironment,
			LogicalNamespaceName name, PhysicalNamespaceName physicalName) {
		this.physicalNamingStrategy = physicalNamingStrategy;
		this.jdbcEnvironment = jdbcEnvironment;
		this.name = name;
		this.physicalName = physicalName;
		physicalNameSnapshot = PhysicalNamespaceSnapshot.from( physicalName );
		BOOT_LOGGER.createdDatabaseNamespace( name, physicalName );
	}

	private static PhysicalNamespaceName physicalName(LogicalNamespaceName name, PhysicalNamingStrategy physicalNaming, JdbcEnvironment environment) {
		return new PhysicalNamespaceName(
				PhysicalNamingStrategyHelper.resolve( name.catalog(), environment, physicalNaming::toPhysicalCatalogName, "catalog", true ),
				PhysicalNamingStrategyHelper.resolve( name.schema(), environment, physicalNaming::toPhysicalSchemaName, "schema", true )
		);
	}

	public LogicalNamespaceName getName() {
		return name;
	}

	public PhysicalNamespaceName getPhysicalName() {
		if ( physicalName == null ) {
			throw new IllegalStateException( "Namespace services must be reattached before accessing physical names" );
		}
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
	public Table locateTable(LogicalName logicalTableName) {
		return tables.get( logicalTableName );
	}

	public void registerTable(LogicalName logicalName, Table table) {
		final Table previous = tables.put( logicalName, table );
		if ( previous != null ) {
			BOOT_LOGGER.replacingTableRegistration(
					String.valueOf( logicalName ),
					String.valueOf( previous ),
					String.valueOf( table )
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
	public Table createTable(LogicalName logicalTableName, Function<PhysicalName,Table> creator) {
		final Table existing = tables.get( logicalTableName );
		if ( existing != null ) {
			return existing;
		}
		else {
			final var physicalTableName = PhysicalNamingStrategyHelper.resolve(
					logicalTableName, jdbcEnvironment, physicalNamingStrategy::toPhysicalTableName, "table", false );
			final Table table = creator.apply( physicalTableName );
			tables.put( logicalTableName, table );
			return table;
		}
	}

	public DenormalizedTable createDenormalizedTable(LogicalName logicalTableName, Function<PhysicalName,DenormalizedTable> creator) {
		final Table existing = tables.get( logicalTableName );
		if ( existing != null ) {
			return (DenormalizedTable) existing;
		}
		else {
			final var physicalTableName = PhysicalNamingStrategyHelper.resolve(
					logicalTableName, jdbcEnvironment, physicalNamingStrategy::toPhysicalTableName, "table", false );
			final DenormalizedTable table = creator.apply( physicalTableName );
			tables.put( logicalTableName, table );
			return table;
		}
	}

	public Sequence locateSequence(LogicalName name) {
		return sequences.get( name );
	}

	public void registerSequence(LogicalName logicalName, Sequence sequence) {
		if ( sequences.containsKey( logicalName ) ) {
			throw new HibernateException( "Sequence was already registered with that name [" + logicalName.toString() + "]" );
		}
		sequences.put( logicalName, sequence );
	}

	public Sequence createSequence(LogicalName logicalName, Function<PhysicalName,Sequence> creator) {
		if ( sequences.containsKey( logicalName ) ) {
			throw new HibernateException( "Sequence was already registered with that name [" + logicalName.toString() + "]" );
		}

		final var physicalName = PhysicalNamingStrategyHelper.resolve( logicalName, jdbcEnvironment, physicalNamingStrategy::toPhysicalSequenceName, "sequence", false );
		final Sequence sequence = creator.apply( physicalName );
		sequences.put( logicalName, sequence );
		return sequence;
	}

	@Incubating(since = "6.6")
	public Collection<UserDefinedType> getUserDefinedTypes() {
		return udts.values();
	}

	@Incubating(since = "6.6")
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
	@Incubating(since = "6.6")
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
	@Incubating(since = "6.6")
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
	@Incubating(since = "6.6")
	public UserDefinedObjectType createUserDefinedType(Identifier logicalTypeName, Function<Identifier, UserDefinedObjectType> creator) {
		final UserDefinedType existing = udts.get( logicalTypeName );
		if ( existing != null ) {
			return (UserDefinedObjectType) existing;
		}
		else {
			final Identifier physicalTableName =
					PhysicalNamingStrategyHelper.toPhysicalTypeName( physicalNamingStrategy, logicalTypeName, jdbcEnvironment );
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
	@Incubating(since = "6.6")
	public UserDefinedArrayType createUserDefinedArrayType(Identifier logicalTypeName, Function<Identifier, UserDefinedArrayType> creator) {
		final UserDefinedType existing = udts.get( logicalTypeName );
		if ( existing != null ) {
			return (UserDefinedArrayType) existing;
		}
		else {
			final Identifier physicalTableName =
					PhysicalNamingStrategyHelper.toPhysicalTypeName( physicalNamingStrategy, logicalTypeName, jdbcEnvironment );
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

	void reattach(PhysicalNamingStrategy physicalNamingStrategy, JdbcEnvironment jdbcEnvironment) {
		this.physicalNamingStrategy = physicalNamingStrategy;
		this.jdbcEnvironment = jdbcEnvironment;
		final var factory = jdbcEnvironment.getIdentifierHelper().getPhysicalNameFactory();
		physicalName = physicalNameSnapshot.restore( factory );
		sequences.values().forEach( sequence -> sequence.reattach( factory ) );
		tables.values().forEach( table -> {
			if ( table instanceof NamedTable namedTable ) {
				namedTable.reattachPhysicalName( factory );
			}
		} );
	}

	/** Logical catalog/schema key; physical qualifiers are kept separately. */
	public record LogicalNamespaceName(LogicalName catalog, LogicalName schema)
			implements Comparable<LogicalNamespaceName>, Serializable {
		@Override
		public int compareTo(LogicalNamespaceName that) {
			final int catalogCheck = compare( catalog, that.catalog );
			return catalogCheck != 0 ? catalogCheck : compare( schema, that.schema );
		}

		private static int compare(LogicalName first, LogicalName second) {
			return first == null ? (second == null ? 0 : 1) : second == null ? -1 : first.compareTo( second );
		}
	}

	public record Name(Identifier catalog, Identifier schema) implements Comparable<Name>, Serializable {

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
