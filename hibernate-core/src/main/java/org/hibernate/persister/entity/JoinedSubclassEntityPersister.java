/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.Internal;
import org.hibernate.MappingException;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.NaturalIdDataAccess;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.DynamicFilterAliasGenerator;
import org.hibernate.internal.FilterAliasGenerator;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.jdbc.Expectation;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Formula;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.Subclass;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.Value;
import org.hibernate.metamodel.mapping.EntityDiscriminatorMapping;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.EntityVersionMapping;
import org.hibernate.metamodel.mapping.TableDetails;
import org.hibernate.metamodel.mapping.internal.BasicEntityIdentifierMappingImpl;
import org.hibernate.metamodel.mapping.internal.CaseStatementDiscriminatorMappingImpl;
import org.hibernate.metamodel.mapping.internal.MappingModelCreationProcess;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.persister.internal.SqlFragmentPredicate;
import org.hibernate.persister.spi.PersisterCreationContext;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.from.TableReferenceJoin;
import org.hibernate.sql.ast.tree.from.UnknownTableReferenceException;
import org.hibernate.sql.model.ast.builder.MutationGroupBuilder;
import org.hibernate.sql.model.ast.builder.TableInsertBuilder;
import org.hibernate.type.BasicType;
import org.hibernate.type.BasicTypeRegistry;
import org.hibernate.type.CompositeType;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;
import org.hibernate.type.spi.TypeConfiguration;

import org.jboss.logging.Logger;

import static java.util.Collections.emptyMap;
import static org.hibernate.internal.util.collections.ArrayHelper.to2DStringArray;
import static org.hibernate.internal.util.collections.ArrayHelper.toIntArray;
import static org.hibernate.internal.util.collections.ArrayHelper.toStringArray;
import static org.hibernate.internal.util.collections.CollectionHelper.linkedMapOfSize;
import static org.hibernate.internal.util.collections.CollectionHelper.mapOfSize;
import static org.hibernate.jdbc.Expectations.createExpectation;
import static org.hibernate.metamodel.mapping.internal.MappingModelCreationHelper.buildEncapsulatedCompositeIdentifierMapping;
import static org.hibernate.metamodel.mapping.internal.MappingModelCreationHelper.buildNonEncapsulatedCompositeIdentifierMapping;
import static org.hibernate.persister.entity.DiscriminatorHelper.NOT_NULL_DISCRIMINATOR;
import static org.hibernate.persister.entity.DiscriminatorHelper.NULL_DISCRIMINATOR;

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
	private static final Logger log = Logger.getLogger( JoinedSubclassEntityPersister.class );

	private static final String IMPLICIT_DISCRIMINATOR_ALIAS = "clazz_";

	// the class hierarchy structure
	private final int tableSpan;
	private final boolean hasDuplicateTables;
	private final String[] tableNames;
	private final String[] naturalOrderTableNames;
	private final String[][] tableKeyColumns;
	private final String[][] tableKeyColumnReaders;
	private final String[][] tableKeyColumnReaderTemplates;
	private final String[][] naturalOrderTableKeyColumns;
	private final boolean[] naturalOrderCascadeDeleteEnabled;

	private final String[] spaces;

//	private final String[] subclassClosure;

	private final String[] subclassTableNameClosure;
	private final String[][] subclassTableKeyColumnClosure;
	private final boolean[] isClassOrSuperclassTable;

	// properties of this class, including inherited properties
	private final int[] naturalOrderPropertyTableNumbers;
//	private final int[] propertyTableNumbers;

	// the closure of all properties in the entire hierarchy including
	// subclasses and superclasses of this class
	private final int[] subclassPropertyTableNumberClosure;

	// the closure of all columns used by the entire hierarchy including
	// subclasses and superclasses of this class
	private final int[] subclassColumnNaturalOrderTableNumberClosure;
//	private final int[] subclassFormulaTableNumberClosure;
	private final String[] subclassColumnClosure;

