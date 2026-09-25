/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.Internal;
import org.hibernate.MappingException;
import org.hibernate.action.queue.spi.bind.JdbcValueBindings;
import org.hibernate.action.queue.spi.meta.EntityTableDescriptor;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.NaturalIdDataAccess;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;
import org.hibernate.metamodel.mapping.DiscriminatorValue;
import org.hibernate.metamodel.mapping.EntityDiscriminatorMapping;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityVersionMapping;
import org.hibernate.metamodel.mapping.TableDetails;
import org.hibernate.metamodel.mapping.internal.BasicEntityIdentifierMappingImpl;
import org.hibernate.metamodel.mapping.internal.CaseStatementDiscriminatorMappingImpl;
import org.hibernate.metamodel.mapping.internal.MappingModelCreationProcess;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.persister.filter.FilterAliasGenerator;
import org.hibernate.persister.filter.internal.DynamicFilterAliasGenerator;
import org.hibernate.persister.state.spi.StateManagement;
import org.hibernate.sql.ast.spi.query.from.SqlAstJoinType;
import org.hibernate.sql.ast.spi.query.from.NamedTableReference;
import org.hibernate.sql.ast.spi.query.from.TableGroup;
import org.hibernate.sql.ast.spi.query.from.TableReference;
import org.hibernate.sql.ast.spi.query.from.TableReferenceJoin;
import org.hibernate.sql.ast.spi.query.from.UnknownTableReferenceException;
import org.hibernate.sql.ast.spi.model.builder.MutationGroupBuilder;
import org.hibernate.sql.ast.spi.model.builder.TableDeleteBuilder;
import org.hibernate.sql.ast.spi.model.builder.TableInsertBuilder;
import org.hibernate.sql.ast.spi.model.builder.TableMutationBuilder;
import org.hibernate.type.BasicType;
import org.hibernate.type.CompositeType;
import org.hibernate.type.StandardBasicTypes;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Collections.emptyMap;
import static java.util.function.Function.identity;
import static org.hibernate.internal.util.collections.ArrayHelper.contains;
import static org.hibernate.internal.util.collections.ArrayHelper.join;
import static org.hibernate.internal.util.collections.ArrayHelper.reverseFirst;
import static org.hibernate.internal.util.collections.ArrayHelper.to2DStringArray;
import static org.hibernate.internal.util.collections.ArrayHelper.toBooleanArray;
import static org.hibernate.internal.util.collections.ArrayHelper.toIntArray;
import static org.hibernate.internal.util.collections.ArrayHelper.toStringArray;
import static org.hibernate.internal.util.collections.CollectionHelper.linkedMapOfSize;
import static org.hibernate.metamodel.mapping.internal.MappingModelCreationHelper.buildEncapsulatedCompositeIdentifierMapping;
import static org.hibernate.metamodel.mapping.internal.MappingModelCreationHelper.buildNonEncapsulatedCompositeIdentifierMapping;

/**
 * An {@link EntityPersister} implementing the normalized
 * {@link jakarta.persistence.InheritanceType#JOINED} inheritance
 * mapping strategy for an entity and its inheritance hierarchy.
 * <p>
 * This is implemented as a separate table for each subclass,
 * with only declared attributes persisted as columns of that table.
 * Thus, each instance of a subclass has its state stored across
 * rows of multiple tables.
 *
 * @author Gavin King
 */
@Internal
public class JoinedSubclassEntityPersister extends AbstractEntityPersister {
	private static final Logger LOG = Logger.getLogger( JoinedSubclassEntityPersister.class );

	private static final String IMPLICIT_DISCRIMINATOR_ALIAS = "clazz_";

	// the class hierarchy structure
	private final int tableSpan;
	private final boolean hasDuplicateTables;
	private final String[] tableNames;
	private final String[] naturalOrderTableNames;
	private final String[][] tableKeyColumns;
//	private final String[][] tableKeyColumnReaders;
//	private final String[][] tableKeyColumnReaderTemplates;
	private final String[][] naturalOrderTableKeyColumns;
	private final boolean[] naturalOrderCascadeDeleteEnabled;

	private final String[] spaces;

//	private final String[] subclassClosure;

	private final String[] subclassTableNameClosure;
	private final String[][] subclassTableKeyColumnClosure;
	private final boolean[] isClassOrSuperclassTable;

	// properties of this class, including inherited properties
	private final int[] naturalOrderPropertyTableNumbers;

	// the closure of all properties in the entire hierarchy including
	// subclasses and superclasses of this class
	private final int[] subclassPropertyTableNumberClosure;

	// the closure of all columns used by the entire hierarchy including
	// subclasses and superclasses of this class
	private final int[] subclassColumnNaturalOrderTableNumberClosure;
	private final String[] subclassColumnClosure;

	private final boolean[] isNullableSubclassTable;

	// subclass discrimination works by assigning particular
	// values to certain combinations of not-null primary key
	// values in the outer join using an SQL CASE
	private final Map<DiscriminatorValue, String> subclassesByDiscriminatorValue = new HashMap<>();
	@Nullable
	private final String[] discriminatorValues;
	@Nullable
	private final boolean[] discriminatorAbstract;
	@Nullable
	private final String[] notNullColumnNames;
	@Nullable
	private final int[] notNullColumnTableNumbers;

	private final String[] constraintOrderedTableNames;
	private final String[][] constraintOrderedKeyColumnNames;

	@Nullable
	private final DiscriminatorValue discriminatorValue;
	@Nullable
	private final String discriminatorSQLString;
	private final BasicType<?> discriminatorType;
	@Nullable
	private final String explicitDiscriminatorColumnName;
	private final String discriminatorAlias;
	private final boolean forceDiscriminator;
	private final boolean discriminatorInsertable;

	// Span of the tables directly mapped by this entity and super-classes, if any
	private final int coreTableSpan;
	private final int subclassCoreTableSpan;
	// only contains values for SecondaryTables, i.e. not tables part of the "coreTableSpan"
	private final boolean[] isNullableTable;
	private final boolean[] isInverseTable;

	private final Map<String, DiscriminatorValue> discriminatorValuesByTableName;
	private final Map<String, String> discriminatorColumnNameByTableName;

	public JoinedSubclassEntityPersister(
			@Nonnull final PersistentClass persistentClass,
			@Nullable final EntityDataAccess cacheAccessStrategy,
			@Nullable final NaturalIdDataAccess naturalIdRegionAccessStrategy,
			@Nonnull final RuntimeModelCreationContext creationContext)
			throws HibernateException {
		this( persistentClass, cacheAccessStrategy, naturalIdRegionAccessStrategy, creationContext, identity() );
	}

