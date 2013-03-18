/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.persister.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.QueryException;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.engine.spi.ExecuteUpdateResultCheckStyle;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.DynamicFilterAliasGenerator;
import org.hibernate.internal.FilterAliasGenerator;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.Subclass;
import org.hibernate.mapping.Table;
import org.hibernate.metamodel.spi.binding.AttributeBinding;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.binding.RelationalValueBinding;
import org.hibernate.metamodel.spi.binding.SecondaryTable;
import org.hibernate.metamodel.spi.binding.SingularAttributeBinding;
import org.hibernate.metamodel.spi.relational.PrimaryKey;
import org.hibernate.metamodel.spi.relational.TableSpecification;
import org.hibernate.sql.CaseFragment;
import org.hibernate.sql.SelectFragment;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;

/**
 * An <tt>EntityPersister</tt> implementing the normalized "table-per-subclass"
 * mapping strategy
 *
 * @author Gavin King
 * @author Strong Liu
 */
public class JoinedSubclassEntityPersister extends AbstractEntityPersister {

	/**
	 * Tables' count, which tables are mapped directly by the current entity ( and its parent entity, recursively, if any ),
	 * including the joined tables, but sub-entities of the current entity are not included.
	 */
	private final int tableSpan;
	/**
	 * Span of the tables directly mapped by this entity and super-classes, if any.
	 * <p/>
	 * so, <code>coreTableSpan = tableSpan - join tables span</code>
	 */
	private final int coreTableSpan;

	/**
	 * Tables' name, the tables' scope is same as {@code tableSpan}.
	 * <p/>
	 *
	 * This array ( the table names ) is order by the following roles:
	 * <ul>
	 *     <li>Table mapped by the root entity of current one if any</li>
	 *     <li>Table mapped by sub entity of the root one, recursively, till the current entity in the hierarchy level</li>
	 *     <li>Joined tables, also, in the top-down order of the hierarchy</li>
	 * </ul>
	 *
	 * <p/>
	 *
	 * Suppose an entity Client extends Person, mapped to the tables CLIENT and PERSON respectively.
	 * For the Client entity:
	 * naturalOrderTableNames -> PERSON, CLIENT; this reflects the sequence in which the tables are
	 * added to the meta-data when the annotated entities are processed.
	 * However, in some instances, for example when generating joins, the CLIENT table needs to be
	 * the first table as it will the driving table.
	 * tableNames -> CLIENT, PERSON
	 *
	 * <p>
	 *     The length of this array is <code>tableSpan</code>
	 * </p>
	 */
	private final String[] naturalOrderTableNames;

	/**
	 * This contains same elements as naturalOrderTableNames, but in different order.
	 * <p/>
	 *
	 * As said above, elements in this array are actually composited by two parts:
	 *
	 * <ul>
	 *     <li>Table names mapped directly by the entities of the hierarchy</li>
	 *     <li>Table names mapped by "join"</li>
	 * </ul>
	 *
	 * In the first part of elements, the naturalOrderTableNames follows "root" -> "sub-entities" -> "current entity"
	 *
	 * here, we have a reversed order, so "current entity" -> "parent entity" -> "root entity"
	 *
	 * <p/>
	 *
	 * The order of the second part is same.
	 *
	 * <p>
	 *     The length of this array is <code>tableSpan</code>
	 * </p>
	 *
	 */
	private final String[] tableNames;

	/**
	 * These two follow same role as above, but for the primary key columns
	 *
	 * <p>
	 *     The first dimension length of this array is <code>tableSpan</code>
	 * </p>
	 */
	private final String[][] naturalOrderTableKeyColumns;
	private final String[][] tableKeyColumns;

	/**
	 * Same as above, just for different column metadata.
 	 */
	private final String[][] tableKeyColumnReaders;
	private final String[][] naturalOrderTableKeyColumnReaders;

	/**
	 * Same as above, just for different column metadata.
	 */
	private final String[][] tableKeyColumnReaderTemplates;
	private final String[][] naturalOrderTableKeyColumnReaderTemplates;

	/**
	 * If the identifier is cascade delete enabled.
	 * Array is ordered by the natural way ( {@see naturalOrderTableNames} )
	 *
	 * <p>
	 *     The length of this array is <code>tableSpan</code>
	 * </p>
	 */
	private final boolean[] naturalOrderCascadeDeleteEnabled;

	/**
	 * <code>tableNames</code>
	 * <code>synchronized tables in the entity hierarchy from top down to the current entity level, sub-entities are not included</code>
	 *
	 * <p>
	 *     The length of this array is <code>tableSpan + sync table count</code>
	 * </p>
	 */
	private final String[] spaces;
	/**
	 * This array contains all the sub-entities' name of the current entity, and
	 * the last element of this array is the current entity name.
	 *
	 * Sub-entities' name is ordered in the most derived subclasses first, from bottom to top, till the current one.
	 *
	 * <p>
	 *     The length of this array is the count of all the sub entities (recursively) of the current one + 1
	 * </p>
	 */
	private final String[] subclassClosure;
	/**
	 * This is kind of same as {@see tableNames}, but it contains all the tables' name mapped by the current entity's super entities AND
	 * sub entities, and joined tables mapped by the entities.
	 *
	 * Actually, due to the order operation, the first <code>coreTableSpan</code> elements are same as <code>tableNames</code>.
	 * (table names mapped by the current entity and its super entities, joined tables are not included)
	 *
	 * So, this property is kind of weird to me, it has:
	 *
	 * 1. "current entity" -> "parent entity" -> "root entity" -> "first sub-entity" -> "second sub-entity" .... "last sub-entity"
	 * 2. "root entity join table" -> ..."last entity join table"
	 *
	 *
	 * <p>
	 *     Though this property is named with "subclassTable" perfix, but its length is actually:
	 *     tableSpan + all sub-entities mapped tables' count + all sub-entities joined tables' count
	 * </p>
	 */
	private final String[] subclassTableNameClosure;
	/**
	 * table's primary key columns, in the same order as above
	 *
	 * <p>
	 *     The length is same as <code>subclassTableNameClosure</code>
	 * </p>
	 */
	private final String[][] subclassTableKeyColumnClosure;

