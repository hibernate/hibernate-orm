/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.NaturalIdDataAccess;
import org.hibernate.engine.spi.ExecuteUpdateResultCheckStyle;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.DynamicFilterAliasGenerator;
import org.hibernate.internal.FilterAliasGenerator;
import org.hibernate.internal.util.MarkerObject;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
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
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.internal.BasicEntityIdentifierMappingImpl;
import org.hibernate.metamodel.mapping.internal.CaseStatementDiscriminatorMappingImpl;
import org.hibernate.metamodel.mapping.internal.MappingModelCreationHelper;
import org.hibernate.metamodel.mapping.internal.MappingModelCreationProcess;
import org.hibernate.persister.spi.PersisterCreationContext;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.InFragment;
import org.hibernate.sql.Insert;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.tree.expression.CaseSearchedExpression;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.QueryLiteral;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.from.TableReferenceJoin;
import org.hibernate.sql.ast.tree.predicate.NullnessPredicate;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.entity.internal.EntityResultJoinedSubclassImpl;
import org.hibernate.type.BasicType;
import org.hibernate.type.CompositeType;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.java.JavaType;

import org.jboss.logging.Logger;

import static java.util.Collections.emptyMap;

/**
 * An <tt>EntityPersister</tt> implementing the normalized "table-per-subclass"
 * mapping strategy
 *
 * @author Gavin King
 */
public class JoinedSubclassEntityPersister extends AbstractEntityPersister {
	private static final Logger log = Logger.getLogger( JoinedSubclassEntityPersister.class );

	private static final String IMPLICIT_DISCRIMINATOR_ALIAS = "clazz_";
	private static final Object NULL_DISCRIMINATOR = new MarkerObject("<null discriminator>");
	private static final Object NOT_NULL_DISCRIMINATOR = new MarkerObject("<not null discriminator>");

	// the class hierarchy structure
	private final int tableSpan;
	private final String[] tableNames;
	private final String[] naturalOrderTableNames;
	private final String[][] tableKeyColumns;
	private final String[][] tableKeyColumnReaders;
	private final String[][] tableKeyColumnReaderTemplates;
	private final String[][] naturalOrderTableKeyColumns;
	private final String[][] naturalOrderTableKeyColumnReaders;
	private final String[][] naturalOrderTableKeyColumnReaderTemplates;
	private final boolean[] naturalOrderCascadeDeleteEnabled;

	private final String[] spaces;

	private final String[] subclassClosure;

	private final String[] subclassTableNameClosure;
	private final String[][] subclassTableKeyColumnClosure;
	private final boolean[] isClassOrSuperclassTable;

	// properties of this class, including inherited properties
	private final int[] naturalOrderPropertyTableNumbers;
	private final int[] propertyTableNumbers;

	// the closure of all properties in the entire hierarchy including
	// subclasses and superclasses of this class
	private final int[] subclassPropertyTableNumberClosure;

	// the closure of all columns used by the entire hierarchy including
	// subclasses and superclasses of this class
	private final int[] subclassColumnTableNumberClosure;
	private final int[] subclassFormulaTableNumberClosure;

	private final boolean[] subclassTableSequentialSelect;
	private final boolean[] subclassTableIsLazyClosure;
	private final boolean[] isInverseSubclassTable;
	private final boolean[] isNullableSubclassTable;

	// subclass discrimination works by assigning particular
	// values to certain combinations of not-null primary key
	// values in the outer join using an SQL CASE
	private final Map<Object,String> subclassesByDiscriminatorValue = new HashMap<>();
	private final String[] discriminatorValues;
	private final String[] notNullColumnNames;
	private final int[] notNullColumnTableNumbers;

	private final String[] constraintOrderedTableNames;
	private final String[][] constraintOrderedKeyColumnNames;

	private final Object discriminatorValue;
	private final String discriminatorSQLString;
	private final BasicType<?> discriminatorType;
	private final String explicitDiscriminatorColumnName;
	private final String discriminatorAlias;

	// Span of the tables directly mapped by this entity and super-classes, if any
	private final int coreTableSpan;
	// only contains values for SecondaryTables, ie. not tables part of the "coreTableSpan"
	private final boolean[] isNullableTable;
	private final boolean[] isInverseTable;

	private final Map<String, Object> discriminatorValuesByTableName;
	private final Map<String, String> discriminatorColumnNameByTableName;
	private final Map<String, String> subclassNameByTableName;

	//INITIALIZATION:

	public JoinedSubclassEntityPersister(
			final PersistentClass persistentClass,
			final EntityDataAccess cacheAccessStrategy,
			final NaturalIdDataAccess naturalIdRegionAccessStrategy,
			final PersisterCreationContext creationContext) throws HibernateException {

		super( persistentClass, cacheAccessStrategy, naturalIdRegionAccessStrategy, creationContext );

		final SessionFactoryImplementor factory = creationContext.getSessionFactory();
		final Database database = creationContext.getMetadata().getDatabase();

		// DISCRIMINATOR

		if ( persistentClass.isPolymorphic() ) {
			final Value discriminatorMapping = persistentClass.getDiscriminator();
			if ( discriminatorMapping != null ) {
				log.debug( "Encountered explicit discriminator mapping for joined inheritance" );

				final Selectable selectable = discriminatorMapping.getColumnIterator().next();
				if ( selectable instanceof Formula ) {
					throw new MappingException( "Discriminator formulas on joined inheritance hierarchies not supported at this time" );
				}
				else {
					final Column column = (Column) selectable;
					explicitDiscriminatorColumnName = column.getQuotedName( factory.getDialect() );
					discriminatorAlias = column.getAlias( factory.getDialect(), persistentClass.getRootTable() );
				}
				discriminatorType = (BasicType<?>) persistentClass.getDiscriminator().getType();
				if ( persistentClass.isDiscriminatorValueNull() ) {
					discriminatorValue = NULL_DISCRIMINATOR;
					discriminatorSQLString = InFragment.NULL;
				}
				else if ( persistentClass.isDiscriminatorValueNotNull() ) {
					discriminatorValue = NOT_NULL_DISCRIMINATOR;
					discriminatorSQLString = InFragment.NOT_NULL;
				}
				else {
					try {
						discriminatorValue = discriminatorType.getJavaTypeDescriptor().fromString( persistentClass.getDiscriminatorValue() );
						discriminatorSQLString = discriminatorType.getJdbcTypeDescriptor().getJdbcLiteralFormatter( (JavaType) discriminatorType.getJavaTypeDescriptor() )
								.toJdbcLiteral(
										discriminatorValue,
										factory.getJdbcServices().getDialect(),
										factory.getWrapperOptions()
								);
					}
					catch (ClassCastException cce) {
						throw new MappingException("Illegal discriminator type: " + discriminatorType.getName() );
					}
					catch (Exception e) {
						throw new MappingException("Could not format discriminator value to SQL string", e);
					}
				}
			}
			else {
				explicitDiscriminatorColumnName = null;
				discriminatorAlias = IMPLICIT_DISCRIMINATOR_ALIAS;
				discriminatorType = factory.getTypeConfiguration()
						.getBasicTypeRegistry()
						.resolve( StandardBasicTypes.INTEGER );
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
			discriminatorType = factory.getTypeConfiguration()
					.getBasicTypeRegistry()
					.resolve( StandardBasicTypes.INTEGER );
			discriminatorValue = null;
			discriminatorSQLString = null;
		}

		if ( optimisticLockStyle().isAllOrDirty() ) {
			throw new MappingException( "optimistic-lock=all|dirty not supported for joined-subclass mappings [" + getEntityName() + "]" );
		}

		//MULTITABLES

		final int idColumnSpan = getIdentifierColumnSpan();

		ArrayList<String> tableNames = new ArrayList<>();
		ArrayList<String[]> keyColumns = new ArrayList<>();
		ArrayList<String[]> keyColumnReaders = new ArrayList<>();
		ArrayList<String[]> keyColumnReaderTemplates = new ArrayList<>();
		ArrayList<Boolean> cascadeDeletes = new ArrayList<>();
		Iterator<Table> tItr = persistentClass.getTableClosureIterator();
		Iterator<KeyValue> kItr = persistentClass.getKeyClosureIterator();
		while ( tItr.hasNext() ) {
			final Table table = tItr.next();
			final KeyValue key = kItr.next();
			final String tableName = determineTableName( table );
			tableNames.add( tableName );
			String[] keyCols = new String[idColumnSpan];
			String[] keyColReaders = new String[idColumnSpan];
			String[] keyColReaderTemplates = new String[idColumnSpan];
			Iterator<Selectable> cItr = key.getColumnIterator();
			for ( int k = 0; k < idColumnSpan; k++ ) {
				Column column = (Column) cItr.next();
				keyCols[k] = column.getQuotedName( factory.getJdbcServices().getDialect() );
				keyColReaders[k] = column.getReadExpr( factory.getJdbcServices().getDialect() );
				keyColReaderTemplates[k] = column.getTemplate( factory.getJdbcServices().getDialect(), factory.getQueryEngine().getSqmFunctionRegistry() );
			}
			keyColumns.add( keyCols );
			keyColumnReaders.add( keyColReaders );
			keyColumnReaderTemplates.add( keyColReaderTemplates );
			cascadeDeletes.add( key.isCascadeDeleteEnabled() && factory.getJdbcServices().getDialect().supportsCascadeDelete() );
		}

		//Span of the tableNames directly mapped by this entity and super-classes, if any
		coreTableSpan = tableNames.size();
		tableSpan = persistentClass.getJoinClosureSpan() + coreTableSpan;

		isNullableTable = new boolean[tableSpan];
		isInverseTable = new boolean[tableSpan];

		Iterator<Join> joinItr = persistentClass.getJoinClosureIterator();
		for ( int tableIndex = 0; joinItr.hasNext(); tableIndex++ ) {
			Join join = joinItr.next();

			isNullableTable[tableIndex] = join.isOptional();
			isInverseTable[tableIndex] = join.isInverse();

			Table table = join.getTable();
			final String tableName = determineTableName( table );
			tableNames.add( tableName );

			KeyValue key = join.getKey();
			int joinIdColumnSpan = key.getColumnSpan();

			String[] keyCols = new String[joinIdColumnSpan];
			String[] keyColReaders = new String[joinIdColumnSpan];
			String[] keyColReaderTemplates = new String[joinIdColumnSpan];

			Iterator<Selectable> cItr = key.getColumnIterator();
			for ( int k = 0; k < joinIdColumnSpan; k++ ) {
				Column column = (Column) cItr.next();
				keyCols[k] = column.getQuotedName( factory.getJdbcServices().getDialect() );
				keyColReaders[k] = column.getReadExpr( factory.getJdbcServices().getDialect() );
				keyColReaderTemplates[k] = column.getTemplate( factory.getJdbcServices().getDialect(), factory.getQueryEngine().getSqmFunctionRegistry() );
			}
			keyColumns.add( keyCols );
			keyColumnReaders.add( keyColReaders );
			keyColumnReaderTemplates.add( keyColReaderTemplates );
			cascadeDeletes.add( key.isCascadeDeleteEnabled() && factory.getJdbcServices().getDialect().supportsCascadeDelete() );
		}

		naturalOrderTableNames = ArrayHelper.toStringArray( tableNames );
		naturalOrderTableKeyColumns = ArrayHelper.to2DStringArray( keyColumns );
		naturalOrderTableKeyColumnReaders = ArrayHelper.to2DStringArray( keyColumnReaders );
		naturalOrderTableKeyColumnReaderTemplates = ArrayHelper.to2DStringArray( keyColumnReaderTemplates );
		naturalOrderCascadeDeleteEnabled = ArrayHelper.toBooleanArray( cascadeDeletes );

		ArrayList<String> subclassTableNames = new ArrayList<>();
		ArrayList<Boolean> isConcretes = new ArrayList<>();
		ArrayList<Boolean> isDeferreds = new ArrayList<>();
		ArrayList<Boolean> isLazies = new ArrayList<>();
		ArrayList<Boolean> isInverses = new ArrayList<>();
		ArrayList<Boolean> isNullables = new ArrayList<>();

		keyColumns = new ArrayList<>();
		tItr = persistentClass.getSubclassTableClosureIterator();
		while ( tItr.hasNext() ) {
			Table tab = tItr.next();
			isConcretes.add( persistentClass.isClassOrSuperclassTable( tab ) );
			isDeferreds.add( Boolean.FALSE );
			isLazies.add( Boolean.FALSE );
			isInverses.add( Boolean.FALSE );
			isNullables.add( Boolean.FALSE );
			final String tableName = determineTableName( tab );
			subclassTableNames.add( tableName );
			String[] key = new String[idColumnSpan];
			Iterator<Column> cItr = tab.getPrimaryKey().getColumnIterator();
			for ( int k = 0; k < idColumnSpan; k++ ) {
				key[k] = cItr.next().getQuotedName( factory.getJdbcServices().getDialect() );
			}
			keyColumns.add( key );
		}

		//Add joins
		joinItr = persistentClass.getSubclassJoinClosureIterator();
		while ( joinItr.hasNext() ) {
			final Join join = joinItr.next();
			final Table joinTable = join.getTable();

			isConcretes.add( persistentClass.isClassOrSuperclassTable( joinTable ) );
			isDeferreds.add( join.isSequentialSelect() );
			isInverses.add( join.isInverse() );
			isNullables.add( join.isOptional() );
			isLazies.add( join.isLazy() );

			String joinTableName = determineTableName( joinTable );
			subclassTableNames.add( joinTableName );
			String[] key = new String[idColumnSpan];
			Iterator<Column> citer = joinTable.getPrimaryKey().getColumnIterator();
			for ( int k = 0; k < idColumnSpan; k++ ) {
				key[k] = citer.next().getQuotedName( factory.getJdbcServices().getDialect() );
			}
			keyColumns.add( key );
		}

		String[] naturalOrderSubclassTableNameClosure = ArrayHelper.toStringArray( subclassTableNames );
		String[][] naturalOrderSubclassTableKeyColumnClosure = ArrayHelper.to2DStringArray( keyColumns );
		isClassOrSuperclassTable = ArrayHelper.toBooleanArray( isConcretes );
		subclassTableSequentialSelect = ArrayHelper.toBooleanArray( isDeferreds );
		subclassTableIsLazyClosure = ArrayHelper.toBooleanArray( isLazies );
		isInverseSubclassTable = ArrayHelper.toBooleanArray( isInverses );
		isNullableSubclassTable = ArrayHelper.toBooleanArray( isNullables );

		constraintOrderedTableNames = new String[naturalOrderSubclassTableNameClosure.length];
		constraintOrderedKeyColumnNames = new String[naturalOrderSubclassTableNameClosure.length][];
		int currentPosition = 0;
		for ( int i = naturalOrderSubclassTableNameClosure.length - 1; i >= 0; i--, currentPosition++ ) {
			constraintOrderedTableNames[currentPosition] = naturalOrderSubclassTableNameClosure[i];
			constraintOrderedKeyColumnNames[currentPosition] = naturalOrderSubclassTableKeyColumnClosure[i];
		}

		/*
		 * Suppose an entity Client extends Person, mapped to the tableNames CLIENT and PERSON respectively.
		 * For the Client entity:
		 * naturalOrderTableNames -> PERSON, CLIENT; this reflects the sequence in which the tableNames are
		 * added to the meta-data when the annotated entities are processed.
		 * However, in some instances, for example when generating joins, the CLIENT table needs to be
		 * the first table as it will the driving table.
		 * tableNames -> CLIENT, PERSON
		 */

		this.tableNames = reverse( naturalOrderTableNames, coreTableSpan );
		tableKeyColumns = reverse( naturalOrderTableKeyColumns, coreTableSpan );
		tableKeyColumnReaders = reverse( naturalOrderTableKeyColumnReaders, coreTableSpan );
		tableKeyColumnReaderTemplates = reverse( naturalOrderTableKeyColumnReaderTemplates, coreTableSpan );
		subclassTableNameClosure = reverse( naturalOrderSubclassTableNameClosure, coreTableSpan );
		subclassTableKeyColumnClosure = reverse( naturalOrderSubclassTableKeyColumnClosure, coreTableSpan );

		spaces = ArrayHelper.join(
				this.tableNames,
				ArrayHelper.toStringArray( persistentClass.getSynchronizedTables() )
		);

		// Custom sql
		customSQLInsert = new String[tableSpan];
		customSQLUpdate = new String[tableSpan];
		customSQLDelete = new String[tableSpan];
		insertCallable = new boolean[tableSpan];
		updateCallable = new boolean[tableSpan];
		deleteCallable = new boolean[tableSpan];
		insertResultCheckStyles = new ExecuteUpdateResultCheckStyle[tableSpan];
		updateResultCheckStyles = new ExecuteUpdateResultCheckStyle[tableSpan];
		deleteResultCheckStyles = new ExecuteUpdateResultCheckStyle[tableSpan];

		PersistentClass pc = persistentClass;
		int jk = coreTableSpan - 1;
		while ( pc != null ) {
			isNullableTable[jk] = false;
			isInverseTable[jk] = false;
			customSQLInsert[jk] = pc.getCustomSQLInsert();
			insertCallable[jk] = customSQLInsert[jk] != null && pc.isCustomInsertCallable();
			insertResultCheckStyles[jk] = pc.getCustomSQLInsertCheckStyle() == null
					? ExecuteUpdateResultCheckStyle.determineDefault(
					customSQLInsert[jk], insertCallable[jk]
			)
					: pc.getCustomSQLInsertCheckStyle();
			customSQLUpdate[jk] = pc.getCustomSQLUpdate();
			updateCallable[jk] = customSQLUpdate[jk] != null && pc.isCustomUpdateCallable();
			updateResultCheckStyles[jk] = pc.getCustomSQLUpdateCheckStyle() == null
					? ExecuteUpdateResultCheckStyle.determineDefault( customSQLUpdate[jk], updateCallable[jk] )
					: pc.getCustomSQLUpdateCheckStyle();
			customSQLDelete[jk] = pc.getCustomSQLDelete();
			deleteCallable[jk] = customSQLDelete[jk] != null && pc.isCustomDeleteCallable();
			deleteResultCheckStyles[jk] = pc.getCustomSQLDeleteCheckStyle() == null
					? ExecuteUpdateResultCheckStyle.determineDefault( customSQLDelete[jk], deleteCallable[jk] )
					: pc.getCustomSQLDeleteCheckStyle();
			jk--;
			pc = pc.getSuperclass();
		}

		if ( jk != -1 ) {
			throw new AssertionFailure( "Tablespan does not match height of joined-subclass hiearchy." );
		}

		joinItr = persistentClass.getJoinClosureIterator();
		int j = coreTableSpan;
		while ( joinItr.hasNext() ) {
			Join join = joinItr.next();

			isInverseTable[j] = join.isInverse();
			isNullableTable[j] = join.isOptional();

			customSQLInsert[j] = join.getCustomSQLInsert();
			insertCallable[j] = customSQLInsert[j] != null && join.isCustomInsertCallable();
			insertResultCheckStyles[j] = join.getCustomSQLInsertCheckStyle() == null
					? ExecuteUpdateResultCheckStyle.determineDefault( customSQLInsert[j], insertCallable[j] )
					: join.getCustomSQLInsertCheckStyle();
			customSQLUpdate[j] = join.getCustomSQLUpdate();
			updateCallable[j] = customSQLUpdate[j] != null && join.isCustomUpdateCallable();
			updateResultCheckStyles[j] = join.getCustomSQLUpdateCheckStyle() == null
					? ExecuteUpdateResultCheckStyle.determineDefault( customSQLUpdate[j], updateCallable[j] )
					: join.getCustomSQLUpdateCheckStyle();
			customSQLDelete[j] = join.getCustomSQLDelete();
			deleteCallable[j] = customSQLDelete[j] != null && join.isCustomDeleteCallable();
			deleteResultCheckStyles[j] = join.getCustomSQLDeleteCheckStyle() == null
					? ExecuteUpdateResultCheckStyle.determineDefault( customSQLDelete[j], deleteCallable[j] )
					: join.getCustomSQLDeleteCheckStyle();
			j++;
		}

		// PROPERTIES
		int hydrateSpan = getPropertySpan();
		naturalOrderPropertyTableNumbers = new int[hydrateSpan];
		propertyTableNumbers = new int[hydrateSpan];
		Iterator<Property> iter = persistentClass.getPropertyClosureIterator();
		int i = 0;
		while ( iter.hasNext() ) {
			Property prop = iter.next();
			String tabname = prop.getValue().getTable().getQualifiedName(
					factory.getSqlStringGenerationContext()
			);
			propertyTableNumbers[i] = getTableId( tabname, this.tableNames );
			naturalOrderPropertyTableNumbers[i] = getTableId( tabname, naturalOrderTableNames );
			i++;
		}

		// subclass closure properties

		//TODO: code duplication with SingleTableEntityPersister

		ArrayList<Integer> columnTableNumbers = new ArrayList<>();
		ArrayList<Integer> formulaTableNumbers = new ArrayList<>();
		ArrayList<Integer> propTableNumbers = new ArrayList<>();

		iter = persistentClass.getSubclassPropertyClosureIterator();
		while ( iter.hasNext() ) {
			Property prop = iter.next();
			Table tab = prop.getValue().getTable();
			String tabname = tab.getQualifiedName(
					factory.getSqlStringGenerationContext()
			);
			Integer tabnum = getTableId( tabname, subclassTableNameClosure );
			propTableNumbers.add( tabnum );

			Iterator<Selectable> citer = prop.getColumnIterator();
			while ( citer.hasNext() ) {
				Selectable thing = citer.next();
				if ( thing.isFormula() ) {
					formulaTableNumbers.add( tabnum );
				}
				else {
					columnTableNumbers.add( tabnum );
				}
			}

		}

		subclassColumnTableNumberClosure = ArrayHelper.toIntArray( columnTableNumbers );
		subclassPropertyTableNumberClosure = ArrayHelper.toIntArray( propTableNumbers );
		subclassFormulaTableNumberClosure = ArrayHelper.toIntArray( formulaTableNumbers );

		// SUBCLASSES

		int subclassSpan = persistentClass.getSubclassSpan() + 1;
		subclassClosure = new String[subclassSpan];
		int subclassSpanMinusOne = subclassSpan - 1;
		subclassClosure[subclassSpanMinusOne] = getEntityName();
		if ( persistentClass.isPolymorphic() ) {
			subclassesByDiscriminatorValue.put( discriminatorValue, getEntityName() );

			discriminatorValuesByTableName = CollectionHelper.linkedMapOfSize( subclassSpan + 1 );
			discriminatorColumnNameByTableName = CollectionHelper.linkedMapOfSize( subclassSpan + 1 );
			subclassNameByTableName = CollectionHelper.mapOfSize( subclassSpan + 1 );
			// We need to convert the `discriminatorSQLString` (which is a String read from boot-mapping) into
			// 	the type indicated by `#discriminatorType` (String -> Integer, e.g.).
			try {
				Table table = persistentClass.getTable();
				final Object convertedDiscriminatorValue = discriminatorType.getJavaTypeDescriptor().fromString( discriminatorSQLString );
				discriminatorValues = new String[subclassSpan];
				initDiscriminatorProperties( factory, subclassSpanMinusOne, table, convertedDiscriminatorValue
				);

				notNullColumnTableNumbers = new int[subclassSpan];
				final int id = getTableId(
						table.getQualifiedName(
								factory.getSqlStringGenerationContext()
					),
					subclassTableNameClosure
			);
			notNullColumnTableNumbers[subclassSpanMinusOne] = id;
				notNullColumnNames = new String[subclassSpan];
				notNullColumnNames[subclassSpanMinusOne] = subclassTableKeyColumnClosure[id][0]; //( (Column) model.getTable().getPrimaryKey().getColumnIterator().next() ).getName();
			}
			catch (HibernateException e) {
				throw e;
			}
			catch (Exception e) {
				throw new MappingException(
						"Could not resolve specified discriminator value [" + discriminatorSQLString
								+ "] to discriminator type [" + discriminatorType + "]"

				);
			}
		}
		else {
			subclassNameByTableName = emptyMap();
			discriminatorValuesByTableName = emptyMap();
			discriminatorColumnNameByTableName = emptyMap();
			discriminatorValues = null;
			notNullColumnTableNumbers = null;
			notNullColumnNames = null;
		}

		Iterator<Subclass> siter = persistentClass.getSubclassIterator();
		int k = 0;
		while ( siter.hasNext() ) {
			Subclass sc = siter.next();
			subclassClosure[k] = sc.getEntityName();
			final Table table = sc.getTable();
			subclassNameByTableName.put( table.getName(), sc.getEntityName() );
			try {
				if ( persistentClass.isPolymorphic() ) {
					final Object discriminatorValue;
					if ( explicitDiscriminatorColumnName != null ) {
						if ( sc.isDiscriminatorValueNull() ) {
							discriminatorValue = NULL_DISCRIMINATOR;
						}
						else if ( sc.isDiscriminatorValueNotNull() ) {
							discriminatorValue = NOT_NULL_DISCRIMINATOR;
						}
						else {
							try {
								discriminatorValue = discriminatorType.getJavaTypeDescriptor().fromString( sc.getDiscriminatorValue() );
							}
							catch (ClassCastException cce) {
								throw new MappingException( "Illegal discriminator type: " + discriminatorType.getName() );
							}
							catch (Exception e) {
								throw new MappingException( "Could not format discriminator value to SQL string", e);
							}
						}
					}
					else {
						// we now use subclass ids that are consistent across all
						// persisters for a class hierarchy, so that the use of
						// "foo.class = Bar" works in HQL
						discriminatorValue = sc.getSubclassId();
					}
					initDiscriminatorProperties( factory, k, table, discriminatorValue );
					subclassesByDiscriminatorValue.put( discriminatorValue, sc.getEntityName() );
					int id = getTableId(
							table.getQualifiedName(
									factory.getSqlStringGenerationContext()
							),
							subclassTableNameClosure
					);
					notNullColumnTableNumbers[k] = id;
					notNullColumnNames[k] = subclassTableKeyColumnClosure[id][0]; //( (Column) sc.getTable().getPrimaryKey().getColumnIterator().next() ).getName();
				}
			}
			catch ( Exception e ) {
				throw new MappingException( "Error parsing discriminator value", e );
			}
			k++;
		}

		subclassNamesBySubclassTable = buildSubclassNamesBySubclassTableMapping( persistentClass, factory );

		initSubclassPropertyAliasesMap( persistentClass );

		postConstruct( creationContext.getMetadata() );

	}

	private void initDiscriminatorProperties(
			SessionFactoryImplementor factory,
			int k,
			Table table,
			Object discriminatorValue) {
		final String tableName = determineTableName( table );
		final String columnName = table.getPrimaryKey().getColumn( 0 ).getQuotedName( factory.getJdbcServices().getDialect() );
		discriminatorValuesByTableName.put( tableName, discriminatorValue );
		discriminatorColumnNameByTableName.put( tableName, columnName );
		discriminatorValues[k] = discriminatorValue.toString();
	}


	/**
	 * Used to hold the name of subclasses that each "subclass table" is part of.  For example, given a hierarchy like:
	 * {@code JoinedEntity <- JoinedEntitySubclass <- JoinedEntitySubSubclass}..
	 * <p/>
	 * For the persister for JoinedEntity, we'd have:
	 * <pre>
	 *     subclassClosure[0] = "JoinedEntitySubSubclass"
	 *     subclassClosure[1] = "JoinedEntitySubclass"
	 *     subclassClosure[2] = "JoinedEntity"
	 *
	 *     subclassTableNameClosure[0] = "T_JoinedEntity"
	 *     subclassTableNameClosure[1] = "T_JoinedEntitySubclass"
	 *     subclassTableNameClosure[2] = "T_JoinedEntitySubSubclass"
	 *
	 *     subclassNameClosureBySubclassTable[0] = ["JoinedEntitySubSubclass", "JoinedEntitySubclass"]
	 *     subclassNameClosureBySubclassTable[1] = ["JoinedEntitySubSubclass"]
	 * </pre>
	 * Note that there are only 2 entries in subclassNameClosureBySubclassTable.  That is because there are really only
	 * 2 tables here that make up the subclass mapping, the others make up the class/superclass table mappings.  We
	 * do not need to account for those here.  The "offset" is defined by the value of {@link #getTableSpan()}.
	 * Therefore the corresponding row in subclassNameClosureBySubclassTable for a given row in subclassTableNameClosure
	 * is calculated as {@code subclassTableNameClosureIndex - getTableSpan()}.
	 * <p/>
	 * As we consider each subclass table we can look into this array based on the subclass table's index and see
	 * which subclasses would require it to be included.  E.g., given {@code TREAT( x AS JoinedEntitySubSubclass )},
	 * when trying to decide whether to include join to "T_JoinedEntitySubclass" (subclassTableNameClosureIndex = 1),
	 * we'd look at {@code subclassNameClosureBySubclassTable[0]} and see if the TREAT-AS subclass name is included in
	 * its values.  Since {@code subclassNameClosureBySubclassTable[1]} includes "JoinedEntitySubSubclass", we'd
	 * consider it included.
	 * <p/>
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
	private String[][] buildSubclassNamesBySubclassTableMapping(PersistentClass persistentClass, SessionFactoryImplementor factory) {
		// this value represents the number of subclasses (and not the class itself)
		final int numberOfSubclassTables = subclassTableNameClosure.length - coreTableSpan;
		if ( numberOfSubclassTables == 0 ) {
			return new String[0][];
		}

		final String[][] mapping = new String[numberOfSubclassTables][];
		processPersistentClassHierarchy( persistentClass, true, factory, mapping );
		return mapping;
	}

	private Set<String> processPersistentClassHierarchy(
			PersistentClass persistentClass,
			boolean isBase,
			SessionFactoryImplementor factory,
			String[][] mapping) {

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// collect all the class names that indicate that the "main table" of the given PersistentClass should be
		// included when one of the collected class names is used in TREAT
		final Set<String> classNames = new HashSet<>();

		final Iterator<Subclass> itr = persistentClass.getDirectSubclasses();
		while ( itr.hasNext() ) {
			final Subclass subclass = itr.next();
			final Set<String> subclassSubclassNames = processPersistentClassHierarchy(
					subclass,
					false,
					factory,
					mapping
			);
			classNames.addAll( subclassSubclassNames );
		}

		classNames.add( persistentClass.getEntityName() );

		if ( ! isBase ) {
			MappedSuperclass msc = persistentClass.getSuperMappedSuperclass();
			while ( msc != null ) {
				classNames.add( msc.getMappedClass().getName() );
				msc = msc.getSuperMappedSuperclass();
			}

			associateSubclassNamesToSubclassTableIndexes( persistentClass, classNames, mapping, factory );
		}

		return classNames;
	}

	private void associateSubclassNamesToSubclassTableIndexes(
			PersistentClass persistentClass,
			Set<String> classNames,
			String[][] mapping,
			SessionFactoryImplementor factory) {

		final String tableName = persistentClass.getTable().getQualifiedName(
				factory.getSqlStringGenerationContext()
		);

		associateSubclassNamesToSubclassTableIndex( tableName, classNames, mapping );

		Iterator<Join> itr = persistentClass.getJoinIterator();
		while ( itr.hasNext() ) {
			final Join join = itr.next();
			final String secondaryTableName = join.getTable().getQualifiedName(
					factory.getSqlStringGenerationContext()
			);
			associateSubclassNamesToSubclassTableIndex( secondaryTableName, classNames, mapping );
		}
	}

	private void associateSubclassNamesToSubclassTableIndex(
			String tableName,
			Set<String> classNames,
			String[][] mapping) {
		// find the table's entry in the subclassTableNameClosure array
		boolean found = false;
		for ( int i = 0; i < subclassTableNameClosure.length; i++ ) {
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
				mapping[index] = ArrayHelper.toStringArray( classNames );
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
	public boolean isNullableTable(int j) {
		return isNullableTable[j];
	}

	@Override
	public boolean isInverseTable(int j) {
		return isInverseTable[j];
	}

	protected boolean isSubclassTableSequentialSelect(int j) {
		return subclassTableSequentialSelect[j] && !isClassOrSuperclassTable[j];
	}

	/*public void postInstantiate() throws MappingException {
		super.postInstantiate();
		//TODO: other lock modes?
		loader = createEntityLoader(LockMode.NONE, CollectionHelper.EMPTY_MAP);
	}*/

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

	public Type getDiscriminatorType() {
		return discriminatorType;
	}

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

	public String getDiscriminatorAlias() {
		return discriminatorAlias;
	}

	public String getSubclassForDiscriminatorValue(Object value) {
		if ( value == null ) {
			return subclassesByDiscriminatorValue.get( NULL_DISCRIMINATOR );
		}
		else {
			String result = subclassesByDiscriminatorValue.get( value );
			if ( result == null ) {
				result = subclassesByDiscriminatorValue.get( NOT_NULL_DISCRIMINATOR );
			}
			return result;
		}
	}

	@Override
	protected void addDiscriminatorToInsert(Insert insert) {
		if ( explicitDiscriminatorColumnName != null ) {
			insert.addColumn( explicitDiscriminatorColumnName, getDiscriminatorSQLValue() );
		}
	}

	public Serializable[] getPropertySpaces() {
		return spaces; // don't need subclass tables, because they can't appear in conditions
	}


	public String getTableName(int j) {
		return naturalOrderTableNames[j];
	}

	public String[] getKeyColumns(int j) {
		return naturalOrderTableKeyColumns[j];
	}

	public boolean isTableCascadeDeleteEnabled(int j) {
		return naturalOrderCascadeDeleteEnabled[j];
	}

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

	public String fromTableFragment(String alias) {
		return getTableName() + ' ' + alias;
	}

	public String getTableName() {
		return tableNames[0];
	}

	private int getRootHierarchyClassTableIndex() {
		final String rootHierarchyClassTableName = naturalOrderTableNames[0];
		for ( int i = 0; i < subclassTableNameClosure.length; i++ ) {
			if ( subclassTableNameClosure[i].equals( rootHierarchyClassTableName ) ) {
				return i;
			}
		}
		return 0;
	}

	@Override
	protected String filterFragment(String alias) {
		return hasWhere() ? getSQLWhereString( generateFilterConditionAlias( alias ) ) : "";
	}

	@Override
	protected String filterFragment(String alias, Set<String> treatAsDeclarations) {
		return filterFragment( alias );
	}

	public String generateFilterConditionAlias(String rootAlias) {
		return generateTableAlias( rootAlias, tableSpan - 1 );
	}

	public String generateWhereConditionAlias(String rootAlias) {
		return generateTableAlias( rootAlias, tableSpan - 1 );
	}

	public String[] getIdentifierColumnNames() {
		return tableKeyColumns[0];
	}

	public String[] getIdentifierColumnReaderTemplates() {
		return tableKeyColumnReaderTemplates[0];
	}

	@Override
	public String getRootTableName() {
		return  naturalOrderTableNames[0];
	}

	public String[] getIdentifierColumnReaders() {
		return tableKeyColumnReaders[0];
	}

	protected int[] getPropertyTableNumbersInSelect() {
		return propertyTableNumbers;
	}

	protected int getSubclassPropertyTableNumber(int i) {
		return subclassPropertyTableNumberClosure[i];
	}

	public int getTableSpan() {
		return tableSpan;
	}

	public boolean isMultiTable() {
		return true;
	}

	protected int[] getSubclassColumnTableNumberClosure() {
		return subclassColumnTableNumberClosure;
	}

	protected int[] getSubclassFormulaTableNumberClosure() {
		return subclassFormulaTableNumberClosure;
	}

	protected int[] getPropertyTableNumbers() {
		return naturalOrderPropertyTableNumbers;
	}

	protected String[] getSubclassTableKeyColumns(int j) {
		return subclassTableKeyColumnClosure[j];
	}

	public String getSubclassTableName(int j) {
		return subclassTableNameClosure[j];
	}

	@Override
	protected String[] getSubclassTableNames() {
		return subclassTableNameClosure;
	}

	public int getSubclassTableSpan() {
		return subclassTableNameClosure.length;
	}

	protected boolean isSubclassTableLazy(int j) {
		return subclassTableIsLazyClosure[j];
	}

	@Override
	protected boolean shouldProcessSuperMapping() {
		return false;
	}

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
	public String getPropertyTableName(String propertyName) {
		Integer index = getEntityMetamodel().getPropertyIndexOrNull( propertyName );
		if ( index == null ) {
			return null;
		}
		return tableNames[propertyTableNumbers[index]];
	}

	public String[] getConstraintOrderedTableNameClosure() {
		return constraintOrderedTableNames;
	}

	public String[][] getContraintOrderedTableKeyColumnClosure() {
		return constraintOrderedKeyColumnNames;
	}

	public String getRootTableAlias(String drivingAlias) {
		return generateTableAlias( drivingAlias, getTableId( getRootTableName(), tableNames ) );
	}

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
		
		final String[] subclassColumnNameClosure = getSubclassColumnClosure();
		for ( int i = 0, max = subclassColumnNameClosure.length; i < max; i++ ) {
			final boolean quoted = subclassColumnNameClosure[i].startsWith( "\"" )
					&& subclassColumnNameClosure[i].endsWith( "\"" );
			if ( quoted ) {
				if ( subclassColumnNameClosure[i].equals( columnName ) ) {
					return getSubclassColumnTableNumberClosure()[i];
				}
			}
			else {
				if ( subclassColumnNameClosure[i].equalsIgnoreCase( columnName ) ) {
					return getSubclassColumnTableNumberClosure()[i];
				}
			}
		}
		throw new HibernateException( "Could not locate table which owns column [" + columnName + "] referenced in order-by mapping" );
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
				return MappingModelCreationHelper.buildEncapsulatedCompositeIdentifierMapping(
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

		return new BasicEntityIdentifierMappingImpl(
				this,
				templateInstanceCreator,
				bootEntityDescriptor.getIdentifierProperty().getName(),
				getTableName(),
				tableKeyColumns[0][0],
				(BasicType<?>) idType,
				creationProcess
		);
	}

	@Override
	protected boolean isPhysicalDiscriminator() {
		return explicitDiscriminatorColumnName != null;
	}

	@Override
	protected EntityDiscriminatorMapping generateDiscriminatorMapping(MappingModelCreationProcess modelCreationProcess) {
		EntityMappingType superMappingType = getSuperMappingType();
		if ( superMappingType != null ) {
			return superMappingType.getDiscriminatorMapping();
		}

		if ( hasSubclasses() ) {
			final String formula = getDiscriminatorFormulaTemplate();
			if ( explicitDiscriminatorColumnName != null || formula != null ) {
				// even though this is a joined-hierarchy the user has defined an
				// explicit discriminator column - so we can use the normal
				// discriminator mapping
				return super.generateDiscriminatorMapping( modelCreationProcess );
			}

			org.hibernate.persister.entity.DiscriminatorType<?> discriminatorMetadataType = (org.hibernate.persister.entity.DiscriminatorType<?>) getTypeDiscriminatorMetadata().getResolutionType();

			// otherwise, we need to use the case-statement approach
			return new CaseStatementDiscriminatorMappingImpl(
					this,
					subclassTableNameClosure,
					notNullColumnTableNumbers,
					notNullColumnNames,
					discriminatorValues,
					subclassNameByTableName,
					discriminatorMetadataType,
					modelCreationProcess
			);
		}

		return null;
	}

	protected EntityIdentifierMapping generateNonEncapsulatedCompositeIdentifierMapping(
			MappingModelCreationProcess creationProcess,
			PersistentClass bootEntityDescriptor) {
		assert declaredAttributeMappings != null;

		return MappingModelCreationHelper.buildNonEncapsulatedCompositeIdentifierMapping(
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
	public boolean canOmitSuperclassTableJoin() {
		return true;
	}

	@Override
	public <T> DomainResult<T> createDomainResult(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			String resultVariable,
			DomainResultCreationState creationState) {
		if ( hasSubclasses() ) {
			final EntityResultJoinedSubclassImpl entityResultJoinedSubclass = new EntityResultJoinedSubclassImpl(
					navigablePath,
					this,
					tableGroup,
					resultVariable,
					creationState
			);
			entityResultJoinedSubclass.afterInitialize( entityResultJoinedSubclass, creationState );
			//noinspection unchecked
			return entityResultJoinedSubclass;
		}
		else {
			return super.createDomainResult( navigablePath, tableGroup, resultVariable, creationState );
		}
	}

	@Override
	public void pruneForSubclasses(TableGroup tableGroup, Set<String> treatedEntityNames) {
		// If the base type is part of the treatedEntityNames this means we can't optimize this,
		// as the table group is e.g. returned through a select
		if ( treatedEntityNames.contains( getEntityName() ) ) {
			return;
		}
		final Set<TableReference> retainedTableReferences = new HashSet<>( treatedEntityNames.size() );
		final Set<String> sharedSuperclassTables = new HashSet<>();

		for ( String treatedEntityName : treatedEntityNames ) {
			final JoinedSubclassEntityPersister subPersister = (JoinedSubclassEntityPersister) getSubclassMappingType( treatedEntityName );
			final String[] subclassTableNames = subPersister.getSubclassTableNames();
			// For every treated entity name, we collect table names that are needed by all treated entity names
			// In mathematical terms, sharedSuperclassTables will be the "intersection" of the table names of all treated entities
			if ( sharedSuperclassTables.isEmpty() ) {
				for ( int i = 0; i < subclassTableNames.length; i++ ) {
					if ( subPersister.isClassOrSuperclassTable[i] ) {
						sharedSuperclassTables.add( subclassTableNames[i] );
					}
				}
			}
			else {
				sharedSuperclassTables.retainAll( Arrays.asList( subclassTableNames ) );
			}
			// Add the table references for all table names of the treated entities as we have to retain these table references.
			// Table references not appearing in this set can later be pruned away
			// todo (6.0): no need to resolve all table references, only the ones needed for cardinality
			for ( int i = 0; i < subclassTableNames.length; i++ ) {
				retainedTableReferences.add( tableGroup.resolveTableReference( null, subclassTableNames[i], false ) );
			}
		}
		final List<TableReferenceJoin> tableReferenceJoins = tableGroup.getTableReferenceJoins();
		// The optimization is to remove all table reference joins that are not contained in the retainedTableReferences
		// In addition, we switch from a possible LEFT join, to an inner join for all sharedSuperclassTables
		// For now, we can only do this if the table group reports canUseInnerJoins or isRealTableGroup,
		// because the switch for table reference joins to INNER must be cardinality preserving.
		// If canUseInnerJoins is true, this is trivially given, but also if the table group is real
		// i.e. with parenthesis around, as that means the table reference joins will be isolated
		if ( tableGroup.canUseInnerJoins() || tableGroup.isRealTableGroup() ) {
			final TableReferenceJoin[] oldJoins = tableReferenceJoins.toArray( new TableReferenceJoin[0] );
			tableReferenceJoins.clear();
			for ( TableReferenceJoin oldJoin : oldJoins ) {
				final TableReference joinedTableReference = oldJoin.getJoinedTableReference();
				if ( retainedTableReferences.contains( joinedTableReference ) ) {
					if ( oldJoin.getJoinType() != SqlAstJoinType.INNER
							&& sharedSuperclassTables.contains( joinedTableReference.getTableExpression() ) ) {
						tableReferenceJoins.add(
								new TableReferenceJoin(
										SqlAstJoinType.INNER,
										joinedTableReference,
										oldJoin.getPredicate()
								)
						);
					}
					else {
						tableReferenceJoins.add( oldJoin );
					}
				}
			}
		}
		else {
			tableReferenceJoins
					.removeIf( join -> !retainedTableReferences.contains( join.getJoinedTableReference() ) );
		}
	}

	@Override
	public void visitConstraintOrderedTables(ConstraintOrderedTableConsumer consumer) {
		for ( int i = 0; i < constraintOrderedTableNames.length; i++ ) {
			final String tableName = constraintOrderedTableNames[i];
			final int tablePosition = i;

			consumer.consume(
					tableName,
					() -> columnConsumer -> columnConsumer.accept( tableName, constraintOrderedKeyColumnNames[tablePosition] )
			);
		}
	}

	private static class CaseSearchedExpressionInfo{
		CaseSearchedExpression caseSearchedExpression;
		List<ColumnReference> columnReferences = new ArrayList<>(  );
	}

	private CaseSearchedExpressionInfo getCaseSearchedExpression(TableGroup entityTableGroup) {
		CaseSearchedExpressionInfo info = new CaseSearchedExpressionInfo();

		final TableReference primaryTableReference = entityTableGroup.getPrimaryTableReference();
		final BasicType<?> discriminatorType = (BasicType<?>) getDiscriminatorType();
		final CaseSearchedExpression caseSearchedExpression = new CaseSearchedExpression( discriminatorType );

		boolean addPrimaryTableCaseAsLastCaseExpression = false;
		for ( String tableName : discriminatorValuesByTableName.keySet() ) {
			if ( !primaryTableReference.getTableExpression().equals( tableName ) ) {
				TableReference tableReference = entityTableGroup.getTableReference( entityTableGroup.getNavigablePath(), tableName );
				if ( tableReference == null ) {
					// we have not yet created a TableReference for this sub-class table, but we need to because
					// it has a discriminator value associated with it
					tableReference = entityTableGroup.resolveTableReference( entityTableGroup.getNavigablePath(), tableName );
				}

				final ColumnReference identifierColumnReference = getIdentifierColumnReference( tableReference );
				info.columnReferences.add( identifierColumnReference );
				addWhen(
						caseSearchedExpression,
						tableReference,
						identifierColumnReference,
						discriminatorType
				);
			}
			else {
				addPrimaryTableCaseAsLastCaseExpression = true;
			}
		}

		if ( addPrimaryTableCaseAsLastCaseExpression ) {
			addWhen(
					caseSearchedExpression,
					primaryTableReference,
					getIdentifierColumnReference( primaryTableReference ),
					discriminatorType
			);
		}

		info.caseSearchedExpression = caseSearchedExpression;
		return info;
	}

	private void addWhen(
			CaseSearchedExpression caseSearchedExpression,
			TableReference table,
			ColumnReference identifierColumnReference,
			BasicType<?> resultType) {
		final Predicate predicate = new NullnessPredicate( identifierColumnReference, true );
		final Expression expression = new QueryLiteral<>(
				discriminatorValuesByTableName.get( table.getTableExpression() ),
				resultType
		);

		caseSearchedExpression.when( predicate, expression );
	}

	private ColumnReference getIdentifierColumnReference(TableReference tableReference) {
		final List<JdbcMapping> jdbcMappings = getIdentifierMapping().getJdbcMappings();
		return new ColumnReference(
				tableReference.getIdentificationVariable(),
				discriminatorColumnNameByTableName.get( tableReference.getTableExpression() ),
				false,
				null,
				null,
				jdbcMappings.get( 0 ),
				getFactory()
		);
	}

}