	protected JoinedSubclassEntityPersister(
			@Nonnull final PersistentClass persistentClass,
			@Nullable final EntityDataAccess cacheAccessStrategy,
			@Nullable final NaturalIdDataAccess naturalIdRegionAccessStrategy,
			@Nonnull final RuntimeModelCreationContext creationContext,
			@Nonnull final Function<StateManagement, StateManagement> stateManagementConverter)
					throws HibernateException {
		super( persistentClass, cacheAccessStrategy, naturalIdRegionAccessStrategy, creationContext, stateManagementConverter );

		final var dialect = creationContext.getDialect();
		final var typeConfiguration = creationContext.getTypeConfiguration();
		final var basicTypeRegistry = typeConfiguration.getBasicTypeRegistry();

		// DISCRIMINATOR

		if ( persistentClass.isPolymorphic() ) {
			forceDiscriminator = persistentClass.isForceDiscriminator();
			final var discriminatorMapping = persistentClass.getDiscriminator();
			if ( discriminatorMapping != null ) {
				LOG.tracef( "Encountered explicit discriminator mapping for joined inheritance" );
				final var selectable = discriminatorMapping.getSelectables().get(0);
				if ( selectable instanceof Column column ) {
					explicitDiscriminatorColumnName = column.getQuotedName( dialect );
					discriminatorAlias = column.getAlias( dialect, persistentClass.getRootTable() );
				}
				else {
					throw new MappingException( "Discriminator formulas on joined inheritance hierarchies not supported at this time" );
				}
				discriminatorType = DiscriminatorHelper.getDiscriminatorType( persistentClass );
				discriminatorValue = DiscriminatorHelper.getDiscriminatorValue( persistentClass );
				discriminatorSQLString = DiscriminatorHelper.getDiscriminatorSQLValue( persistentClass, dialect );
				discriminatorInsertable = isDiscriminatorInsertable( persistentClass );
			}
			else {
				explicitDiscriminatorColumnName = null;
				discriminatorAlias = IMPLICIT_DISCRIMINATOR_ALIAS;
				discriminatorType = basicTypeRegistry.resolve( StandardBasicTypes.INTEGER );
				try {
					discriminatorValue = new DiscriminatorValue.Literal( persistentClass.getSubclassId() );
					discriminatorSQLString = Integer.toString( persistentClass.getSubclassId() );
				}
				catch ( Exception e ) {
					throw new MappingException( "Could not format discriminator value to SQL string", e );
				}
				discriminatorInsertable = true;
			}
		}
		else {
			explicitDiscriminatorColumnName = null;
			discriminatorAlias = IMPLICIT_DISCRIMINATOR_ALIAS;
			discriminatorType = basicTypeRegistry.resolve( StandardBasicTypes.INTEGER );
			discriminatorValue = null;
			discriminatorSQLString = null;
			forceDiscriminator = false;
			discriminatorInsertable = false;
		}

		if ( optimisticLockStyle().isAllOrDirty() ) {
			throw new MappingException( "optimistic-lock=all|dirty not supported for joined-subclass mappings [" + getEntityName() + "]" );
		}

		//MULTITABLES

		final int idColumnSpan = getIdentifierColumnSpan();

		final ArrayList<String> tableNames = new ArrayList<>();
		final ArrayList<String[]> keyColumns = new ArrayList<>();
//		final ArrayList<String[]> keyColumnReaders = new ArrayList<>();
//		final ArrayList<String[]> keyColumnReaderTemplates = new ArrayList<>();
		final ArrayList<Boolean> cascadeDeletes = new ArrayList<>();
		final var tableClosure = persistentClass.getTableClosure();
		final var keyClosure = persistentClass.getKeyClosure();
		for ( int i = 0; i < tableClosure.size() && i < keyClosure.size(); i++ ) {
			tableNames.add( determineTableName( tableClosure.get(i) ) );

			final var key = keyClosure.get(i);
			final String[] keyCols = new String[idColumnSpan];
//			final String[] keyColReaders = new String[idColumnSpan];
//			final String[] keyColReaderTemplates = new String[idColumnSpan];
			final var columns = key.getColumns();
			for ( int k = 0; k < idColumnSpan; k++ ) {
				final var column = columns.get(k);
				keyCols[k] = column.getQuotedName( dialect );
//				keyColReaders[k] = column.getReadExpr( dialect );
//				keyColReaderTemplates[k] = column.getTemplate( dialect, typeConfiguration );
			}
			keyColumns.add( keyCols );
//			keyColumnReaders.add( keyColReaders );
//			keyColumnReaderTemplates.add( keyColReaderTemplates );
			cascadeDeletes.add( key.isCascadeDeleteEnabled() && dialect.getForeignKeySupport().supportsOnDeleteAction( org.hibernate.annotations.OnDeleteAction.CASCADE ) );
		}

		//Span of the tableNames directly mapped by this entity and super-classes, if any
		coreTableSpan = tableNames.size();
		subclassCoreTableSpan = persistentClass.getSubclassTableClosure().size();
		tableSpan = persistentClass.getJoinClosureSpan() + coreTableSpan;

		isNullableTable = new boolean[tableSpan];
		isInverseTable = new boolean[tableSpan];

		final var joinClosure = persistentClass.getJoinClosure();
		for ( int i = 0; i < joinClosure.size(); i++ ) {
			final var join = joinClosure.get(i);
			isNullableTable[i] = join.isOptional();
			isInverseTable[i] = join.isInverse();

			tableNames.add( determineTableName( join.getTable() ) );

			final var key = join.getKey();
			final int joinIdColumnSpan = key.getColumnSpan();

			final String[] keyCols = new String[joinIdColumnSpan];
//			final String[] keyColReaders = new String[joinIdColumnSpan];
//			final String[] keyColReaderTemplates = new String[joinIdColumnSpan];

			final var columns = key.getColumns();
			for ( int k = 0; k < joinIdColumnSpan; k++ ) {
				final var column = columns.get(k);
				keyCols[k] = column.getQuotedName( dialect );
//				keyColReaders[k] = column.getReadExpr( dialect );
//				keyColReaderTemplates[k] = column.getTemplate( dialect, typeConfiguration );
			}
			keyColumns.add( keyCols );
//			keyColumnReaders.add( keyColReaders );
//			keyColumnReaderTemplates.add( keyColReaderTemplates );
			cascadeDeletes.add( key.isCascadeDeleteEnabled() && dialect.getForeignKeySupport().supportsOnDeleteAction( org.hibernate.annotations.OnDeleteAction.CASCADE ) );
		}

		hasDuplicateTables = new HashSet<>( tableNames ).size() == tableNames.size();
		naturalOrderTableNames = toStringArray( tableNames );
		naturalOrderTableKeyColumns = to2DStringArray( keyColumns );
//		final String[][] naturalOrderTableKeyColumnReaders = to2DStringArray( keyColumnReaders );
//		final String[][] naturalOrderTableKeyColumnReaderTemplates = to2DStringArray( keyColumnReaderTemplates );
		naturalOrderCascadeDeleteEnabled = toBooleanArray( cascadeDeletes );

		final ArrayList<String> subclassTableNames = new ArrayList<>();
		final ArrayList<Boolean> isConcretes = new ArrayList<>();
		final ArrayList<Boolean> isNullables = new ArrayList<>();

		final ArrayList<String[]> allKeyColumns = new ArrayList<>();
		for ( var persistentSubclass : persistentClass.getPersistentClassClosure() ) {
			final Table table = persistentSubclass.getTable();
			isConcretes.add( persistentClass.isClassOrSuperclassTable( table ) );
			isNullables.add( false );
			final String tableName = determineTableName( table );
			subclassTableNames.add( tableName );
			final String[] key = new String[idColumnSpan];
			final var columns = persistentSubclass.getKey().getColumns();
			for ( int k = 0; k < idColumnSpan; k++ ) {
				key[k] = columns.get(k).getQuotedName( dialect );
			}
			allKeyColumns.add( key );
		}

		//Add joins
		for ( var join : persistentClass.getSubclassJoinClosure() ) {
			final var joinTable = join.getTable();
			isConcretes.add( persistentClass.isClassOrSuperclassTable( joinTable ) );
			isNullables.add( join.isOptional() );
			final String joinTableName = determineTableName( joinTable );
			subclassTableNames.add( joinTableName );
			final String[] key = new String[idColumnSpan];
			final var columns = join.getKey().getColumns();
			for ( int k = 0; k < idColumnSpan; k++ ) {
				key[k] = columns.get(k).getQuotedName( dialect );
			}
			allKeyColumns.add( key );
		}

		final String[] naturalOrderSubclassTableNameClosure = toStringArray( subclassTableNames );
		final String[][] naturalOrderSubclassTableKeyColumnClosure = to2DStringArray( allKeyColumns );
		isClassOrSuperclassTable = toBooleanArray( isConcretes );
		isNullableSubclassTable = toBooleanArray( isNullables );

		constraintOrderedTableNames = new String[naturalOrderSubclassTableNameClosure.length];
		constraintOrderedKeyColumnNames = new String[naturalOrderSubclassTableNameClosure.length][];
		int currentPosition = 0;
		for ( int i = naturalOrderSubclassTableNameClosure.length - 1; i >= 0; i--, currentPosition++ ) {
			constraintOrderedTableNames[currentPosition] = naturalOrderSubclassTableNameClosure[i];
			constraintOrderedKeyColumnNames[currentPosition] = naturalOrderSubclassTableKeyColumnClosure[i];
		}

		// Suppose an entity Client extends Person, mapped to the tableNames CLIENT and PERSON respectively.
		// For the Client entity:
		// naturalOrderTableNames -> PERSON, CLIENT; this reflects the sequence in which the tableNames are
		// added to the meta-data when the annotated entities are processed.
		// However, in some instances, for example when generating joins, the CLIENT table needs to be
		// the first table as it will the driving table.
		// tableNames -> CLIENT, PERSON

		this.tableNames = reverseFirst( naturalOrderTableNames, coreTableSpan );
		tableKeyColumns = reverseFirst( naturalOrderTableKeyColumns, coreTableSpan );
//		tableKeyColumnReaders = reverseFirst( naturalOrderTableKeyColumnReaders, coreTableSpan );
//		tableKeyColumnReaderTemplates = reverseFirst( naturalOrderTableKeyColumnReaderTemplates, coreTableSpan );
		subclassTableNameClosure = reverseFirst( naturalOrderSubclassTableNameClosure, coreTableSpan );
		subclassTableKeyColumnClosure = reverseFirst( naturalOrderSubclassTableKeyColumnClosure, coreTableSpan );

		spaces = join( this.tableNames, toStringArray( persistentClass.getSynchronizedTables() ) );

		PersistentClass currentClass = persistentClass;
		int jk = coreTableSpan - 1;
		while ( currentClass != null ) {
			isNullableTable[jk] = false;
			isInverseTable[jk] = false;

			jk--;
			currentClass = currentClass.getSuperclass();
		}

		if ( jk != -1 ) {
			throw new AssertionFailure( "Tablespan does not match height of joined-subclass hierarchy." );
		}

		int j = coreTableSpan;
		for ( var join : persistentClass.getJoinClosure() ) {
			isInverseTable[j] = join.isInverse();
			isNullableTable[j] = join.isOptional();

			j++;
		}

		final var sqlStringGenerationContext = creationContext.getSqlStringGenerationContext();

		// PROPERTIES
		final int hydrateSpan = getPropertySpan();
		naturalOrderPropertyTableNumbers = new int[hydrateSpan];
		final var propertyClosure = persistentClass.getPropertyClosure();
		for ( int i = 0; i < propertyClosure.size(); i++ ) {
			final String tableName =
					propertyClosure.get(i).getValue().getTable()
							.getQualifiedName( sqlStringGenerationContext );
			naturalOrderPropertyTableNumbers[i] = getTableId( tableName, naturalOrderTableNames );
		}

		// subclass closure properties

		//TODO: code duplication with SingleTableEntityPersister

		final ArrayList<Integer> columnTableNumbers = new ArrayList<>();
		final ArrayList<Integer> propTableNumbers = new ArrayList<>();
		final ArrayList<String> columns = new ArrayList<>();

		for ( var property : persistentClass.getSubclassPropertyClosure() ) {
			final String tableName = property.getValue().getTable().
					getQualifiedName( sqlStringGenerationContext );
			final Integer tableNumber = getTableId( tableName, subclassTableNameClosure );
			final Integer naturalTableNumber = getTableId( tableName, naturalOrderSubclassTableNameClosure );
			propTableNumbers.add( tableNumber );

			for ( var selectable : property.getSelectables() ) {
				if ( selectable instanceof Column column ) {
					columnTableNumbers.add( naturalTableNumber );
					columns.add( column.getQuotedName( dialect ) );
				}
			}
		}

		subclassColumnNaturalOrderTableNumberClosure = toIntArray( columnTableNumbers );
		subclassPropertyTableNumberClosure = toIntArray( propTableNumbers );
		subclassColumnClosure = toStringArray( columns );

		// SUBCLASSES

		final int subclassSpan = persistentClass.getSubclassSpan() + 1;
		final int subclassSpanMinusOne = subclassSpan - 1;
		if ( !persistentClass.isPolymorphic() ) {
			discriminatorValuesByTableName = emptyMap();
			discriminatorColumnNameByTableName = emptyMap();
			discriminatorValues = null;
			discriminatorAbstract = null;
			notNullColumnTableNumbers = null;
			notNullColumnNames = null;
		}
		else {
			subclassesByDiscriminatorValue.put( discriminatorValue, getEntityName() );

			discriminatorValuesByTableName = linkedMapOfSize( subclassSpan + 1 );
			discriminatorColumnNameByTableName = linkedMapOfSize( subclassSpan + 1 );

			final var table = persistentClass.getTable();
			discriminatorValues = new String[subclassSpan];
			discriminatorAbstract = new boolean[subclassSpan];
			initDiscriminatorProperties(
					dialect,
					subclassSpanMinusOne,
					table,
					castNonNull( discriminatorValue ),
					isAbstract( persistentClass)
			);

			notNullColumnTableNumbers = new int[subclassSpan];
			final int id = getTableId(
					table.getQualifiedName( sqlStringGenerationContext ),
					subclassTableNameClosure
			);
			notNullColumnTableNumbers[subclassSpanMinusOne] = id;
			notNullColumnNames = new String[subclassSpan];
			notNullColumnNames[subclassSpanMinusOne] = subclassTableKeyColumnClosure[id][0];

			final var subclasses = persistentClass.getSubclasses();
			for ( int k = 0; k < subclasses.size(); k++ ) {
				final var subclass = subclasses.get(k);
				final var subclassTable = subclass.getTable();
				if ( persistentClass.isPolymorphic() ) {
					final DiscriminatorValue discriminatorValue = explicitDiscriminatorColumnName != null
							? DiscriminatorHelper.getDiscriminatorValue( subclass )
							// we now use subclass ids that are consistent across all
							// persisters for a class hierarchy, so that the use of
							// "foo.class = Bar" works in HQL
							: new DiscriminatorValue.Literal( subclass.getSubclassId() );
					initDiscriminatorProperties( dialect, k, subclassTable, discriminatorValue, isAbstract( subclass ) );
					subclassesByDiscriminatorValue.put( discriminatorValue, subclass.getEntityName() );
					final int tableId = getTableId(
							subclassTable.getQualifiedName( sqlStringGenerationContext ),
							subclassTableNameClosure
					);
					notNullColumnTableNumbers[k] = tableId;
					notNullColumnNames[k] = subclassTableKeyColumnClosure[tableId][0];
				}
			}
		}

		subclassNamesBySubclassTable = buildSubclassNamesBySubclassTableMapping(
				persistentClass,
				sqlStringGenerationContext
		);
	}