	/**
	 * If the table, in the order of <code>subclassTableNameClosure</code> is concreted.
	 * By "concreted", here means if the table is one of current entity or its super entity mapped table
	 *
	 * <p>
	 *     The length is same as <code>subclassTableNameClosure</code>
	 * </p>
	 */
	private final boolean[] isClassOrSuperclassTable;

	/**
	 * The element in this array is the index of the  {@see naturalOrderTableNames}, which table that the column is belonged to.
	 *
	 * So, this is all about the columns ( except PK ) mapped by the properties in the current entity and its parent entity, in a top down order.
	 *
	 * <p>
	 *     The length is the count of columns, mapped by the properties in the current entity and from its super entity, and joined columns.
	 * </p>
	 */
	private final int[] naturalOrderPropertyTableNumbers;

	/**
	 * Same as above, but here is the index of {@see tableNames}
	 *
	 * <p>
	 *     The length is same as above.
	 * </p>
	 */
	private final int[] propertyTableNumbers;

	/**
	 * the closure of all properties in the entire hierarchy including
	 * subclasses and superclasses of this class
	 *
	 * The element is the index of {@see subclassTableNameClosure}, which table that the property's relational value belongs to.
	 *
	 * <p>
	 *     The length is all the properties count in the entire hierarchy.
	 * </p>
	 *
	 */
	private final int[] subclassPropertyTableNumberClosure;

	// the closure of all columns used by the entire hierarchy including
	// subclasses and superclasses of this class
	private final int[] subclassColumnTableNumberClosure;
	private final int[] subclassFormulaTableNumberClosure;

	private final boolean[] subclassTableSequentialSelect;
	private final boolean[] subclassTableIsLazyClosure;

	// subclass discrimination works by assigning particular
	// values to certain combinations of null primary key
	// values in the outer join using an SQL CASE
	private final Map<Object,String> subclassesByDiscriminatorValue = new HashMap<Object, String>();
	private final String[] discriminatorValues;
	private final String[] notNullColumnNames;
	private final int[] notNullColumnTableNumbers;

	private final String[] constraintOrderedTableNames;
	private final String[][] constraintOrderedKeyColumnNames;

	private final Object discriminatorValue;
	private final String discriminatorSQLString;


	// only contains values for SecondaryTables, ie. not tables part of the "coreTableSpan"
	private final boolean[] isNullableTable;