//	private final boolean[] subclassTableSequentialSelect;
//	private final boolean[] subclassTableIsLazyClosure;
	private final boolean[] isInverseSubclassTable;
	private final boolean[] isNullableSubclassTable;

	// subclass discrimination works by assigning particular
	// values to certain combinations of not-null primary key
	// values in the outer join using an SQL CASE
	private final Map<Object,String> subclassesByDiscriminatorValue = new HashMap<>();
	private final String[] discriminatorValues;
	private final boolean[] discriminatorAbstract;
	private final String[] notNullColumnNames;
	private final int[] notNullColumnTableNumbers;

	private final String[] constraintOrderedTableNames;
	private final String[][] constraintOrderedKeyColumnNames;

	private final Object discriminatorValue;
	private final String discriminatorSQLString;
	private final BasicType<?> discriminatorType;
	private final String explicitDiscriminatorColumnName;
	private final String discriminatorAlias;
	private final boolean forceDiscriminator;

	// Span of the tables directly mapped by this entity and super-classes, if any
	private final int coreTableSpan;
	private final int subclassCoreTableSpan;
	// only contains values for SecondaryTables, i.e. not tables part of the "coreTableSpan"
	private final boolean[] isNullableTable;
	private final boolean[] isInverseTable;

	private final Map<String, Object> discriminatorValuesByTableName;
	private final Map<String, String> discriminatorColumnNameByTableName;
	private final Map<String, String> subclassNameByTableName;

	//INITIALIZATION:

	@Deprecated(since = "6.0")
	public JoinedSubclassEntityPersister(
			final PersistentClass persistentClass,
			final EntityDataAccess cacheAccessStrategy,
			final NaturalIdDataAccess naturalIdRegionAccessStrategy,
			final PersisterCreationContext creationContext) throws HibernateException {
		this( persistentClass,cacheAccessStrategy,naturalIdRegionAccessStrategy,
				(RuntimeModelCreationContext) creationContext );
	}

	public JoinedSubclassEntityPersister(
			final PersistentClass persistentClass,
			final EntityDataAccess cacheAccessStrategy,
			final NaturalIdDataAccess naturalIdRegionAccessStrategy,
			final RuntimeModelCreationContext creationContext) throws HibernateException {

		super( persistentClass, cacheAccessStrategy, naturalIdRegionAccessStrategy, creationContext );

		final Dialect dialect = creationContext.getDialect();
		final SqmFunctionRegistry functionRegistry = creationContext.getFunctionRegistry();
		final TypeConfiguration typeConfiguration = creationContext.getTypeConfiguration();
		final BasicTypeRegistry basicTypeRegistry = typeConfiguration.getBasicTypeRegistry();

		// DISCRIMINATOR

		if ( persistentClass.isPolymorphic() ) {
			forceDiscriminator = persistentClass.isForceDiscriminator();
			final Value discriminatorMapping = persistentClass.getDiscriminator();
			if ( discriminatorMapping != null ) {
				log.debug( "Encountered explicit discriminator mapping for joined inheritance" );
				final Selectable selectable = discriminatorMapping.getSelectables().get(0);
				if ( selectable instanceof Formula ) {
					throw new MappingException( "Discriminator formulas on joined inheritance hierarchies not supported at this time" );
				}
				else {
					final Column column = (Column) selectable;
					explicitDiscriminatorColumnName = column.getQuotedName( dialect );
					discriminatorAlias = column.getAlias( dialect, persistentClass.getRootTable() );
				}
				discriminatorType = DiscriminatorHelper.getDiscriminatorType( persistentClass );
				discriminatorValue = DiscriminatorHelper.getDiscriminatorValue( persistentClass );
				discriminatorSQLString = DiscriminatorHelper.getDiscriminatorSQLValue( persistentClass, dialect );
			}
			else {
				explicitDiscriminatorColumnName = null;
				discriminatorAlias = IMPLICIT_DISCRIMINATOR_ALIAS;
				discriminatorType = basicTypeRegistry.resolve( StandardBasicTypes.INTEGER );
				try {
					discriminatorValue = persistentClass.getSubclassId();
					discriminatorSQLString = discriminatorValue.toString();
				}
				catch ( Exception e ) {
					throw new MappingException( "Could not format discriminator value to SQL string", e );
				}
			}
		}
		else {
			explicitDiscriminatorColumnName = null;
			discriminatorAlias = IMPLICIT_DISCRIMINATOR_ALIAS;
			discriminatorType = basicTypeRegistry.resolve( StandardBasicTypes.INTEGER );
			discriminatorValue = null;
			discriminatorSQLString = null;
			forceDiscriminator = false;
		}

		if ( optimisticLockStyle().isAllOrDirty() ) {
			throw new MappingException( "optimistic-lock=all|dirty not supported for joined-subclass mappings [" + getEntityName() + "]" );
		}

		//MULTITABLES

		final int idColumnSpan = getIdentifierColumnSpan();

		final ArrayList<String> tableNames = new ArrayList<>();
		final ArrayList<String[]> keyColumns = new ArrayList<>();
		final ArrayList<String[]> keyColumnReaders = new ArrayList<>();
		final ArrayList<String[]> keyColumnReaderTemplates = new ArrayList<>();
		final ArrayList<Boolean> cascadeDeletes = new ArrayList<>();
		final List<Table> tableClosure = persistentClass.getTableClosure();
		final List<KeyValue> keyClosure = persistentClass.getKeyClosure();
		for ( int i = 0; i < tableClosure.size() && i < keyClosure.size(); i++ ) {
			tableNames.add( determineTableName( tableClosure.get(i) ) );

			final KeyValue key = keyClosure.get(i);
			final String[] keyCols = new String[idColumnSpan];
			final String[] keyColReaders = new String[idColumnSpan];
			final String[] keyColReaderTemplates = new String[idColumnSpan];
			final List<Column> columns = key.getColumns();
			for ( int k = 0; k < idColumnSpan; k++ ) {
				final Column column = columns.get(k);
				keyCols[k] = column.getQuotedName( dialect );
				keyColReaders[k] = column.getReadExpr( dialect );
				keyColReaderTemplates[k] = column.getTemplate( dialect, typeConfiguration, functionRegistry );
			}
			keyColumns.add( keyCols );
			keyColumnReaders.add( keyColReaders );
			keyColumnReaderTemplates.add( keyColReaderTemplates );
			cascadeDeletes.add( key.isCascadeDeleteEnabled() && dialect.supportsCascadeDelete() );
		}

		//Span of the tableNames directly mapped by this entity and super-classes, if any
		coreTableSpan = tableNames.size();
		subclassCoreTableSpan = persistentClass.getSubclassTableClosure().size();
		tableSpan = persistentClass.getJoinClosureSpan() + coreTableSpan;

		isNullableTable = new boolean[tableSpan];
		isInverseTable = new boolean[tableSpan];

		final List<Join> joinClosure = persistentClass.getJoinClosure();
		for ( int i = 0; i < joinClosure.size(); i++ ) {
			final Join join = joinClosure.get(i);
			isNullableTable[i] = join.isOptional();
			isInverseTable[i] = join.isInverse();

			tableNames.add( determineTableName( join.getTable() ) );

			final KeyValue key = join.getKey();
			final int joinIdColumnSpan = key.getColumnSpan();

			final String[] keyCols = new String[joinIdColumnSpan];
			final String[] keyColReaders = new String[joinIdColumnSpan];
			final String[] keyColReaderTemplates = new String[joinIdColumnSpan];

			final List<Column> columns = key.getColumns();
			for ( int k = 0; k < joinIdColumnSpan; k++ ) {
				final Column column = columns.get(k);
				keyCols[k] = column.getQuotedName( dialect );
				keyColReaders[k] = column.getReadExpr( dialect );
				keyColReaderTemplates[k] = column.getTemplate( dialect, typeConfiguration, functionRegistry );
			}
			keyColumns.add( keyCols );
			keyColumnReaders.add( keyColReaders );
			keyColumnReaderTemplates.add( keyColReaderTemplates );
			cascadeDeletes.add( key.isCascadeDeleteEnabled() && dialect.supportsCascadeDelete() );
		}

		hasDuplicateTables = new HashSet<>( tableNames ).size() == tableNames.size();
		naturalOrderTableNames = toStringArray( tableNames );
		naturalOrderTableKeyColumns = to2DStringArray( keyColumns );
		final String[][] naturalOrderTableKeyColumnReaders = to2DStringArray( keyColumnReaders );
		final String[][] naturalOrderTableKeyColumnReaderTemplates = to2DStringArray( keyColumnReaderTemplates );
		naturalOrderCascadeDeleteEnabled = ArrayHelper.toBooleanArray( cascadeDeletes );

		final ArrayList<String> subclassTableNames = new ArrayList<>();
		final ArrayList<Boolean> isConcretes = new ArrayList<>();
//		final ArrayList<Boolean> isDeferreds = new ArrayList<>();
//		final ArrayList<Boolean> isLazies = new ArrayList<>();
		final ArrayList<Boolean> isInverses = new ArrayList<>();
		final ArrayList<Boolean> isNullables = new ArrayList<>();

		final ArrayList<String[]> allKeyColumns = new ArrayList<>();
		for ( Table table : persistentClass.getSubclassTableClosure() ) {
			isConcretes.add( persistentClass.isClassOrSuperclassTable( table ) );
//			isDeferreds.add( false );
//			isLazies.add( false );
			isInverses.add( false );
			isNullables.add( false );
			final String tableName = determineTableName( table );
			subclassTableNames.add( tableName );
			final String[] key = new String[idColumnSpan];
			final List<Column> columns = table.getPrimaryKey().getColumnsInOriginalOrder();
			for ( int k = 0; k < idColumnSpan; k++ ) {
				key[k] = columns.get(k).getQuotedName( dialect );
			}
			allKeyColumns.add( key );
		}

		//Add joins
		for ( Join join : persistentClass.getSubclassJoinClosure() ) {
			final Table joinTable = join.getTable();
			isConcretes.add( persistentClass.isClassOrSuperclassTable( joinTable ) );
//			isDeferreds.add( join.isSequentialSelect() );
			isInverses.add( join.isInverse() );
			isNullables.add( join.isOptional() );
//			isLazies.add( join.isLazy() );
			final String joinTableName = determineTableName( joinTable );
			subclassTableNames.add( joinTableName );
			final String[] key = new String[idColumnSpan];
			final List<Column> columns = joinTable.getPrimaryKey().getColumnsInOriginalOrder();
			for ( int k = 0; k < idColumnSpan; k++ ) {
				key[k] = columns.get(k).getQuotedName( dialect );
			}
			allKeyColumns.add( key );
		}

		final String[] naturalOrderSubclassTableNameClosure = toStringArray( subclassTableNames );
		final String[][] naturalOrderSubclassTableKeyColumnClosure = to2DStringArray( allKeyColumns );
		isClassOrSuperclassTable = ArrayHelper.toBooleanArray( isConcretes );
//		subclassTableSequentialSelect = ArrayHelper.toBooleanArray( isDeferreds );
//		subclassTableIsLazyClosure = ArrayHelper.toBooleanArray( isLazies );
		isInverseSubclassTable = ArrayHelper.toBooleanArray( isInverses );
		isNullableSubclassTable = ArrayHelper.toBooleanArray( isNullables );

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

		this.tableNames = reverse( naturalOrderTableNames, coreTableSpan );
		tableKeyColumns = reverse( naturalOrderTableKeyColumns, coreTableSpan );
		tableKeyColumnReaders = reverse( naturalOrderTableKeyColumnReaders, coreTableSpan );
		tableKeyColumnReaderTemplates = reverse( naturalOrderTableKeyColumnReaderTemplates, coreTableSpan );
		subclassTableNameClosure = reverse( naturalOrderSubclassTableNameClosure, coreTableSpan );
		subclassTableKeyColumnClosure = reverse( naturalOrderSubclassTableKeyColumnClosure, coreTableSpan );

		spaces = ArrayHelper.join( this.tableNames, toStringArray( persistentClass.getSynchronizedTables() ) );

		// Custom sql
		customSQLInsert = new String[tableSpan];
		customSQLUpdate = new String[tableSpan];
		customSQLDelete = new String[tableSpan];
		insertCallable = new boolean[tableSpan];
		updateCallable = new boolean[tableSpan];
		deleteCallable = new boolean[tableSpan];

		insertExpectations = new Expectation[tableSpan];
		updateExpectations = new Expectation[tableSpan];
		deleteExpectations = new Expectation[tableSpan];

		PersistentClass currentClass = persistentClass;
		int jk = coreTableSpan - 1;
		while ( currentClass != null ) {
			isNullableTable[jk] = false;
			isInverseTable[jk] = false;

			customSQLInsert[jk] = currentClass.getCustomSQLInsert();
			insertCallable[jk] = currentClass.isCustomInsertCallable();
			insertExpectations[jk] = createExpectation( currentClass.getInsertExpectation(), insertCallable[jk] );

			customSQLUpdate[jk] = currentClass.getCustomSQLUpdate();
			updateCallable[jk] = currentClass.isCustomUpdateCallable();
			updateExpectations[jk] = createExpectation( currentClass.getUpdateExpectation(), updateCallable[jk] );

			customSQLDelete[jk] = currentClass.getCustomSQLDelete();
			deleteCallable[jk] = currentClass.isCustomDeleteCallable();
			deleteExpectations[jk] = createExpectation( currentClass.getDeleteExpectation(), deleteCallable[jk] );

			jk--;
			currentClass = currentClass.getSuperclass();
		}

		if ( jk != -1 ) {
			throw new AssertionFailure( "Tablespan does not match height of joined-subclass hierarchy." );
		}

		int j = coreTableSpan;
		for ( Join join : persistentClass.getJoinClosure() ) {
			isInverseTable[j] = join.isInverse();
			isNullableTable[j] = join.isOptional();

			customSQLInsert[j] = join.getCustomSQLInsert();
			insertCallable[j] = join.isCustomInsertCallable();
			insertExpectations[j] = createExpectation( join.getInsertExpectation(), insertCallable[j] );

			customSQLUpdate[j] = join.getCustomSQLUpdate();
			updateCallable[j] = join.isCustomUpdateCallable();
			updateExpectations[j] = createExpectation( join.getUpdateExpectation(), updateCallable[j] );

			customSQLDelete[j] = join.getCustomSQLDelete();
			deleteCallable[j] = join.isCustomDeleteCallable();
			deleteExpectations[j] = createExpectation( join.getDeleteExpectation(), deleteCallable[j] );

			j++;
		}

		// PROPERTIES
		final int hydrateSpan = getPropertySpan();
		naturalOrderPropertyTableNumbers = new int[hydrateSpan];
//		propertyTableNumbers = new int[hydrateSpan];
		final List<Property> propertyClosure = persistentClass.getPropertyClosure();
		for ( int i = 0; i < propertyClosure.size(); i++ ) {
			final String tableName =
					propertyClosure.get(i).getValue().getTable()
							.getQualifiedName( creationContext.getSqlStringGenerationContext() );
//			propertyTableNumbers[i] = getTableId( tableName, this.tableNames );
			naturalOrderPropertyTableNumbers[i] = getTableId( tableName, naturalOrderTableNames );
		}

		// subclass closure properties

		//TODO: code duplication with SingleTableEntityPersister

		final ArrayList<Integer> columnTableNumbers = new ArrayList<>();
//		final ArrayList<Integer> formulaTableNumbers = new ArrayList<>();
		final ArrayList<Integer> propTableNumbers = new ArrayList<>();
		final ArrayList<String> columns = new ArrayList<>();

		for ( Property property : persistentClass.getSubclassPropertyClosure() ) {
			final String tableName = property.getValue().getTable().
					getQualifiedName( creationContext.getSqlStringGenerationContext() );
			final Integer tableNumber = getTableId( tableName, subclassTableNameClosure );
			final Integer naturalTableNumber = getTableId( tableName, naturalOrderSubclassTableNameClosure );
			propTableNumbers.add( tableNumber );

			for ( Selectable selectable : property.getSelectables() ) {
				if ( !selectable.isFormula() ) {
					columnTableNumbers.add( naturalTableNumber );
					Column column = (Column) selectable;
					columns.add( column.getQuotedName( dialect ) );
				}
//				else {
//					formulaTableNumbers.add( tableNumber );
//				}
			}
		}

		subclassColumnNaturalOrderTableNumberClosure = toIntArray( columnTableNumbers );
		subclassPropertyTableNumberClosure = toIntArray( propTableNumbers );
//		subclassFormulaTableNumberClosure = ArrayHelper.toIntArray( formulaTableNumbers );
		subclassColumnClosure = toStringArray( columns );

		// SUBCLASSES

		final int subclassSpan = persistentClass.getSubclassSpan() + 1;
//		subclassClosure = new String[subclassSpan];
		final int subclassSpanMinusOne = subclassSpan - 1;
//		subclassClosure[subclassSpanMinusOne] = getEntityName();
		if ( !persistentClass.isPolymorphic() ) {
			subclassNameByTableName = emptyMap();
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
			subclassNameByTableName = mapOfSize( subclassSpan + 1 );

			final Table table = persistentClass.getTable();
			discriminatorValues = new String[subclassSpan];
			discriminatorAbstract = new boolean[subclassSpan];
			initDiscriminatorProperties( dialect, subclassSpanMinusOne, table, discriminatorValue, isAbstract( persistentClass) );

			notNullColumnTableNumbers = new int[subclassSpan];
			final int id = getTableId(
					table.getQualifiedName( creationContext.getSqlStringGenerationContext() ),
					subclassTableNameClosure
			);
			notNullColumnTableNumbers[subclassSpanMinusOne] = id;
			notNullColumnNames = new String[subclassSpan];
			notNullColumnNames[subclassSpanMinusOne] = subclassTableKeyColumnClosure[id][0];

			final List<Subclass> subclasses = persistentClass.getSubclasses();
			for ( int k = 0; k < subclasses.size(); k++ ) {
				final Subclass subclass = subclasses.get(k);
//				subclassClosure[k] = subclass.getEntityName();
				final Table subclassTable = subclass.getTable();
				subclassNameByTableName.put( subclassTable.getName(), subclass.getEntityName() );
				if ( persistentClass.isPolymorphic() ) {
					final Object discriminatorValue = explicitDiscriminatorColumnName != null
							? DiscriminatorHelper.getDiscriminatorValue( subclass )
							// we now use subclass ids that are consistent across all
							// persisters for a class hierarchy, so that the use of
							// "foo.class = Bar" works in HQL
							: subclass.getSubclassId();
					initDiscriminatorProperties( dialect, k, subclassTable, discriminatorValue, isAbstract( subclass ) );
					subclassesByDiscriminatorValue.put( discriminatorValue, subclass.getEntityName() );
					final int tableId = getTableId(
							subclassTable.getQualifiedName( creationContext.getSqlStringGenerationContext() ),
							subclassTableNameClosure
					);
					notNullColumnTableNumbers[k] = tableId;
					notNullColumnNames[k] = subclassTableKeyColumnClosure[tableId][0];
				}
			}
		}

		subclassNamesBySubclassTable = buildSubclassNamesBySubclassTableMapping(
				persistentClass,
				creationContext.getSqlStringGenerationContext()
		);

		initSubclassPropertyAliasesMap( persistentClass );

		postConstruct( creationContext.getMetadata() );

	}

	private void initDiscriminatorProperties(Dialect dialect, int k, Table table, Object discriminatorValue, boolean isAbstract) {
		final String tableName = determineTableName( table );
		final String columnName = table.getPrimaryKey().getColumn( 0 ).getQuotedName( dialect );
		discriminatorValuesByTableName.put( tableName, discriminatorValue );
		discriminatorColumnNameByTableName.put( tableName, columnName );
		discriminatorValues[k] = discriminatorValue.toString();
		discriminatorAbstract[k] = isAbstract;
	}

	@Override
	public Map<Object, String> getSubclassByDiscriminatorValue() {
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
	private String[][] buildSubclassNamesBySubclassTableMapping(
			PersistentClass persistentClass,
			SqlStringGenerationContext context) {
		// this value represents the number of subclasses (and not the class itself)
		final int numberOfSubclassTables = subclassTableNameClosure.length - coreTableSpan;
		if ( numberOfSubclassTables == 0 ) {
			return new String[0][];
		}

		final String[][] mapping = new String[numberOfSubclassTables][];
		processPersistentClassHierarchy( persistentClass, true, mapping, context );
		return mapping;
	}

	private Set<String> processPersistentClassHierarchy(
			PersistentClass persistentClass,
			boolean isBase,
			String[][] mapping,
			SqlStringGenerationContext context) {

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// collect all the class names that indicate that the "main table" of the given PersistentClass should be
		// included when one of the collected class names is used in TREAT
		final Set<String> classNames = new HashSet<>();

		for ( Subclass subclass : persistentClass.getDirectSubclasses() ) {
			final Set<String> subclassSubclassNames =
					processPersistentClassHierarchy( subclass, false, mapping, context );
			classNames.addAll( subclassSubclassNames );
		}

		classNames.add( persistentClass.getEntityName() );

		if ( !isBase ) {
			MappedSuperclass msc = persistentClass.getSuperMappedSuperclass();
			while ( msc != null ) {
				classNames.add( msc.getMappedClass().getName() );
				msc = msc.getSuperMappedSuperclass();
			}

			associateSubclassNamesToSubclassTableIndexes( persistentClass, classNames, mapping, context );
		}

		return classNames;
	}

	private void associateSubclassNamesToSubclassTableIndexes(
			PersistentClass persistentClass,
			Set<String> classNames,
			String[][] mapping,
			SqlStringGenerationContext context) {

		final String tableName = persistentClass.getTable().getQualifiedName( context );
		associateSubclassNamesToSubclassTableIndex( tableName, classNames, mapping );

		for ( Join join : persistentClass.getJoins() ) {
			final String secondaryTableName = join.getTable().getQualifiedName( context );
			associateSubclassNamesToSubclassTableIndex( secondaryTableName, classNames, mapping );
		}
	}

	private void associateSubclassNamesToSubclassTableIndex(
			String tableName,
			Set<String> classNames,
			String[][] mapping) {
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
	protected void visitMutabilityOrderedTables(MutabilityOrderedTableConsumer consumer) {
		for ( int i = 0; i < naturalOrderTableNames.length; i++ ) {
			final String tableName = naturalOrderTableNames[i];
			final int tableIndex = i;
			consumer.consume(
					tableName,
					tableIndex,
					() -> (columnConsumer) -> columnConsumer.accept(
							tableName,
							getIdentifierMapping(),
							naturalOrderTableKeyColumns[tableIndex]
					)
			);
		}
	}

	@Override
	protected boolean isIdentifierTable(String tableExpression) {
		return tableExpression.equals( getRootTableName() );
	}

	@Override
	public boolean hasSkippableTables() {
		// todo (6.x) : cache this?
		return hasAnySkippableTables( isNullableTable, isInverseTable );
	}

	@Override
	public boolean isInverseTable(int j) {
		return isInverseTable[j];
	}

	@Override
	public String getSubclassPropertyTableName(int i) {
		return subclassTableNameClosure[subclassPropertyTableNumberClosure[i]];
	}

	@Override
	protected boolean isInverseSubclassTable(int j) {
		return isInverseSubclassTable[j];
	}

	@Override
	protected boolean isNullableSubclassTable(int j) {
		return isNullableSubclassTable[j];
	}

	@Override
	public BasicType<?> getDiscriminatorType() {
		return discriminatorType;
	}

	@Override
	public Object getDiscriminatorValue() {
		return discriminatorValue;
	}

	@Override
	public String getDiscriminatorSQLValue() {
		return discriminatorSQLString;
	}

	@Override
	public String getDiscriminatorColumnName() {
		return explicitDiscriminatorColumnName == null
				? super.getDiscriminatorColumnName()
				: explicitDiscriminatorColumnName;
	}

	@Override
	public String getDiscriminatorColumnReaders() {
		return getDiscriminatorColumnName();
	}

	@Override
	public String getDiscriminatorColumnReaderTemplate() {
		return getDiscriminatorColumnName();
	}

	@Override
	public String getDiscriminatorAlias() {
		return discriminatorAlias;
	}

	@Override
	public String getSubclassForDiscriminatorValue(Object value) {
		if ( value == null ) {
			return subclassesByDiscriminatorValue.get( NULL_DISCRIMINATOR );
		}
		else {
			final String result = subclassesByDiscriminatorValue.get( value );
			return result == null ? subclassesByDiscriminatorValue.get( NOT_NULL_DISCRIMINATOR ) : result;
		}
	}

	@Override
	public void addDiscriminatorToInsertGroup(MutationGroupBuilder insertGroupBuilder) {
		if ( explicitDiscriminatorColumnName != null ) {
			final TableInsertBuilder tableInsertBuilder = insertGroupBuilder.getTableDetailsBuilder( getRootTableName() );
			final String discriminatorValueToUse;
			if ( discriminatorValue == NULL_DISCRIMINATOR ) {
				discriminatorValueToUse = "null";
			}
			else if ( discriminatorValue == NOT_NULL_DISCRIMINATOR ) {
				discriminatorValueToUse = "not null";
			}
			else {
				discriminatorValueToUse = discriminatorSQLString;
			}
			tableInsertBuilder.addValueColumn(
					explicitDiscriminatorColumnName,
					discriminatorValueToUse,
					getDiscriminatorMapping().getJdbcMapping()
			);
		}
	}

	@Override
	public String[] getPropertySpaces() {
		return spaces; // don't need subclass tables, because they can't appear in conditions
	}

	@Override
	public boolean hasDuplicateTables() {
		return hasDuplicateTables;
	}

	@Override
	public String getTableName(int j) {
		return naturalOrderTableNames[j];
	}

	@Override
	public String[] getKeyColumns(int j) {
		return naturalOrderTableKeyColumns[j];
	}

	@Override
	public boolean isTableCascadeDeleteEnabled(int j) {
		return naturalOrderCascadeDeleteEnabled[j];
	}

	@Override
	public boolean isPropertyOfTable(int property, int j) {
		return naturalOrderPropertyTableNumbers[property] == j;
	}

	/**
	 * Reverse the first n elements of the incoming array
	 *
	 * @return New array with the first n elements in reversed order
	 */
	private static String[] reverse(String[] objects, int n) {

		int size = objects.length;
		String[] temp = new String[size];

		for ( int i = 0; i < n; i++ ) {
			temp[i] = objects[n - i - 1];
		}

		for ( int i = n; i < size; i++ ) {
			temp[i] = objects[i];
		}

		return temp;
	}

	/**
	 * Reverse the first n elements of the incoming array
	 *
	 * @return New array with the first n elements in reversed order
	 */
	private static String[][] reverse(String[][] objects, int n) {
		int size = objects.length;
		String[][] temp = new String[size][];
		for ( int i = 0; i < n; i++ ) {
			temp[i] = objects[n - i - 1];
		}

		for ( int i = n; i < size; i++ ) {
			temp[i] = objects[i];
		}

		return temp;
	}

	@Override
	public String fromTableFragment(String alias) {
		return getTableName() + ' ' + alias;
	}

	@Override
	public String getTableName() {
		return tableNames[0];
	}

	@Override
	public String generateFilterConditionAlias(String rootAlias) {
		return generateTableAlias( rootAlias, tableSpan - 1 );
	}

	@Override
	public String[] getIdentifierColumnNames() {
		return tableKeyColumns[0];
	}

	@Override
	public String[] getIdentifierColumnReaderTemplates() {
		return tableKeyColumnReaderTemplates[0];
	}

	@Override
	public String getRootTableName() {
		return naturalOrderTableNames[0];
	}

	@Override
	public String[] getIdentifierColumnReaders() {
		return tableKeyColumnReaders[0];
	}

	@Override
	protected int getSubclassPropertyTableNumber(int i) {
		return subclassPropertyTableNumberClosure[i];
	}

	@Override
	public int getTableSpan() {
		return tableSpan;
	}

	@Override
	protected boolean hasMultipleTables() {
		return true;
	}

	@Override
	protected int[] getPropertyTableNumbers() {
		return naturalOrderPropertyTableNumbers;
	}

	@Override
	protected String[] getSubclassTableKeyColumns(int j) {
		return subclassTableKeyColumnClosure[j];
	}

	@Override
	public String getSubclassTableName(int j) {
		return subclassTableNameClosure[j];
	}

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
			Set<String> treatAsDeclarations) {
		if ( treatAsDeclarations == null || treatAsDeclarations.isEmpty() ) {
			return false;
		}

		final String[] inclusionSubclassNameClosure = getSubclassNameClosureBySubclassTable( subclassTableNumber );

		// NOTE : we assume the entire hierarchy is joined-subclass here
		for ( String subclassName : treatAsDeclarations ) {
			for ( String inclusionSubclassName : inclusionSubclassNameClosure ) {
				if ( inclusionSubclassName.equals( subclassName ) ) {
					return true;
				}
			}
		}

		return false;
	}

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

	@Override
	public String[] getConstraintOrderedTableNameClosure() {
		return constraintOrderedTableNames;
	}

	@Override
	public String[][] getContraintOrderedTableKeyColumnClosure() {
		return constraintOrderedKeyColumnNames;
	}

	@Override
	public String getRootTableAlias(String drivingAlias) {
		return generateTableAlias( drivingAlias, getTableId( getRootTableName(), tableNames ) );
	}

	@Override
	public Declarer getSubclassPropertyDeclarer(String propertyPath) {
		if ( "class".equals( propertyPath ) ) {
			// special case where we need to force include all subclass joins
			return Declarer.SUBCLASS;
		}
		return super.getSubclassPropertyDeclarer( propertyPath );
	}

	@Override
	public int determineTableNumberForColumn(String columnName) {
		// HHH-7630: In case the naturalOrder/identifier column is explicitly given in the ordering, check here.
		for ( int i = 0, max = naturalOrderTableKeyColumns.length; i < max; i++ ) {
			final String[] keyColumns = naturalOrderTableKeyColumns[i];
			if ( ArrayHelper.contains( keyColumns, columnName ) ) {
				return naturalOrderPropertyTableNumbers[i];
			}
		}

		for (int i = 0, max = subclassColumnClosure.length; i < max; i++ ) {
			final boolean quoted = subclassColumnClosure[i].startsWith( "\"" )
					&& subclassColumnClosure[i].endsWith( "\"" );
			if ( quoted ) {
				if ( subclassColumnClosure[i].equals( columnName ) ) {
					return subclassColumnNaturalOrderTableNumberClosure[i];
				}
			}
			else {
				if ( subclassColumnClosure[i].equalsIgnoreCase( columnName ) ) {
					return subclassColumnNaturalOrderTableNumberClosure[i];
				}
			}
		}
		throw new HibernateException(
				"Could not locate table which owns column [" + columnName + "] referenced in order-by mapping - " + getEntityName()
		);
	}

	@Override
	public Object forceVersionIncrement(Object id, Object currentVersion, SharedSessionContractImplementor session) {
		if ( getSuperMappingType() != null ) {
			return getSuperMappingType().getEntityPersister().forceVersionIncrement( id, currentVersion, session );
		}
		return super.forceVersionIncrement( id, currentVersion, session );
	}

	@Override
	public Object forceVersionIncrement(
			Object id,
			Object currentVersion,
			boolean batching,
			SharedSessionContractImplementor session) throws HibernateException {
		if ( getSuperMappingType() != null ) {
			return getSuperMappingType().getEntityPersister().forceVersionIncrement( id, currentVersion, session );
		}
		return super.forceVersionIncrement( id, currentVersion, batching, session );
	}

	@Override
	protected EntityVersionMapping generateVersionMapping(
			Supplier<?> templateInstanceCreator,
			PersistentClass bootEntityDescriptor,
			MappingModelCreationProcess creationProcess) {
		if ( getVersionType() == null ) {
			return null;
		}
		else {
			if ( getTableName().equals( getVersionedTableName() ) ) {
				final int versionPropertyIndex = getVersionProperty();
				final String versionPropertyName = getPropertyNames()[versionPropertyIndex];
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

	@Override
	protected EntityIdentifierMapping generateIdentifierMapping(
			Supplier<?> templateInstanceCreator,
			PersistentClass bootEntityDescriptor,
			MappingModelCreationProcess creationProcess) {
		final Type idType = getIdentifierType();

		if ( idType instanceof CompositeType ) {
			final CompositeType cidType = (CompositeType) idType;

			// NOTE: the term `isEmbedded` here uses Hibernate's older (pre-JPA) naming for its "non-aggregated"
			// composite-id support.  It unfortunately conflicts with the JPA usage of "embedded".  Here we normalize
			// the legacy naming to the more descriptive encapsulated versus non-encapsulated phrasing

			final boolean encapsulated = ! cidType.isEmbedded();
			if ( encapsulated ) {
				// we have an `@EmbeddedId`
				return buildEncapsulatedCompositeIdentifierMapping(
						this,
						bootEntityDescriptor.getIdentifierProperty(),
						bootEntityDescriptor.getIdentifierProperty().getName(),
						getTableName(),
						tableKeyColumns[0],
						cidType,
						creationProcess
				);
			}

			// otherwise we have a non-encapsulated composite-identifier
			return generateNonEncapsulatedCompositeIdentifierMapping( creationProcess, bootEntityDescriptor );
		}

		final String columnDefinition;
		final Long length;
		final Integer precision;
		final Integer scale;
		if ( bootEntityDescriptor.getIdentifier() == null ) {
			columnDefinition = null;
			length = null;
			precision = null;
			scale = null;
		}
		else {
			final Column column = bootEntityDescriptor.getIdentifier().getColumns().get( 0 );
			columnDefinition = column.getSqlType();
			length = column.getLength();
			precision = column.getPrecision();
			scale = column.getScale();
		}
		final Value value = bootEntityDescriptor.getIdentifierProperty().getValue();
		return new BasicEntityIdentifierMappingImpl(
				this,
				templateInstanceCreator,
				bootEntityDescriptor.getIdentifierProperty().getName(),
				getTableName(),
				tableKeyColumns[0][0],
				columnDefinition,
				length,
				precision,
				scale,
				value.isColumnInsertable( 0 ),
				value.isColumnUpdateable( 0 ),
				(BasicType<?>) idType,
				creationProcess
		);
	}

	@Override
	protected boolean isPhysicalDiscriminator() {
		return explicitDiscriminatorColumnName != null;
	}

	@Override
	protected EntityDiscriminatorMapping generateDiscriminatorMapping(PersistentClass bootEntityDescriptor) {
		final EntityMappingType superMappingType = getSuperMappingType();
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
						notNullColumnTableNumbers,
						notNullColumnNames,
						discriminatorValues,
						discriminatorAbstract,
						resolveDiscriminatorType()
				);
			}
		}
		else {
			return null;
		}
	}

	@Override
	protected EntityIdentifierMapping generateNonEncapsulatedCompositeIdentifierMapping(
			MappingModelCreationProcess creationProcess,
			PersistentClass bootEntityDescriptor) {
		assert declaredAttributeMappings != null;

		return buildNonEncapsulatedCompositeIdentifierMapping(
				this,
				getTableName(),
				tableKeyColumns[0],
				bootEntityDescriptor,
				creationProcess
		);
	}

	@Override
	public FilterAliasGenerator getFilterAliasGenerator(String rootAlias) {
		return new DynamicFilterAliasGenerator(subclassTableNameClosure, rootAlias);
	}

	@Override
	public TableDetails getMappedTableDetails() {
		// Subtract the number of secondary tables (tableSpan - coreTableSpan) and get the last table mapping
		return getTableMapping( getTableMappings().length - ( tableSpan - coreTableSpan ) - 1 );
	}

	@Override
	public TableDetails getIdentifierTableDetails() {
		final EntityMappingType superMappingType = getSuperMappingType();
		return superMappingType == null
				? getMappedTableDetails()
				: getRootEntityDescriptor().getIdentifierTableDetails();
	}

	@Override
	public void pruneForSubclasses(TableGroup tableGroup, Map<String, EntityNameUse> entityNameUses) {
		final Set<TableReference> retainedTableReferences = new HashSet<>( entityNameUses.size() );
		final MappingMetamodelImplementor metamodel = getFactory().getRuntimeMetamodels().getMappingMetamodel();
		// We can only do this optimization if the table group reports canUseInnerJoins or isRealTableGroup,
		// because the switch for table reference joins to INNER must be cardinality preserving.
		// If canUseInnerJoins is true, this is trivially given, but also if the table group is real
		// i.e. with parenthesis around, as that means the table reference joins will be isolated
		final boolean innerJoinOptimization = tableGroup.canUseInnerJoins() || tableGroup.isRealTableGroup();
		final Set<String> tablesToInnerJoin = innerJoinOptimization ? new HashSet<>() : null;
		boolean needsTreatDiscriminator = false;
		for ( Map.Entry<String, EntityNameUse> entry : entityNameUses.entrySet() ) {
			final EntityNameUse.UseKind useKind = entry.getValue().getKind();
			final JoinedSubclassEntityPersister persister =
					(JoinedSubclassEntityPersister) metamodel.findEntityDescriptor( entry.getKey() );
			// The following block tries to figure out what can be inner joined and which super class table joins can be omitted
			if ( innerJoinOptimization && ( useKind == EntityNameUse.UseKind.TREAT || useKind == EntityNameUse.UseKind.FILTER ) ) {
				final String[] subclassTableNames = persister.getSubclassTableNames();
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
							final TableReference mainTableReference = tableGroup.getTableReference(
									null,
									subclassTableName,
									false
							);
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
			final String tableName = persister.getTableName();
			final TableReference mainTableReference = tableGroup.getTableReference(
					null,
					tableName,
					false
			);
			if ( mainTableReference != null ) {
				retainedTableReferences.add( mainTableReference );
			}
			final String sqlWhereStringTableExpression = persister.getSqlWhereStringTableExpression();
			if ( sqlWhereStringTableExpression != null ) {
				final TableReference tableReference = tableGroup.getTableReference( sqlWhereStringTableExpression );
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
			final String[] subclassTableNames = getSubclassTableNames();
			for ( int i = 0; i < subclassTableNames.length; i++ ) {
				if ( isClassOrSuperclassTable[i] ) {
					tablesToInnerJoin.add( subclassTableNames[i] );
				}
			}
		}

		final List<TableReferenceJoin> tableReferenceJoins = tableGroup.getTableReferenceJoins();
		if ( needsTreatDiscriminator ) {
			if ( tableReferenceJoins.isEmpty() ) {
				// We need to apply the discriminator predicate to the primary table reference itself
				final String discriminatorPredicate = getPrunedDiscriminatorPredicate( entityNameUses, metamodel, "t" );
				if ( discriminatorPredicate != null ) {
					final NamedTableReference tableReference = (NamedTableReference) tableGroup.getPrimaryTableReference();
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
					final TableReferenceJoin join = tableReferenceJoins.get( i );
					applied = applyDiscriminatorPredicate( join, join.getJoinedTableReference(), entityNameUses, metamodel );
				}
				assert applied : "Could not apply treat discriminator predicate to root table join";
				if ( i != 0 ) {
					// Always retain the root table reference join where the discriminator was applied
					retainedTableReferences.add( tableReferenceJoins.get( i - 1 ).getJoinedTableReference() );
				}
			}
		}
		if ( tableReferenceJoins.isEmpty() ) {
			return;
		}
		// The optimization is to remove all table reference joins that are not contained in the retainedTableReferences
		// In addition, we switch from a possible LEFT join, to an INNER join for all tablesToInnerJoin
		if ( innerJoinOptimization ) {
			final TableReferenceJoin[] oldJoins = tableReferenceJoins.toArray( new TableReferenceJoin[0] );
			tableReferenceJoins.clear();
			for ( TableReferenceJoin oldJoin : oldJoins ) {
				final NamedTableReference joinedTableReference = oldJoin.getJoinedTableReference();
				if ( retainedTableReferences.contains( joinedTableReference ) ) {
					final TableReferenceJoin join = oldJoin.getJoinType() != SqlAstJoinType.INNER
								&& tablesToInnerJoin.contains( joinedTableReference.getTableExpression() )
							? new TableReferenceJoin( true, joinedTableReference, oldJoin.getPredicate() )
							: oldJoin;
					tableReferenceJoins.add( join );
				}
				else {
					for ( int i = subclassCoreTableSpan; i < subclassTableNameClosure.length; i++ ) {
						if ( joinedTableReference.getTableExpression().equals( subclassTableNameClosure[i] ) ) {
							// Retain joins to secondary tables
							tableReferenceJoins.add( oldJoin );
							break;
						}
					}
				}
			}
		}
		else {
			tableReferenceJoins.removeIf( join -> !retainedTableReferences.contains( join.getJoinedTableReference() ) );
		}
	}

	@Override
	public EntityIdentifierMapping getIdentifierMappingForJoin() {
		// If the joined subclass has a physical discriminator and has subtypes
		// we must use the root table identifier mapping for joining to allow table group elimination to work
		return isPhysicalDiscriminator() && !getSubMappingTypes().isEmpty()
				? getRootEntityDescriptor().getIdentifierMapping()
				: super.getIdentifierMappingForJoin();
	}

	private boolean applyDiscriminatorPredicate(
			TableReferenceJoin join,
			NamedTableReference tableReference,
			Map<String, EntityNameUse> entityNameUses,
			MappingMetamodelImplementor metamodel) {
		if ( tableReference.getTableExpression().equals( getRootTableName() ) ) {
			assert join.getJoinType() == SqlAstJoinType.INNER : "Found table reference join with root table of non-INNER type: " + join.getJoinType();
			final String discriminatorPredicate = getPrunedDiscriminatorPredicate(
					entityNameUses,
					metamodel,
					"t"
//					tableReference.getIdentificationVariable()
			);
			tableReference.setPrunedTableExpression( "(select * from " + getRootTableName() + " t where " + discriminatorPredicate + ")" );
//			join.applyPredicate( new SqlFragmentPredicate( discriminatorPredicate ) );
			return true;
		}
		return false;
	}

	@Override
	public void visitConstraintOrderedTables(ConstraintOrderedTableConsumer consumer) {
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