	private void initDiscriminatorProperties(
			@Nonnull Dialect dialect,
			int k,
			@Nonnull Table table,
			@Nonnull DiscriminatorValue discriminatorValue,
			boolean isAbstract) {
		final String tableName = determineTableName( table );
		final String columnName = table.getPrimaryKey().getColumn( 0 ).getQuotedName( dialect );
		discriminatorValuesByTableName.put( tableName, discriminatorValue );
		discriminatorColumnNameByTableName.put( tableName, columnName );
		if ( discriminatorValue instanceof DiscriminatorValue.Literal literal ) {
			castNonNull( discriminatorValues )[k] = String.valueOf( literal.value() );
		}
		else if ( discriminatorValue == DiscriminatorValue.Special.NULL ) {
			castNonNull( discriminatorValues )[k] = "null";
		}
		else {
			castNonNull( discriminatorValues )[k] = "not null";
		}
		castNonNull( discriminatorAbstract )[k] = isAbstract;
	}

	@Nonnull
	@Override
	public Map<DiscriminatorValue, String> getSubclassByDiscriminatorValue() {
		return subclassesByDiscriminatorValue;
	}

	/**
	 * Used to hold the name of subclasses that each "subclass table" is part of.  For example, given a hierarchy like:
	 * {@code JoinedEntity <- JoinedEntitySubclass <- JoinedEntitySubSubclass}..
	 * <p>
	 * For the persister for JoinedEntity, we'd have:
	 * <pre>
	 *	 subclassClosure[0] = "JoinedEntitySubSubclass"
	 *	 subclassClosure[1] = "JoinedEntitySubclass"
	 *	 subclassClosure[2] = "JoinedEntity"
	 *
	 *	 subclassTableNameClosure[0] = "T_JoinedEntity"
	 *	 subclassTableNameClosure[1] = "T_JoinedEntitySubclass"
	 *	 subclassTableNameClosure[2] = "T_JoinedEntitySubSubclass"
	 *
	 *	 subclassNameClosureBySubclassTable[0] = ["JoinedEntitySubSubclass", "JoinedEntitySubclass"]
	 *	 subclassNameClosureBySubclassTable[1] = ["JoinedEntitySubSubclass"]
	 * </pre>
	 * <p>
	 * Note that there are only 2 entries in subclassNameClosureBySubclassTable.  That is because there are really only
	 * 2 tables here that make up the subclass mapping, the others make up the class/superclass table mappings.  We
	 * do not need to account for those here.  The "offset" is defined by the value of {@link #getTableSpan()}.
	 * Therefore the corresponding row in subclassNameClosureBySubclassTable for a given row in subclassTableNameClosure
	 * is calculated as {@code subclassTableNameClosureIndex - getTableSpan()}.
	 * <p>
	 * As we consider each subclass table we can look into this array based on the subclass table's index and see
	 * which subclasses would require it to be included.  E.g., given {@code TREAT( x AS JoinedEntitySubSubclass )},
	 * when trying to decide whether to include join to "T_JoinedEntitySubclass" (subclassTableNameClosureIndex = 1),
	 * we'd look at {@code subclassNameClosureBySubclassTable[0]} and see if the TREAT-AS subclass name is included in
	 * its values.  Since {@code subclassNameClosureBySubclassTable[1]} includes "JoinedEntitySubSubclass", we'd
	 * consider it included.
	 * <p>
	 * {@link #subclassTableNameClosure} also accounts for secondary tables and we properly handle those as we
	 * build the subclassNamesBySubclassTable array and they are therefore properly handled when we use it
	 */
	private final String[][] subclassNamesBySubclassTable;