	private void assertOptimisticLockStyle() {
		if ( optimisticLockStyle() == OptimisticLockStyle.ALL || optimisticLockStyle() == OptimisticLockStyle.DIRTY ) {
			throw new MappingException( "optimistic-lock=all|dirty not supported for joined-subclass mappings [" + getEntityName() + "]" );
		}
	}
	//INITIALIZATION:
	@SuppressWarnings( {"UnusedDeclaration"})
	public JoinedSubclassEntityPersister(
			final PersistentClass persistentClass,
			final EntityRegionAccessStrategy cacheAccessStrategy,
			final NaturalIdRegionAccessStrategy naturalIdRegionAccessStrategy,
			final SessionFactoryImplementor factory,
			final Mapping mapping) throws HibernateException {

		super( persistentClass, cacheAccessStrategy, naturalIdRegionAccessStrategy, factory );

		// DISCRIMINATOR

		if ( persistentClass.isPolymorphic() ) {
			try {
				discriminatorValue = persistentClass.getSubclassId();
				discriminatorSQLString = discriminatorValue.toString();
			}
			catch ( Exception e ) {
				throw new MappingException( "Could not format discriminator value to SQL string", e );
			}
		}
		else {
			discriminatorValue = null;
			discriminatorSQLString = null;
		}

		assertOptimisticLockStyle();

		//MULTITABLES

		final int idColumnSpan = getIdentifierColumnSpan();
		ArrayList tables = new ArrayList();
		ArrayList keyColumns = new ArrayList();
		ArrayList keyColumnReaders = new ArrayList();
		ArrayList keyColumnReaderTemplates = new ArrayList();
		ArrayList cascadeDeletes = new ArrayList();
		Iterator titer = persistentClass.getTableClosureIterator();
		Iterator kiter = persistentClass.getKeyClosureIterator();
		while ( titer.hasNext() ) {
			Table tab = (Table) titer.next();
			KeyValue key = (KeyValue) kiter.next();
			String tabname = tab.getQualifiedName(
					factory.getDialect(),
					factory.getSettings().getDefaultCatalogName(),
					factory.getSettings().getDefaultSchemaName()
			);
			tables.add( tabname );
			String[] keyCols = new String[idColumnSpan];
			String[] keyColReaders = new String[idColumnSpan];
			String[] keyColReaderTemplates = new String[idColumnSpan];
			Iterator citer = key.getColumnIterator();
			for ( int k = 0; k < idColumnSpan; k++ ) {
				Column column = (Column) citer.next();
				keyCols[k] = column.getQuotedName( factory.getDialect() );
				keyColReaders[k] = column.getReadExpr( factory.getDialect() );
				keyColReaderTemplates[k] = column.getTemplate( factory.getDialect(), factory.getSqlFunctionRegistry() );
			}
			keyColumns.add( keyCols );
			keyColumnReaders.add( keyColReaders );
			keyColumnReaderTemplates.add( keyColReaderTemplates );
			cascadeDeletes.add( key.isCascadeDeleteEnabled() && factory.getDialect().supportsCascadeDelete() );
		}

		//Span of the tables directly mapped by this entity and super-classes, if any
		coreTableSpan = tables.size();
		isNullableTable = new boolean[persistentClass.getJoinClosureSpan()];

		int tableIndex = 0;
		Iterator joinIter = persistentClass.getJoinClosureIterator();
		while ( joinIter.hasNext() ) {
			Join join = (Join) joinIter.next();

			isNullableTable[tableIndex++] = join.isOptional();

			Table table = join.getTable();

			String tableName = table.getQualifiedName(
					factory.getDialect(),
					factory.getSettings().getDefaultCatalogName(),
					factory.getSettings().getDefaultSchemaName()
			);
			tables.add( tableName );

			KeyValue key = join.getKey();
			int joinIdColumnSpan = key.getColumnSpan();

			String[] keyCols = new String[joinIdColumnSpan];
			String[] keyColReaders = new String[joinIdColumnSpan];
			String[] keyColReaderTemplates = new String[joinIdColumnSpan];

			Iterator citer = key.getColumnIterator();

			for ( int k = 0; k < joinIdColumnSpan; k++ ) {
				Column column = (Column) citer.next();
				keyCols[k] = column.getQuotedName( factory.getDialect() );
				keyColReaders[k] = column.getReadExpr( factory.getDialect() );
				keyColReaderTemplates[k] = column.getTemplate( factory.getDialect(), factory.getSqlFunctionRegistry() );
			}
			keyColumns.add( keyCols );
			keyColumnReaders.add( keyColReaders );
			keyColumnReaderTemplates.add( keyColReaderTemplates );
			cascadeDeletes.add( key.isCascadeDeleteEnabled() && factory.getDialect().supportsCascadeDelete() );
		}

		naturalOrderTableNames = ArrayHelper.toStringArray( tables );
		naturalOrderTableKeyColumns = ArrayHelper.to2DStringArray( keyColumns );
		naturalOrderTableKeyColumnReaders = ArrayHelper.to2DStringArray( keyColumnReaders );
		naturalOrderTableKeyColumnReaderTemplates = ArrayHelper.to2DStringArray( keyColumnReaderTemplates );
		naturalOrderCascadeDeleteEnabled = ArrayHelper.toBooleanArray( cascadeDeletes );

		ArrayList subtables = new ArrayList();
		ArrayList isConcretes = new ArrayList();
		ArrayList isDeferreds = new ArrayList();
		ArrayList isLazies = new ArrayList();
		keyColumns = new ArrayList();
		titer = persistentClass.getSubclassTableClosureIterator();
		while ( titer.hasNext() ) {
			Table tab = (Table) titer.next();
			isConcretes.add( persistentClass.isClassOrSuperclassTable( tab ) );
			isDeferreds.add( Boolean.FALSE );
			isLazies.add( Boolean.FALSE );
			String tabname = tab.getQualifiedName(
					factory.getDialect(),
					factory.getSettings().getDefaultCatalogName(),
					factory.getSettings().getDefaultSchemaName()
			);
			subtables.add( tabname );
			String[] key = new String[idColumnSpan];
			Iterator citer = tab.getPrimaryKey().getColumnIterator();
			for ( int k = 0; k < idColumnSpan; k++ ) {
				key[k] = ( (Column) citer.next() ).getQuotedName( factory.getDialect() );
			}
			keyColumns.add( key );
		}
		//Add joins
		joinIter = persistentClass.getSubclassJoinClosureIterator();
		while ( joinIter.hasNext() ) {
			Join join = (Join) joinIter.next();

			Table tab = join.getTable();

			isConcretes.add( persistentClass.isClassOrSuperclassTable( tab ) );
			isDeferreds.add( join.isSequentialSelect() );
			isLazies.add( join.isLazy() );

			String tabname = tab.getQualifiedName(
					factory.getDialect(),
					factory.getSettings().getDefaultCatalogName(),
					factory.getSettings().getDefaultSchemaName()
			);
			subtables.add( tabname );
			String[] key = new String[idColumnSpan];
			Iterator citer = tab.getPrimaryKey().getColumnIterator();
			for ( int k = 0; k < idColumnSpan; k++ ) {
				key[k] = ( (Column) citer.next() ).getQuotedName( factory.getDialect() );
			}
			keyColumns.add( key );
		}

		String[] naturalOrderSubclassTableNameClosure = ArrayHelper.toStringArray( subtables );
		String[][] naturalOrderSubclassTableKeyColumnClosure = ArrayHelper.to2DStringArray( keyColumns );
		isClassOrSuperclassTable = ArrayHelper.toBooleanArray( isConcretes );
		subclassTableSequentialSelect = ArrayHelper.toBooleanArray( isDeferreds );
		subclassTableIsLazyClosure = ArrayHelper.toBooleanArray( isLazies );
		constraintOrderedTableNames = new String[naturalOrderSubclassTableNameClosure.length];
		constraintOrderedKeyColumnNames = new String[naturalOrderSubclassTableNameClosure.length][];
		int currentPosition = 0;
		for ( int i = naturalOrderSubclassTableNameClosure.length - 1; i >= 0; i--, currentPosition++ ) {
			constraintOrderedTableNames[currentPosition] = naturalOrderSubclassTableNameClosure[i];
			constraintOrderedKeyColumnNames[currentPosition] = naturalOrderSubclassTableKeyColumnClosure[i];
		}

		/**
		 * Suppose an entity Client extends Person, mapped to the tables CLIENT and PERSON respectively.
		 * For the Client entity:
		 * naturalOrderTableNames -> PERSON, CLIENT; this reflects the sequence in which the tables are
		 * added to the meta-data when the annotated entities are processed.
		 * However, in some instances, for example when generating joins, the CLIENT table needs to be
		 * the first table as it will the driving table.
		 * tableNames -> CLIENT, PERSON
		 */

		tableSpan = naturalOrderTableNames.length;
		tableNames = reverse( naturalOrderTableNames, coreTableSpan );
		tableKeyColumns = reverse( naturalOrderTableKeyColumns, coreTableSpan );
		tableKeyColumnReaders = reverse( naturalOrderTableKeyColumnReaders, coreTableSpan );
		tableKeyColumnReaderTemplates = reverse( naturalOrderTableKeyColumnReaderTemplates, coreTableSpan );
		subclassTableNameClosure = reverse( naturalOrderSubclassTableNameClosure, coreTableSpan );
		subclassTableKeyColumnClosure = reverse( naturalOrderSubclassTableKeyColumnClosure, coreTableSpan );
		spaces = ArrayHelper.join(
				tableNames,
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
		joinIter = persistentClass.getJoinClosureIterator();
		int j = coreTableSpan;
		while ( joinIter.hasNext() ) {
			Join join = (Join) joinIter.next();

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
		Iterator iter = persistentClass.getPropertyClosureIterator();
		int i = 0;
		while ( iter.hasNext() ) {
			Property prop = (Property) iter.next();
			String tabname = prop.getValue().getTable().getQualifiedName(
					factory.getDialect(),
					factory.getSettings().getDefaultCatalogName(),
					factory.getSettings().getDefaultSchemaName()
			);
			propertyTableNumbers[i] = getTableId( tabname, tableNames );
			naturalOrderPropertyTableNumbers[i] = getTableId( tabname, naturalOrderTableNames );
			i++;
		}

		// subclass closure properties

		//TODO: code duplication with SingleTableEntityPersister

		ArrayList columnTableNumbers = new ArrayList();
		ArrayList formulaTableNumbers = new ArrayList();
		ArrayList propTableNumbers = new ArrayList();
		iter = persistentClass.getSubclassPropertyClosureIterator();
		while ( iter.hasNext() ) {
			Property prop = (Property) iter.next();
			Table tab = prop.getValue().getTable();
			String tabname = tab.getQualifiedName(
					factory.getDialect(),
					factory.getSettings().getDefaultCatalogName(),
					factory.getSettings().getDefaultSchemaName()
			);
			Integer tabnum = getTableId( tabname, subclassTableNameClosure );
			propTableNumbers.add( tabnum );

			Iterator citer = prop.getColumnIterator();
			while ( citer.hasNext() ) {
				Selectable thing = (Selectable) citer.next();
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

		if ( persistentClass.isPolymorphic() ) {
			subclassesByDiscriminatorValue.put( discriminatorValue, getEntityName() );
			discriminatorValues = new String[subclassSpan];
			discriminatorValues[subclassSpan - 1] = discriminatorSQLString;
			notNullColumnTableNumbers = new int[subclassSpan];
			final int id = getTableId(
					persistentClass.getTable().getQualifiedName(
							factory.getDialect(),
							factory.getSettings().getDefaultCatalogName(),
							factory.getSettings().getDefaultSchemaName()
					),
					subclassTableNameClosure
			);
			notNullColumnTableNumbers[subclassSpan - 1] = id;
			notNullColumnNames = new String[subclassSpan];
			notNullColumnNames[subclassSpan - 1] = subclassTableKeyColumnClosure[id][0]; //( (Column) model.getTable().getPrimaryKey().getColumnIterator().next() ).getName();
		}
		else {
			discriminatorValues = null;
			notNullColumnTableNumbers = null;
			notNullColumnNames = null;
		}

		subclassClosure = new String[subclassSpan];
		subclassClosure[subclassSpan - 1] = getEntityName();
		iter = persistentClass.getSubclassIterator();
		int k = 0;
		while ( iter.hasNext() ) {
			Subclass sc = (Subclass) iter.next();
			subclassClosure[k] = sc.getEntityName();
			try {
				if ( persistentClass.isPolymorphic() ) {
					// we now use subclass ids that are consistent across all
					// persisters for a class hierarchy, so that the use of
					// "foo.class = Bar" works in HQL
					Integer subclassId = sc.getSubclassId();
					subclassesByDiscriminatorValue.put( subclassId, sc.getEntityName() );
					discriminatorValues[k] = subclassId.toString();
					int id = getTableId(
							sc.getTable().getQualifiedName(
									factory.getDialect(),
									factory.getSettings().getDefaultCatalogName(),
									factory.getSettings().getDefaultSchemaName()
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

		initLockers();

		initSubclassPropertyAliasesMap( persistentClass );

		postConstruct( mapping );

	}


	@SuppressWarnings( {"UnusedDeclaration"})
	public JoinedSubclassEntityPersister(
			final EntityBinding entityBinding,
			final EntityRegionAccessStrategy cacheAccessStrategy,
			final NaturalIdRegionAccessStrategy naturalIdRegionAccessStrategy,
			final SessionFactoryImplementor factory,
			final Mapping mapping) throws HibernateException {

		super( entityBinding, cacheAccessStrategy, naturalIdRegionAccessStrategy, factory );

		assertOptimisticLockStyle();

		final boolean isCascadeDeleteDefault = factory.getDialect().supportsCascadeDelete();

		final EntityBinding[] entityBindings = entityBinding.getEntityBindingClosure();
		final TableSpecification[] tables = entityBinding.getTableClosure();
		final SecondaryTable[] secondaryTables = entityBinding.getSecondaryTableClosure();
		final String[] synchronizedTableNames = entityBinding.getSynchronizedTableNameClosure();
		final AttributeBinding[] attributeBindings = entityBinding.getNonIdAttributeBindingClosure();
		                                                       //todo the count of these two are not equal, which they should be
		final EntityBinding[] preOrderSubEntityBindings = entityBinding.getPreOrderSubEntityBindingClosure();
		final EntityBinding[] postOrderSubEntityBindings = entityBinding.getPostOrderSubEntityBindingClosure();
		final TableSpecification[] subTables = entityBinding.getPreOrderSubTableClosure();
		final SecondaryTable[] subSecondaryTables = entityBinding.getSubEntitySecondaryTables();
		final AttributeBinding[] allAttributeBindings = entityBinding.getNonIdEntitiesAttributeBindingClosure();

		final int idColumnSpan = getIdentifierColumnSpan();
		coreTableSpan = tables.length;
		final int secondaryTableSpan = secondaryTables.length;
		tableSpan = coreTableSpan + secondaryTableSpan;
		final int subclassSpan = postOrderSubEntityBindings.length;
		final int subclassSecondaryTableSpan = subSecondaryTables.length;
		final int subTableSpan = subclassSpan + subclassSecondaryTableSpan;
		final int allTableSpan = tableSpan + subTableSpan;
		final int hydrateSpan = getPropertySpan();
		isClassOrSuperclassTable = new boolean[allTableSpan];
		subclassTableSequentialSelect = new boolean[allTableSpan];
		subclassTableIsLazyClosure = new boolean[allTableSpan];
		naturalOrderTableNames = new String[tableSpan];
		naturalOrderCascadeDeleteEnabled = new boolean[tableSpan];

		naturalOrderTableKeyColumns = new String[tableSpan][];
		naturalOrderTableKeyColumnReaders = new String[tableSpan][];
		naturalOrderTableKeyColumnReaderTemplates = new String[tableSpan][];
		//custom sql
		customSQLInsert = new String[tableSpan];
		customSQLUpdate = new String[tableSpan];
		customSQLDelete = new String[tableSpan];
		insertCallable = new boolean[tableSpan];
		updateCallable = new boolean[tableSpan];
		deleteCallable = new boolean[tableSpan];
		insertResultCheckStyles = new ExecuteUpdateResultCheckStyle[tableSpan];
		updateResultCheckStyles = new ExecuteUpdateResultCheckStyle[tableSpan];
		deleteResultCheckStyles = new ExecuteUpdateResultCheckStyle[tableSpan];
		subclassClosure = new String[subclassSpan+1];
		subclassClosure[subclassSpan] = getEntityName();
		isNullableTable = new boolean[secondaryTableSpan];
		naturalOrderPropertyTableNumbers = new int[hydrateSpan];
		propertyTableNumbers = new int[hydrateSpan];
		constraintOrderedTableNames = new String[allTableSpan];
		constraintOrderedKeyColumnNames = new String[allTableSpan][];
		/**
		 * 1. core table names
		 * 2. direct sub entity table names
		 * 3. core joined table names
		 * 4. direct sub entity joined table names
		 */
		final String[] naturalOrderSubclassTableNameClosure = new String[allTableSpan];
		final String[][] naturalOrderSubclassTableKeyColumnClosure = new String[allTableSpan][];


		int tableIndex = 0;
		int allTableIndex =0;
		//first, process tables / entitybindings mapped directly by the current entitybinding and its super entitybindings
		for ( int i = 0; i < coreTableSpan; i++, tableIndex++, allTableIndex++ ) {
			final TableSpecification table = tables[i];
			final EntityBinding currentEntityBinding = entityBindings[i];
			naturalOrderTableNames[tableIndex] = table.getQualifiedName( factory.getDialect() );
			naturalOrderCascadeDeleteEnabled[tableIndex] = currentEntityBinding.isCascadeDeleteEnabled() && factory.getDialect().supportsCascadeDelete();
			naturalOrderTableKeyColumns[tableIndex] = new String[idColumnSpan];
			naturalOrderTableKeyColumnReaders[tableIndex] = new String[idColumnSpan];
			naturalOrderTableKeyColumnReaderTemplates[tableIndex] = new String[idColumnSpan];
			PrimaryKey primaryKey = table.getPrimaryKey();
			resolvePkColumnNames(
					factory,
					primaryKey,
					naturalOrderTableKeyColumns[tableIndex],
					naturalOrderTableKeyColumnReaders[tableIndex],
					naturalOrderTableKeyColumnReaderTemplates[tableIndex]
			);
			final EntityBinding eb = entityBindings[i];
			//Custom SQL
			initializeCustomSql( eb.getCustomInsert(), tableIndex, customSQLInsert, insertCallable, insertResultCheckStyles );
			initializeCustomSql( eb.getCustomUpdate(), tableIndex, customSQLUpdate, updateCallable, updateResultCheckStyles );
			initializeCustomSql( eb.getCustomDelete(), tableIndex, customSQLDelete, deleteCallable, deleteResultCheckStyles );
			isClassOrSuperclassTable[allTableIndex] = true;//EntityBindingHelper.isClassOrSuperclassTable( entityBinding, table );
			subclassTableSequentialSelect[allTableIndex] = false;
			subclassTableIsLazyClosure[allTableIndex] = false;
		}

		//#1
		System.arraycopy( naturalOrderTableNames, 0, naturalOrderSubclassTableNameClosure, 0, coreTableSpan );
		System.arraycopy( naturalOrderTableKeyColumns, 0, naturalOrderSubclassTableKeyColumnClosure, 0, coreTableSpan );
		//--------------------------------- directly sub entities
		final String[] naturalOrderSubTableNames = new String[subclassSpan];
		final String[][] naturalOrderSubTableKeyColumns = new String[subclassSpan][idColumnSpan];

		for ( int i = 0; i < subclassSpan; i++, allTableIndex++ ) {
			final EntityBinding subEntityBinding = preOrderSubEntityBindings[i]; //todo post order??
			final TableSpecification table = subEntityBinding.getPrimaryTable();
			naturalOrderSubTableNames[i] = table.getQualifiedName( factory.getDialect() );

			final PrimaryKey pk = table.getPrimaryKey();
			for(int j=0;j<idColumnSpan;j++){
				naturalOrderSubTableKeyColumns[i][j] = pk.getColumns().get( j ).getColumnName().getText( factory.getDialect() );
			}
			isClassOrSuperclassTable[allTableIndex] = false;//EntityBindingHelper.isClassOrSuperclassTable( entityBinding, table );
			subclassTableSequentialSelect[allTableIndex] = false;
			subclassTableIsLazyClosure[allTableIndex] = false;
		}

		//#2
		System.arraycopy(
				naturalOrderSubTableNames,
				0,
				naturalOrderSubclassTableNameClosure,
				coreTableSpan,
				subclassSpan
		);
		System.arraycopy( naturalOrderSubTableKeyColumns, 0, naturalOrderSubclassTableKeyColumnClosure, coreTableSpan,
				subclassSpan );



		//--------------------------------- secondary tables


		for ( int i = 0; i < secondaryTableSpan; i++,tableIndex++, allTableIndex++ ) {
			final SecondaryTable secondaryTable = secondaryTables[i];
			final PrimaryKey pk = secondaryTable.getSecondaryTableReference().getPrimaryKey();
			naturalOrderTableNames[tableIndex] = secondaryTable.getSecondaryTableReference()
					.getQualifiedName( factory.getDialect() );
			isNullableTable[i] = secondaryTable.isOptional();
			naturalOrderCascadeDeleteEnabled[tableIndex] = secondaryTable.isCascadeDeleteEnabled() && factory.getDialect().supportsCascadeDelete();

			final int secondaryTablePKColumnSpan = secondaryTable.getSecondaryTableReference()
					.getPrimaryKey()
					.getColumnSpan();
			naturalOrderTableKeyColumns[tableIndex] = new String[secondaryTablePKColumnSpan];
			naturalOrderTableKeyColumnReaders[tableIndex] = new String[secondaryTablePKColumnSpan];
			naturalOrderTableKeyColumnReaderTemplates[tableIndex] = new String[secondaryTablePKColumnSpan];
			resolvePkColumnNames(
					factory,
					pk,
					naturalOrderTableKeyColumns[tableIndex],
					naturalOrderTableKeyColumnReaders[tableIndex],
					naturalOrderTableKeyColumnReaderTemplates[tableIndex]
			);
			//todo custom sql in secondary table binding
			initializeCustomSql(null, tableIndex, customSQLInsert, insertCallable, insertResultCheckStyles);
			initializeCustomSql(null, tableIndex, customSQLUpdate, updateCallable, updateResultCheckStyles);
			initializeCustomSql(null, tableIndex, customSQLDelete, deleteCallable, deleteResultCheckStyles);
			isClassOrSuperclassTable[allTableIndex] = false;//EntityBindingHelper.isClassOrSuperclassTable( entityBinding, table );
			subclassTableSequentialSelect[allTableIndex] = secondaryTable.getFetchStyle() == FetchStyle.SELECT;
			subclassTableIsLazyClosure[allTableIndex] = secondaryTable.isLazy();

		}

		//#3
		System.arraycopy(
				naturalOrderTableNames,
				tableSpan - coreTableSpan,
				naturalOrderSubclassTableNameClosure,
				coreTableSpan + subclassSpan,
				secondaryTableSpan
		);
		System.arraycopy(
				naturalOrderTableKeyColumns,
				tableSpan - coreTableSpan,
				naturalOrderSubclassTableKeyColumnClosure,
				coreTableSpan + subclassSpan,
				secondaryTableSpan
		);

		//--------------------------------- direct sub entity secondary tables
		final String[] naturalOrderSubSecondaryTableNames = new String[subclassSecondaryTableSpan];
		final String[][] naturalOrderSubSecondaryTableKeyColumns = new String[subclassSecondaryTableSpan][];
		for ( int i = 0; i < subclassSecondaryTableSpan; i++, allTableIndex++ ) {
			final SecondaryTable secondaryTable = subSecondaryTables[i];
			naturalOrderSubSecondaryTableNames[i] = secondaryTable.getSecondaryTableReference().getQualifiedName( factory.getDialect() );
			final PrimaryKey pk = secondaryTable.getSecondaryTableReference().getPrimaryKey();
			naturalOrderSubSecondaryTableKeyColumns[i] = new String[pk.getColumnSpan()];
			for(int j =0;j<pk.getColumnSpan();j++){
				naturalOrderSubSecondaryTableKeyColumns[i][j]= pk.getColumns().get( j ).getColumnName().getText( factory.getDialect() );
			}
			isClassOrSuperclassTable[allTableIndex] = false;//EntityBindingHelper.isClassOrSuperclassTable( entityBinding, table );
			subclassTableSequentialSelect[allTableIndex] = secondaryTable.getFetchStyle() == FetchStyle.SELECT;
			subclassTableIsLazyClosure[allTableIndex] = secondaryTable.isLazy();
		}
		//#4
		System.arraycopy(
				naturalOrderSubSecondaryTableNames,
				0,
				naturalOrderSubclassTableNameClosure,
				tableSpan + subclassSpan,
				subclassSecondaryTableSpan
		);
		//#4
		System.arraycopy(
				naturalOrderSubSecondaryTableKeyColumns,
				0,
				naturalOrderSubclassTableKeyColumnClosure,
				tableSpan + subclassSpan,
				subclassSecondaryTableSpan
		);
		//--------------------------------- core and secondary tables

		tableNames = reverse( naturalOrderTableNames, coreTableSpan );
		tableKeyColumns = reverse( naturalOrderTableKeyColumns, coreTableSpan );
		tableKeyColumnReaders = reverse( naturalOrderTableKeyColumnReaders, coreTableSpan );
		tableKeyColumnReaderTemplates = reverse( naturalOrderTableKeyColumnReaderTemplates, coreTableSpan );
		spaces = ArrayHelper.join( tableNames, synchronizedTableNames );
		//--------------------------------- sub entities



		int currentPosition = 0;
		for ( int i = allTableSpan - 1; i >= 0; i--, currentPosition++ ) {
			constraintOrderedTableNames[currentPosition] = naturalOrderSubclassTableNameClosure[i];
			constraintOrderedKeyColumnNames[currentPosition] = naturalOrderSubclassTableKeyColumnClosure[i];
		}
		subclassTableNameClosure = reverse( naturalOrderSubclassTableNameClosure, coreTableSpan );
		subclassTableKeyColumnClosure = reverse( naturalOrderSubclassTableKeyColumnClosure, coreTableSpan );

		// PROPERTIES


		ArrayList<Integer> columnTableNumbers = new ArrayList<Integer>();
		ArrayList<Integer> formulaTableNumbers = new ArrayList<Integer>();
		ArrayList<Integer> propTableNumbers = new ArrayList<Integer>();
		for(int i=0;i<allAttributeBindings.length;i++){
			final AttributeBinding attributeBinding = allAttributeBindings[i];
			//if this is identifier, then continue
			if ( isIdentifierAttributeBinding( attributeBinding ) ) {
				continue;
			}
			final List<RelationalValueBinding> valueBindings;
			if ( SingularAttributeBinding.class.isInstance( attributeBinding ) ) {
				SingularAttributeBinding singularAttributeBinding = SingularAttributeBinding.class.cast(
						attributeBinding
				);
				valueBindings = singularAttributeBinding.getRelationalValueBindings();
			}
			else  {
				valueBindings = Collections.EMPTY_LIST;
			}
			TableSpecification table = attributeBinding.getContainer().seekEntityBinding().getPrimaryTable();// valueBinding.getValue().getTable();
			final String tableName = table.getQualifiedName( factory.getDialect() );
			if ( i < hydrateSpan ) {
				propertyTableNumbers[i] = getTableId( tableName, tableNames );
				naturalOrderPropertyTableNumbers[i] = getTableId( tableName, naturalOrderTableNames );
			}
			final int tableNumberInSubclass = getTableId( tableName, subclassTableNameClosure );
			propTableNumbers.add( tableNumberInSubclass );
			for ( RelationalValueBinding vb : valueBindings ) {
				if ( vb.isDerived() ) {
					formulaTableNumbers.add( tableNumberInSubclass );
				}
				else {
					columnTableNumbers.add( tableNumberInSubclass );
				}
			}
		}



		subclassColumnTableNumberClosure = ArrayHelper.toIntArray( columnTableNumbers );
		subclassPropertyTableNumberClosure = ArrayHelper.toIntArray( propTableNumbers );
		subclassFormulaTableNumberClosure = ArrayHelper.toIntArray( formulaTableNumbers );
		// SUBCLASSES

		// DISCRIMINATOR

		if ( entityBinding.isPolymorphic() ) {
			try {
				discriminatorValue = entityBinding.getSubEntityBindingId();
				discriminatorSQLString = discriminatorValue.toString();
			}
			catch ( Exception e ) {
				throw new MappingException( "Could not format discriminator value to SQL string", e );
			}
			subclassesByDiscriminatorValue.put( discriminatorValue, getEntityName() );
			discriminatorValues = new String[subclassSpan+1];
			discriminatorValues[subclassSpan] = discriminatorSQLString;
			notNullColumnTableNumbers = new int[subclassSpan+1];
			final int id = getTableId(
					tableNames[0], //the current entitybinding's primary table name
					subclassTableNameClosure
			);
			notNullColumnTableNumbers[subclassSpan] = id;
			notNullColumnNames = new String[subclassSpan+1];
			notNullColumnNames[subclassSpan] = subclassTableKeyColumnClosure[id][0];
		}
		else {
			discriminatorValues = null;
			notNullColumnTableNumbers = null;
			notNullColumnNames = null;
			discriminatorValue = null;
			discriminatorSQLString = null;
		}

		for ( int k = 0; k < postOrderSubEntityBindings.length; k++ ) {
			final EntityBinding eb = postOrderSubEntityBindings[k];
			subclassClosure[k] = eb.getEntityName();
			try {
				if ( entityBinding.isPolymorphic() ) {
					// we now use subclass ids that are consistent across all
					// persisters for a class hierarchy, so that the use of
					// "foo.class = Bar" works in HQL
					Integer subclassId = eb.getSubEntityBindingId();
					subclassesByDiscriminatorValue.put( subclassId, eb.getEntityName() );
					discriminatorValues[k] = subclassId.toString();
					int id = getTableId(
							eb.getPrimaryTable().getQualifiedName( factory.getDialect() ),
							subclassTableNameClosure
					);
					notNullColumnTableNumbers[k] = id;
					notNullColumnNames[k] = subclassTableKeyColumnClosure[id][0]; //( (Column) sc.getTable().getPrimaryKey().getColumnIterator().next() ).getName();
					if(notNullColumnNames[k] == null){
						System.out.println();
					}
				}
			}
			catch ( Exception e ) {
				throw new MappingException( "Error parsing discriminator value", e );
			}
		}

		initLockers();
		initSubclassPropertyAliasesMap( entityBinding );

		postConstruct( mapping );
	}



	private void resolvePkColumnNames(SessionFactoryImplementor factory, PrimaryKey primaryKey, String[] columns, String[] readers, String[] templates) {
		for ( int k = 0; k < primaryKey.getColumnSpan(); k++ ) {
			org.hibernate.metamodel.spi.relational.Column column = primaryKey.getColumns().get( k );
			columns[k] = column.getColumnName().getText( factory.getDialect() );
			readers[k] = column.getReadExpr( factory.getDialect() );
			templates[k] = column.getTemplate(
					factory.getDialect(),
					factory.getSqlFunctionRegistry()
			);
		}
	}

	protected boolean isNullableTable(int j) {
		return j >= coreTableSpan && isNullableTable[j - coreTableSpan];
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

	public Type getDiscriminatorType() {
		return StandardBasicTypes.INTEGER;
	}

	public Object getDiscriminatorValue() {
		return discriminatorValue;
	}

	public String getDiscriminatorSQLValue() {
		return discriminatorSQLString;
	}

	public String getSubclassForDiscriminatorValue(Object value) {
		return subclassesByDiscriminatorValue.get( value );
	}

	public Serializable[] getPropertySpaces() {
		return spaces; // don't need subclass tables, because they can't appear in conditions
	}


	protected String getTableName(int j) {
		return naturalOrderTableNames[j];
	}

	protected String[] getKeyColumns(int j) {
		return naturalOrderTableKeyColumns[j];
	}

	protected boolean isTableCascadeDeleteEnabled(int j) {
		return naturalOrderCascadeDeleteEnabled[j];
	}

	protected boolean isPropertyOfTable(int property, int j) {
		return naturalOrderPropertyTableNumbers[property] == j;
	}

	/**
	 * Load an instance using either the <tt>forUpdateLoader</tt> or the outer joining <tt>loader</tt>,
	 * depending upon the value of the <tt>lock</tt> parameter
	 */
	/*public Object load(Serializable id,	Object optionalObject, LockMode lockMode, SessionImplementor session)
	throws HibernateException {

		if ( log.isTraceEnabled() ) log.trace( "Materializing entity: " + MessageHelper.infoString(this, id) );

		final UniqueEntityLoader loader = hasQueryLoader() ?
				getQueryLoader() :
				this.loader;
		try {

			final Object result = loader.load(id, optionalObject, session);

			if (result!=null) lock(id, getVersion(result), result, lockMode, session);

			return result;

		}
		catch (SQLException sqle) {
			throw new JDBCException( "could not load by id: " +  MessageHelper.infoString(this, id), sqle );
		}
	}*/
	private static void reverse(Object[] objects, int len) {
		Object[] temp = new Object[len];
		for ( int i = 0; i < len; i++ ) {
			temp[i] = objects[len - i - 1];
		}
		System.arraycopy( temp, 0, objects, 0, len );
	}


	/**
	 * Reverse the first n elements of the incoming array
	 *
	 * @param objects
	 * @param n
	 *
	 * @return New array with the first n elements in reversed order
	 */
	private static String[] reverse(String[] objects, int n) {

		final int size = objects.length;
		final String[] temp = new String[size];

		for ( int i = 0; i < n; i++ ) {
			temp[i] = objects[n - i - 1];
		}

		System.arraycopy( objects, n, temp, n, size - n );

		return temp;
	}

	public static void main(String[] args) {
		String [] array = {"a", "b", "c", "d", "e"};
		array = reverse( array, 3 );
		for(String s : array){
			System.out.println(s);
		}
	}

	/**
	 * Reverse the first n elements of the incoming array
	 *
	 * @param objects
	 * @param n
	 *
	 * @return New array with the first n elements in reversed order
	 */
	private static String[][] reverse(String[][] objects, int n) {
		int size = objects.length;
		String[][] temp = new String[size][];
		for ( int i = 0; i < n; i++ ) {
			temp[i] = objects[n - i - 1];
		}

		System.arraycopy( objects, n, temp, n, size - n );

		return temp;
	}


	public String fromTableFragment(String alias) {
		return getTableName() + ' ' + alias;
	}

	public String getTableName() {
		return tableNames[0];
	}

	public void addDiscriminatorToSelect(SelectFragment select, String name, String suffix) {
		if ( hasSubclasses() ) {
			select.setExtraSelectList( discriminatorFragment( name ), getDiscriminatorAlias() );
		}
	}

	private CaseFragment discriminatorFragment(String alias) {
		CaseFragment cases = getFactory().getDialect().createCaseFragment();

		for ( int i = 0; i < discriminatorValues.length; i++ ) {
			cases.addWhenColumnNotNull(
					generateTableAlias( alias, notNullColumnTableNumbers[i] ),
					notNullColumnNames[i],
					discriminatorValues[i]
			);
		}

		return cases;
	}

	public String filterFragment(String alias) {
		return hasWhere() ?
				" and " + getSQLWhereString( generateFilterConditionAlias( alias ) ) :
				"";
	}

	public String generateFilterConditionAlias(String rootAlias) {
		return generateTableAlias( rootAlias, tableSpan - 1 );
	}

	public String[] getIdentifierColumnNames() {
		return tableKeyColumns[0];
	}

	public String[] getIdentifierColumnReaderTemplates() {
		return tableKeyColumnReaderTemplates[0];
	}

	public String[] getIdentifierColumnReaders() {
		return tableKeyColumnReaders[0];
	}

	public String[] toColumns(String alias, String propertyName) throws QueryException {
		if ( ENTITY_CLASS.equals( propertyName ) ) {
			// This doesn't actually seem to work but it *might*
			// work on some dbs. Also it doesn't work if there
			// are multiple columns of results because it
			// is not accounting for the suffix:
			// return new String[] { getDiscriminatorColumnName() };

			return new String[] { discriminatorFragment( alias ).toFragmentString() };
		}
		else {
			return super.toColumns( alias, propertyName );
		}
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

	public int getSubclassTableSpan() {
		return subclassTableNameClosure.length;
	}

	protected boolean isSubclassTableLazy(int j) {
		return subclassTableIsLazyClosure[j];
	}


	protected boolean isClassOrSuperclassTable(int j) {
		return isClassOrSuperclassTable[j];
	}

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

	public String getRootTableName() {
		return naturalOrderTableNames[0];
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
	public FilterAliasGenerator getFilterAliasGenerator(String rootAlias) {
		return new DynamicFilterAliasGenerator(subclassTableNameClosure, rootAlias);
	}
}