	/**
	 * Essentially we are building a mapping that we can later use to determine whether a given "subclass table"
	 * should be included in joins when JPA TREAT-AS is used.
	 *
	 * @return subclassNamesBySubclassTable
	 */
	@Nonnull
	private String[][] buildSubclassNamesBySubclassTableMapping(
			@Nonnull PersistentClass persistentClass,
			@Nonnull SqlStringGenerationContext context) {
		// this value represents the number of subclasses (and not the class itself)
		final int numberOfSubclassTables = subclassTableNameClosure.length - coreTableSpan;
		if ( numberOfSubclassTables == 0 ) {
			return new String[0][];
		}
		else {
			final var mapping = new String[numberOfSubclassTables][];
			processPersistentClassHierarchy( persistentClass, true, mapping, context );
			return mapping;
		}
	}

	@Nonnull
	private Set<String> processPersistentClassHierarchy(
			@Nonnull PersistentClass persistentClass,
			boolean isBase,
			@Nonnull String[][] mapping,
			@Nonnull SqlStringGenerationContext context) {
		// collect all the class names that indicate that the "main table"
		// of the given PersistentClass should be included when one of the
		// collected class names is used in TREAT
		final Set<String> classNames = new HashSet<>();
		for ( var subclass : persistentClass.getDirectSubclasses() ) {
			classNames.addAll( processPersistentClassHierarchy( subclass, false, mapping, context ) );
		}
		classNames.add( persistentClass.getEntityName() );
		if ( !isBase ) {
			var mappedSuperclass = persistentClass.getSuperMappedSuperclass();
			while ( mappedSuperclass != null ) {
				classNames.add( mappedSuperclass.getMappedClass().getName() );
				mappedSuperclass = mappedSuperclass.getSuperMappedSuperclass();
			}
			associateSubclassNamesToSubclassTableIndexes( persistentClass, classNames, mapping, context );
		}
		return classNames;
	}

	private void associateSubclassNamesToSubclassTableIndexes(
			@Nonnull PersistentClass persistentClass,
			@Nonnull Set<String> classNames,
			@Nonnull String[][] mapping,
			@Nonnull SqlStringGenerationContext context) {
		final String tableName = persistentClass.getTable().getQualifiedName( context );
		associateSubclassNamesToSubclassTableIndex( tableName, classNames, mapping );
		for ( var join : persistentClass.getJoins() ) {
			final String secondaryTableName = join.getTable().getQualifiedName( context );
			associateSubclassNamesToSubclassTableIndex( secondaryTableName, classNames, mapping );
		}
	}

	private void associateSubclassNamesToSubclassTableIndex(
			@Nonnull String tableName,
			@Nonnull Set<String> classNames,
			@Nonnull String[][] mapping) {
		// find the table's entry in the subclassTableNameClosure array
		boolean found = false;
		for ( int i = 1; i < subclassTableNameClosure.length; i++ ) {
			if ( subclassTableNameClosure[i].equals( tableName ) ) {
				found = true;
				final int index = i - coreTableSpan;
				if ( index < 0 || index >= mapping.length ) {
					throw new IllegalStateException(
							String.format(
									"Encountered 'subclass table index' [%s] was outside expected range ( [%s] < i < [%s] )",
									index,
									0,
									mapping.length
							)
					);
				}
				mapping[index] = toStringArray( classNames );
				break;
			}
		}
		if ( !found ) {
			throw new IllegalStateException(
					String.format(
							"Was unable to locate subclass table [%s] in 'subclassTableNameClosure'",
							tableName
					)
			);
		}
	}

	@Override
	public boolean needsDiscriminator() {
		return forceDiscriminator;
	}

	@Override
	public boolean isNullableTable(int j) {
		return isNullableTable[j];
	}

	@Override
	protected void visitMutabilityOrderedTables(@Nonnull MutabilityOrderedTableConsumer consumer) {
		for ( int i = 0; i < naturalOrderTableNames.length; i++ ) {
			final String tableName = naturalOrderTableNames[i];
			final int tableIndex = i;
			consumer.consume(
					tableName,
					tableIndex,
					() -> columnConsumer -> columnConsumer.accept(
							getIdentifierMapping(),
							tableName,
							naturalOrderTableKeyColumns[tableIndex]
					)
			);
		}
	}

	@Override
	protected boolean isIdentifierTable(@Nonnull String tableExpression) {
		return tableExpression.equals( getRootTableName() );
	}

	@Override
	protected boolean isSecondaryTable(@Nonnull String tableExpression, int relativePosition) {
		// In JoinedSubclassEntityPersister, secondary tables come after inheritance tables.
		// Tables at positions >= subclassCoreTableSpan are from getSubclassJoinClosure() (secondary tables)
		// Tables at positions < subclassCoreTableSpan are from getSubclassTableClosure() (inheritance tables)
		return relativePosition >= subclassCoreTableSpan;
	}

	@Override
	public boolean isInverseTable(int j) {
		return isInverseTable[j];
	}

	@Nonnull
	@Override
	public String getAttributeMutationTableName(int i) {
		return subclassTableNameClosure[subclassPropertyTableNumberClosure[i]];
	}

	@Override
	protected boolean isNullableSubclassTable(int j) {
		return isNullableSubclassTable[j];
	}

	@Nullable
	@Override
	public BasicType<?> getDiscriminatorType() {
		return discriminatorType;
	}

	@Nullable
	@Override
	public DiscriminatorValue getDiscriminatorValue() {
		return discriminatorValue;
	}

	@Nullable
	@Override
	public String getDiscriminatorSQLValue() {
		return discriminatorSQLString;
	}

	@Nullable
	@Override
	public String getDiscriminatorColumnName() {
		return explicitDiscriminatorColumnName == null
				? super.getDiscriminatorColumnName()
				: explicitDiscriminatorColumnName;
	}

	@Nullable
	@Override
	public String getDiscriminatorColumnReaders() {
		return getDiscriminatorColumnName();
	}

	@Nullable
	@Override
	public String getDiscriminatorAlias() {
		return discriminatorAlias;
	}

	@Override
	public void addDiscriminatorToInsertGroup(@Nonnull MutationGroupBuilder insertGroupBuilder) {
		if ( explicitDiscriminatorColumnName != null && discriminatorInsertable ) {
			final TableInsertBuilder tableInsertBuilder =
					insertGroupBuilder.getTableDetailsBuilder( getRootTableName() );
			tableInsertBuilder.addColumnAssignment(
					getDiscriminatorMapping(),
					getDiscriminatorValueString()
			);
		}
	}

	@Nonnull
	@Override
	public EntityTableDescriptor getIdentifierTableDescriptor() {
		final var superMappingType = getSuperMappingType();
		return superMappingType == null
				? getTableDescriptors()[0]
				: getRootEntityDescriptor().getEntityPersister().getIdentifierTableDescriptor();
	}

	private static boolean isDiscriminatorInsertable(@Nonnull PersistentClass persistentClass) {
		return !persistentClass.isDiscriminatorValueNull()
			&& !persistentClass.isDiscriminatorValueNotNull()
			&& persistentClass.isDiscriminatorInsertable()
			&& !persistentClass.getDiscriminator().hasFormula();
	}

	@Override
	public void addDiscriminatorToInsertGroup(@Nonnull Function<String, TableInsertBuilder> insertGroupBuilder) {
		if ( explicitDiscriminatorColumnName != null && discriminatorInsertable ) {
			final TableInsertBuilder tableInsertBuilder = insertGroupBuilder.apply( getRootTableName() );
			if ( discriminatorValue == DiscriminatorValue.Special.NULL ) {
				tableInsertBuilder.addColumnAssignment(	getDiscriminatorMapping(), TableMutationBuilder.NULL );
			}
			else if ( discriminatorValue == DiscriminatorValue.Special.NOT_NULL ) {
				tableInsertBuilder.addColumnAssignment(	getDiscriminatorMapping(), TableMutationBuilder.NOT_NULL );
			}
			else {
				tableInsertBuilder.addColumnAssignment(	getDiscriminatorMapping() );
			}
		}
	}

	@Override
	public void bindDiscriminatorForInsert(@Nonnull JdbcValueBindings jdbcValueBindings) {
		if ( explicitDiscriminatorColumnName != null
				&& discriminatorInsertable
				&& discriminatorValue != DiscriminatorValue.Special.NULL
				&& discriminatorValue != DiscriminatorValue.Special.NOT_NULL ) {
			jdbcValueBindings.bindAssignment( -1, castNonNull( discriminatorValue ).value(), getDiscriminatorMapping() );
		}
	}

	@Override
	public void addDiscriminatorToDelete(@Nonnull TableDeleteBuilder tableDeleteBuilder) {
		if ( explicitDiscriminatorColumnName != null ) {
			if ( discriminatorValue == DiscriminatorValue.Special.NULL ) {
				tableDeleteBuilder.addNonKeyRestriction( getDiscriminatorMapping(), TableMutationBuilder.NULL );
			}
			else if ( discriminatorValue == DiscriminatorValue.Special.NOT_NULL ) {
				tableDeleteBuilder.addNonKeyRestriction( getDiscriminatorMapping(), TableMutationBuilder.NOT_NULL );
			}
			else {
				tableDeleteBuilder.addNonKeyRestriction( getDiscriminatorMapping() );
			}
		}
	}

	@Override
	public void bindDiscriminatorForDelete(@Nonnull JdbcValueBindings jdbcValueBindings) {
		if ( explicitDiscriminatorColumnName != null
				&& discriminatorValue != DiscriminatorValue.Special.NULL
				&& discriminatorValue != DiscriminatorValue.Special.NOT_NULL ) {
			jdbcValueBindings.bindRestriction( -1, castNonNull( discriminatorValue ).value(), getDiscriminatorMapping() );
		}
	}

	@Nullable
	private String getDiscriminatorValueString() {
		if ( discriminatorValue == DiscriminatorValue.Special.NULL ) {
			return "null";
		}
		else if ( discriminatorValue == DiscriminatorValue.Special.NOT_NULL ) {
			return "not null";
		}
		else {
			return discriminatorSQLString;
		}
	}

	@Nonnull
	@Override
	public String[] getPropertySpaces() {
		return spaces; // don't need subclass tables, because they can't appear in conditions
	}

	@Override
	public boolean hasDuplicateTables() {
		return hasDuplicateTables;
	}

	@Nonnull
	@Override
	public String getTableName(int j) {
		return naturalOrderTableNames[j];
	}

	@Nonnull
	@Override
	public String[] getKeyColumns(int j) {
		return naturalOrderTableKeyColumns[j];
	}

	@Override
	public boolean isTableCascadeDeleteEnabled(int j) {
		return naturalOrderCascadeDeleteEnabled[j];
	}

	@Nonnull
	@Override
	protected TableMutationDetails createTableMutationDetails(@Nonnull PersistentClass bootEntityDescriptor, int relativePosition) {
		if ( relativePosition < coreTableSpan ) {
			PersistentClass currentClass = bootEntityDescriptor;
			int currentPosition = coreTableSpan - 1;
			while ( currentClass != null ) {
				if ( currentPosition == relativePosition ) {
					return createTableMutationDetails( currentClass );
				}
				currentPosition--;
				currentClass = currentClass.getSuperclass();
			}
			throw new AssertionFailure( "Could not resolve table mutation details for table position " + relativePosition );
		}
		else {
			return createTableMutationDetails( bootEntityDescriptor.getJoinClosure().get( relativePosition - coreTableSpan ) );
		}
	}

	@Override
	public boolean isPropertyOfTable(int property, int j) {
		return naturalOrderPropertyTableNumbers[property] == j;
	}

	@Nonnull
	@Override
	public String getTableName() {
		return tableNames[0];
	}

	@Nonnull
	@Override
	public String[] getIdentifierColumnNames() {
		return tableKeyColumns[0];
	}

	@Nonnull
	@Override
	public String getRootTableName() {
		return naturalOrderTableNames[0];
	}

	@Override
	public int getTableSpan() {
		return tableSpan;
	}

	@Override
	public boolean hasMultipleTables() {
		return true;
	}

	@Nonnull
	@Override
	protected int[] getPropertyTableNumbers() {
		return naturalOrderPropertyTableNumbers;
	}

	@Nonnull
	@Override
	protected String[] getSubclassTableKeyColumns(int j) {
		return subclassTableKeyColumnClosure[j];
	}

	@Nonnull
	@Override
	public String getSubclassTableName(int j) {
		return subclassTableNameClosure[j];
	}

	@Nonnull
	@Override
	protected String[] getSubclassTableNames() {
		return subclassTableNameClosure;
	}

	@Override
	public int getSubclassTableSpan() {
		return subclassTableNameClosure.length;
	}

	@Override
	protected boolean shouldProcessSuperMapping() {
		return false;
	}

	@Override
	protected boolean isClassOrSuperclassTable(int j) {
		return isClassOrSuperclassTable[j];
	}

	@Override
	protected boolean isSubclassTableIndicatedByTreatAsDeclarations(
			int subclassTableNumber,
			@Nullable Set<String> treatAsDeclarations) {
		if ( treatAsDeclarations != null && !treatAsDeclarations.isEmpty() ) {
			final var inclusionSubclassNameClosure =
					getSubclassNameClosureBySubclassTable( subclassTableNumber );
			// NOTE: we assume the entire hierarchy is joined-subclass here
			for ( String subclassName : treatAsDeclarations ) {
				for ( String inclusionSubclassName : inclusionSubclassNameClosure ) {
					if ( inclusionSubclassName.equals( subclassName ) ) {
						return true;
					}
				}
			}
		}
		return false;
	}

	@Nonnull
	private String[] getSubclassNameClosureBySubclassTable(int subclassTableNumber) {
		final int index = subclassTableNumber - getTableSpan();
		if ( index >= subclassNamesBySubclassTable.length ) {
			throw new IllegalArgumentException(
					"Given subclass table number is outside expected range [" + (subclassNamesBySubclassTable.length -1)
							+ "] as defined by subclassTableNameClosure/subclassClosure"
			);
		}
		return subclassNamesBySubclassTable[index];
	}

	@Nonnull
	@Override
	public String[] getConstraintOrderedTableNameClosure() {
		return constraintOrderedTableNames;
	}

	@Nonnull
	@Override
	public String[][] getConstraintOrderedTableKeyColumnClosure() {
		return constraintOrderedKeyColumnNames;
	}

	@Override
	protected int determineTableNumberForColumn(@Nonnull String columnName) {
		// HHH-7630: In case the naturalOrder/identifier column is explicitly given in the ordering, check here.
		for ( int i = 0, max = naturalOrderTableKeyColumns.length; i < max; i++ ) {
			final var keyColumns = naturalOrderTableKeyColumns[i];
			if ( contains( keyColumns, columnName ) ) {
				return naturalOrderPropertyTableNumbers[i];
			}
		}

		for ( int i = 0, max = subclassColumnClosure.length; i < max; i++ ) {
			final String subclassColumn = subclassColumnClosure[i];
			final boolean quoted =
					subclassColumn.startsWith( "\"" )
					&& subclassColumn.endsWith( "\"" );
			if ( quoted ) {
				if ( subclassColumn.equals( columnName ) ) {
					return subclassColumnNaturalOrderTableNumberClosure[i];
				}
			}
			else {
				if ( subclassColumn.equalsIgnoreCase( columnName ) ) {
					return subclassColumnNaturalOrderTableNumberClosure[i];
				}
			}
		}
		throw new HibernateException(
				"Could not locate table which owns column [" + columnName + "] referenced in order-by mapping - " + getEntityName()
		);
	}

	@Nonnull
	@Override
	public Object forceVersionIncrement(@Nonnull Object id, @Nullable Object currentVersion, @Nonnull SharedSessionContractImplementor session) {
		final var superMappingType = getSuperMappingType();
		return superMappingType != null
				? superMappingType.getEntityPersister().forceVersionIncrement( id, currentVersion, session )
				: super.forceVersionIncrement( id, currentVersion, session );
	}

	@Nonnull
	@Override
	public Object forceVersionIncrement(
			@Nonnull Object id,
			@Nullable Object currentVersion,
			boolean batching,
			@Nonnull SharedSessionContractImplementor session) throws HibernateException {
		final var superMappingType = getSuperMappingType();
		return superMappingType != null
				? superMappingType.getEntityPersister().forceVersionIncrement( id, currentVersion, session )
				: super.forceVersionIncrement( id, currentVersion, batching, session );
	}

	@Nullable
	@Override
	protected EntityVersionMapping generateVersionMapping(
			@Nullable Supplier<?> templateInstanceCreator,
			@Nonnull PersistentClass bootEntityDescriptor,
			@Nonnull MappingModelCreationProcess creationProcess) {
		if ( getVersionType() == null ) {
			return null;
		}
		else {
			if ( getTableName().equals( getVersionedTableName() ) ) {
				final String versionPropertyName = getPropertyNames()[getVersionPropertyIndex()];
				return creationProcess.processSubPart(
						versionPropertyName,
						(role, process) -> generateVersionMapping(
								this,
								templateInstanceCreator,
								bootEntityDescriptor,
								process
						)
				);
			}
			else if ( getSuperMappingType() != null ) {
				return getSuperMappingType().getVersionMapping();
			}
		}
		return null;
	}

	@Nonnull
	@Override
	protected EntityIdentifierMapping generateIdentifierMapping(
			@Nullable Supplier<?> templateInstanceCreator,
			@Nonnull PersistentClass persistentClass,
			@Nonnull MappingModelCreationProcess creationProcess) {
		final var idType = getIdentifierType();
		if ( idType instanceof CompositeType compositeIdType ) {
			return compositeIdentifierMapping( persistentClass, creationProcess, compositeIdType );
		}
		else if ( idType instanceof BasicType<?> basicIdType ) {
			return basicIdentifierMapping( templateInstanceCreator, persistentClass, creationProcess, basicIdType );
		}
		else {
			throw new AssertionFailure( "Unrecognized id type" );
		}
	}

	@Nonnull
	private BasicEntityIdentifierMappingImpl basicIdentifierMapping(
			@Nullable Supplier<?> templateInstanceCreator,
			@Nonnull PersistentClass persistentClass,
			@Nonnull MappingModelCreationProcess creationProcess,
			@Nonnull BasicType<?> idType) {
		final var identifier = persistentClass.getIdentifier();
		final Long length;
		final Integer arrayLength;
		final Integer precision;
		final Integer scale;
		if ( identifier == null ) {
			length = null;
			arrayLength = null;
			precision = null;
			scale = null;
		}
		else {
			final var column = identifier.getColumns().get( 0 );
			length = column.getLength();
			arrayLength = column.getArrayLength();
			precision = column.getPrecision();
			scale = column.getScale();
		}
		final var identifierProperty = persistentClass.getIdentifierProperty();
		final var value = identifierProperty.getValue();
		return new BasicEntityIdentifierMappingImpl(
				this,
				templateInstanceCreator,
				identifierProperty.getName(),
				getTableName(),
				tableKeyColumns[0][0],
				length,
				arrayLength,
				precision,
				scale,
				value.isColumnInsertable( 0 ),
				value.isColumnUpdateable( 0 ),
				idType,
				creationProcess
		);
	}

	@Nonnull
	private EntityIdentifierMapping compositeIdentifierMapping(
			@Nonnull PersistentClass persistentClass,
			@Nonnull MappingModelCreationProcess creationProcess,
			@Nonnull CompositeType compositeIdType) {
		// NOTE: the term `isEmbedded` here uses Hibernate's older (pre-JPA) naming for its
		//       "non-aggregated" composite-id support. It unfortunately conflicts with the
		//       JPA usage of "embedded".  Here we normalize the legacy naming to the more
		//       descriptive encapsulated versus non-encapsulated phrasing
		final boolean encapsulated = !compositeIdType.isEmbedded();
		if ( encapsulated ) {
			// we have an `@EmbeddedId`
			final var identifierProperty = persistentClass.getIdentifierProperty();
			return buildEncapsulatedCompositeIdentifierMapping(
					this,
					identifierProperty,
					identifierProperty.getName(),
					getTableName(),
					tableKeyColumns[0],
					compositeIdType,
					creationProcess
			);
		}
		else {
			// otherwise we have a non-encapsulated composite-identifier
			return generateNonEncapsulatedCompositeIdentifierMapping( creationProcess, persistentClass );
		}
	}

	@Override
	protected boolean isPhysicalDiscriminator() {
		return explicitDiscriminatorColumnName != null;
	}

	@Nullable
	@Override
	protected EntityDiscriminatorMapping generateDiscriminatorMapping(@Nonnull PersistentClass bootEntityDescriptor) {
		final var superMappingType = getSuperMappingType();
		if ( superMappingType != null ) {
			return superMappingType.getDiscriminatorMapping();
		}
		else if ( hasSubclasses() ) {
			final String formula = getDiscriminatorFormulaTemplate();
			if ( explicitDiscriminatorColumnName != null || formula != null ) {
				// even though this is a JOINED hierarchy the user has defined an
				// explicit discriminator column - so we can use the normal
				// discriminator mapping
				return super.generateDiscriminatorMapping( bootEntityDescriptor );
			}
			else {
				// otherwise, we need to use the case approach
				return new CaseStatementDiscriminatorMappingImpl(
						this,
						subclassTableNameClosure,
						castNonNull( notNullColumnTableNumbers ),
						castNonNull( notNullColumnNames ),
						castNonNull( discriminatorValues ),
						castNonNull( discriminatorAbstract ),
						castNonNull( getDiscriminatorDomainType() )
				);
			}
		}
		else {
			return null;
		}
	}

	@Nonnull
	@Override
	protected EntityIdentifierMapping generateNonEncapsulatedCompositeIdentifierMapping(
			@Nonnull MappingModelCreationProcess creationProcess,
			@Nonnull PersistentClass bootEntityDescriptor) {
		assert declaredAttributeMappings != null;
		return buildNonEncapsulatedCompositeIdentifierMapping(
				this,
				getTableName(),
				tableKeyColumns[0],
				bootEntityDescriptor,
				creationProcess
		);
	}

	@Nonnull
	@Override
	public FilterAliasGenerator getFilterAliasGenerator(@Nonnull String rootAlias) {
		return new DynamicFilterAliasGenerator(subclassTableNameClosure, rootAlias);
	}

	@Override
	public void forEachTableDetails(@Nonnull Consumer<TableDetails> consumer) {
		super.forEachTableDetails( consumer );
	}

	@Nonnull
	@Override
	public TableDetails getMappedTableDetails() {
		// Subtract the number of secondary tables (tableSpan - coreTableSpan) and get the last table mapping
		return getTableMapping( getTableMappings().length - ( tableSpan - coreTableSpan ) - 1 );
	}

	@Nonnull
	@Override
	public TableDetails getIdentifierTableDetails() {
		final var superMappingType = getSuperMappingType();
		return superMappingType == null
				? getMappedTableDetails()
				: getRootEntityDescriptor().getIdentifierTableDetails();
	}

	@Override
	public void pruneForSubclasses(@Nonnull TableGroup tableGroup, @Nonnull Map<String, EntityNameUse> entityNameUses) {
		final Set<TableReference> retainedTableReferences = new HashSet<>( entityNameUses.size() );
		final var metamodel = getFactory().getMappingMetamodel();
		// We can only do this optimization if the table group reports canUseInnerJoins or isRealTableGroup,
		// because the switch for table reference joins to INNER must be cardinality preserving.
		// If canUseInnerJoins is true, this is trivially given, but also if the table group is real
		// i.e. with parenthesis around, as that means the table reference joins will be isolated
		final boolean innerJoinOptimization = tableGroup.canUseInnerJoins() || tableGroup.isRealTableGroup();
		final Set<String> tablesToInnerJoin = innerJoinOptimization ? new HashSet<>() : null;
		boolean needsTreatDiscriminator = false;
		for ( var entry : entityNameUses.entrySet() ) {
			final var useKind = entry.getValue().getKind();
			final var persister = (JoinedSubclassEntityPersister) metamodel.findEntityDescriptor( entry.getKey() );
			// The following block tries to figure out what can be inner joined and which super class table joins can be omitted
			if ( innerJoinOptimization ) {
				optimizeInnerJoins( tableGroup, useKind, persister, tablesToInnerJoin, retainedTableReferences );
			}
			final String tableName = persister.getTableName();
			final var mainTableReference = tableGroup.getTableReference( null, tableName, false );
			if ( mainTableReference != null ) {
				retainedTableReferences.add( mainTableReference );
			}
			final String sqlWhereStringTableExpression = persister.getSqlWhereStringTableExpression();
			if ( sqlWhereStringTableExpression != null ) {
				final var tableReference = tableGroup.getTableReference( sqlWhereStringTableExpression );
				if ( tableReference != null ) {
					retainedTableReferences.add( tableReference );
				}
			}
			if ( needsDiscriminator() ) {
				// We allow multiple joined subclasses to use the same table if they define a discriminator column.
				// In this case, we might need to add a discriminator condition to make sure we filter the correct subtype,
				// see SingleTableEntityPersister#pruneForSubclasses for more details on this condition
				needsTreatDiscriminator = needsTreatDiscriminator || !persister.isAbstract()
						&& useKind == EntityNameUse.UseKind.TREAT && ( isInherited() || !isTypeOrSuperType( persister ) );
			}
		}
		// If no tables to inner join have been found, we add at least the super class tables of this persister
		if ( innerJoinOptimization && tablesToInnerJoin.isEmpty() ) {
			final var subclassTableNames = getSubclassTableNames();
			for ( int i = 0; i < subclassTableNames.length; i++ ) {
				if ( isClassOrSuperclassTable[i] ) {
					tablesToInnerJoin.add( subclassTableNames[i] );
				}
			}
		}

		final var tableReferenceJoins = tableGroup.getTableReferenceJoins();
		if ( needsTreatDiscriminator ) {
			if ( tableReferenceJoins.isEmpty() ) {
				// We need to apply the discriminator predicate to the primary table reference itself
				final String discriminatorPredicate =
						getPrunedDiscriminatorPredicate( entityNameUses, metamodel, "t" );
				if ( discriminatorPredicate != null ) {
					final var tableReference = (NamedTableReference) tableGroup.getPrimaryTableReference();
					tableReference.setPrunedTableExpression( "(select * from " + getRootTableName() + " t where " + discriminatorPredicate + ")" );
				}
			}
			else {
				// We have to apply the discriminator condition to the root table reference join
				boolean applied = applyDiscriminatorPredicate(
						tableReferenceJoins.get( 0 ),
						(NamedTableReference) tableGroup.getPrimaryTableReference(),
						entityNameUses,
						metamodel
				);
				int i = 0;
				for ( ; !applied && i < tableReferenceJoins.size(); i++ ) {
					final var join = tableReferenceJoins.get( i );
					applied = applyDiscriminatorPredicate( join, join.getJoinedTableReference(), entityNameUses, metamodel );
				}
				assert applied : "Could not apply treat discriminator predicate to root table join";
				if ( i != 0 ) {
					// Always retain the root table reference join where the discriminator was applied
					retainedTableReferences.add( tableReferenceJoins.get( i - 1 ).getJoinedTableReference() );
				}
			}
		}
		// Retain resolved secondary-table joins, including those mapped to auxiliary audit tables.
		for ( int i = subclassCoreTableSpan; i < subclassTableNameClosure.length; i++ ) {
			final var tableReference =
					tableGroup.getTableReference( null, subclassTableNameClosure[i], false );
			if ( tableReference != null ) {
				retainedTableReferences.add( tableReference );
			}
		}
		if ( !tableReferenceJoins.isEmpty() ) {
			// The optimization is to remove all table reference joins that are not contained in the retainedTableReferences
			// In addition, we switch from a possible LEFT join, to an INNER join for all tablesToInnerJoin
			if ( innerJoinOptimization ) {
				final var oldJoins = tableReferenceJoins.toArray( new TableReferenceJoin[0] );
				tableReferenceJoins.clear();
				for ( var oldJoin : oldJoins ) {
					final var joinedTableReference = oldJoin.getJoinedTableReference();
					if ( retainedTableReferences.contains( joinedTableReference ) ) {
						final var join =
								oldJoin.getJoinType() != SqlAstJoinType.INNER
									&& tablesToInnerJoin.contains( joinedTableReference.getTableExpression() )
										? new TableReferenceJoin( true, joinedTableReference, oldJoin.getPredicate() )
										: oldJoin;
						tableReferenceJoins.add( join );
					}
				}
			}
			else {
				tableReferenceJoins.removeIf( join -> !retainedTableReferences.contains( join.getJoinedTableReference() ) );
			}
		}
	}

	private void optimizeInnerJoins(
			@Nonnull TableGroup tableGroup,
			@Nonnull EntityNameUse.UseKind useKind,
			@Nonnull JoinedSubclassEntityPersister persister,
			@Nonnull Set<String> tablesToInnerJoin,
			@Nonnull Set<TableReference> retainedTableReferences) {
		if ( useKind == EntityNameUse.UseKind.TREAT || useKind == EntityNameUse.UseKind.FILTER ) {
			final var subclassTableNames = persister.getSubclassTableNames();
			// Build the intersection of all tables names that are of the class or super class
			// These are the tables that can be safely inner joined
			final Set<String> classOrSuperclassTables = new HashSet<>( subclassTableNames.length );
			for ( int i = 0; i < subclassTableNames.length; i++ ) {
				if ( persister.isClassOrSuperclassTable[i] ) {
					classOrSuperclassTables.add( subclassTableNames[i] );
				}
			}
			if ( tablesToInnerJoin.isEmpty() ) {
				tablesToInnerJoin.addAll( classOrSuperclassTables );
			}
			else {
				tablesToInnerJoin.retainAll( classOrSuperclassTables );
			}
			if ( useKind == EntityNameUse.UseKind.FILTER && explicitDiscriminatorColumnName == null ) {
				// If there is no discriminator column,
				// we must retain all joins to subclass tables to be able to discriminate the rows
				for ( int i = 0; i < subclassTableNames.length; i++ ) {
					if ( !persister.isClassOrSuperclassTable[i] ) {
						final String subclassTableName = subclassTableNames[i];
						final var mainTableReference =
								tableGroup.getTableReference( null, subclassTableName, false );
						if ( mainTableReference == null ) {
							throw new UnknownTableReferenceException(
									subclassTableName,
									"Couldn't find table reference"
							);
						}
						retainedTableReferences.add( mainTableReference );
					}
				}
			}
		}
	}

	@Nonnull
	@Override
	public EntityIdentifierMapping getIdentifierMappingForJoin() {
		// If the joined subclass has a physical discriminator and has subtypes
		// we must use the root table identifier mapping for joining to allow table group elimination to work
		return isPhysicalDiscriminator() && !getSubMappingTypes().isEmpty()
				? getRootEntityDescriptor().getIdentifierMapping()
				: super.getIdentifierMappingForJoin();
	}

	private boolean applyDiscriminatorPredicate(
			@Nonnull TableReferenceJoin join,
			@Nonnull NamedTableReference tableReference,
			@Nonnull Map<String, EntityNameUse> entityNameUses,
			@Nonnull MappingMetamodelImplementor metamodel) {
		if ( tableReference.getTableExpression().equals( getRootTableName() ) ) {
			final String discriminatorPredicate =
					getPrunedDiscriminatorPredicate( entityNameUses, metamodel, "t" );
			// null means we're filtering for all subtypes, so we don't need to apply a predicate
			if ( discriminatorPredicate != null ) {
				assert join.getJoinType() == SqlAstJoinType.INNER : "Found table reference join with root table of non-INNER type: " + join.getJoinType();
				tableReference.setPrunedTableExpression( "(select * from " + getRootTableName() + " t where " + discriminatorPredicate + ")" );
			}
			return true;
		}
		return false;
	}

	@Override
	public void visitConstraintOrderedTables(@Nonnull ConstraintOrderedTableConsumer consumer) {
		for ( int i = 0; i < constraintOrderedTableNames.length; i++ ) {
			final String tableName = constraintOrderedTableNames[i];
			final int tablePosition = i;
			consumer.consume(
					tableName,
					() -> columnConsumer -> columnConsumer.accept(
							tableName,
							constraintOrderedKeyColumnNames[tablePosition],
							getIdentifierMapping()::getJdbcMapping
					)
			);
		}
	}

}
