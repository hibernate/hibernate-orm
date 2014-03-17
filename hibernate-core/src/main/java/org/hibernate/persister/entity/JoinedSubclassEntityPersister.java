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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.hibernate.internal.util.MarkerObject;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Subclass;
import org.hibernate.metamodel.spi.binding.AttributeBinding;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.binding.EntityDiscriminator;
import org.hibernate.metamodel.spi.binding.RelationalValueBinding;
import org.hibernate.metamodel.spi.binding.SecondaryTable;
import org.hibernate.metamodel.spi.binding.SingularAttributeBinding;
import org.hibernate.metamodel.spi.domain.Hierarchical;
import org.hibernate.metamodel.spi.domain.MappedSuperclass;
import org.hibernate.metamodel.spi.relational.DerivedValue;
import org.hibernate.metamodel.spi.relational.PrimaryKey;
import org.hibernate.metamodel.spi.relational.TableSpecification;
import org.hibernate.sql.CaseFragment;
import org.hibernate.sql.InFragment;
import org.hibernate.sql.Insert;
import org.hibernate.sql.SelectFragment;
import org.hibernate.type.DiscriminatorType;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;

import org.jboss.logging.Logger;

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

	// subclass discrimination works by assigning particular
	// values to certain combinations of null primary key
	// values in the outer join using an SQL CASE
	private final Map subclassesByDiscriminatorValue = new HashMap();
	private final String[] discriminatorValues;
	private final String[] notNullColumnNames;
	private final int[] notNullColumnTableNumbers;

	private final String[] constraintOrderedTableNames;
	private final String[][] constraintOrderedKeyColumnNames;

	private final Object discriminatorValue;
	private final String discriminatorSQLString;
	private final DiscriminatorType discriminatorType;
	private final String explicitDiscriminatorColumnName;
	private final String discriminatorAlias;

	// Span of the tables directly mapped by this entity and super-classes, if any
	private final int coreTableSpan;
	// only contains values for SecondaryTables, ie. not tables part of the "coreTableSpan"
	private final boolean[] isNullableTable;

	//INITIALIZATION:

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
	 * @param persistentClass
	 * @param factory
	 * @return
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

	private String[][] buildSubclassNamesBySubclassTableMapping(EntityBinding entityBinding, SessionFactoryImplementor factory) {
		// this value represents the number of subclasses (and not the class itself)
		final int numberOfSubclassTables = subclassTableNameClosure.length - coreTableSpan;
		if ( numberOfSubclassTables == 0 ) {
			return new String[0][];
		}

		final String[][] mapping = new String[numberOfSubclassTables][];
		processPersistentClassHierarchy( entityBinding, true, factory, mapping );
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
		final Set<String> classNames = new HashSet<String>();

		final Iterator itr = persistentClass.getDirectSubclasses();
		while ( itr.hasNext() ) {
			final Subclass subclass = (Subclass) itr.next();
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
			org.hibernate.mapping.MappedSuperclass msc = persistentClass.getSuperMappedSuperclass();
			while ( msc != null ) {
				classNames.add( msc.getMappedClass().getName() );
				msc = msc.getSuperMappedSuperclass();
			}

			associateSubclassNamesToSubclassTableIndexes( persistentClass, classNames, mapping, factory );
		}

		return classNames;
	}

	private Set<String> processPersistentClassHierarchy(
			EntityBinding entityBinding,
			boolean isBase,
			SessionFactoryImplementor factory,
			String[][] mapping) {

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// collect all the class names that indicate that the "main table" of the given PersistentClass should be
		// included when one of the collected class names is used in TREAT
		final Set<String> classNames = new HashSet<String>();

		for ( EntityBinding subEntityBinding : entityBinding.getDirectSubEntityBindings() ) {
			final Set<String> subclassSubclassNames = processPersistentClassHierarchy(
					subEntityBinding,
					false,
					factory,
					mapping
			);
			classNames.addAll( subclassSubclassNames );
		}

		classNames.add( entityBinding.getEntityName() );

		if ( ! isBase ) {
			Hierarchical superType = entityBinding.getEntity().getSuperType();
			while ( superType != null ) {
				if ( MappedSuperclass.class.isInstance( superType ) ) {
					classNames.add( superType.getDescriptor().getName().toString() );
				}
				superType = superType.getSuperType();
			}

			associateSubclassNamesToSubclassTableIndexes( entityBinding, classNames, mapping, factory );
		}

		return classNames;
	}

	private void associateSubclassNamesToSubclassTableIndexes(
			PersistentClass persistentClass,
			Set<String> classNames,
			String[][] mapping,
			SessionFactoryImplementor factory) {

		final String tableName = persistentClass.getTable().getQualifiedName(
				factory.getDialect(),
				factory.getSettings().getDefaultCatalogName(),
				factory.getSettings().getDefaultSchemaName()
		);

		associateSubclassNamesToSubclassTableIndex( tableName, classNames, mapping );

		Iterator itr = persistentClass.getJoinIterator();
		while ( itr.hasNext() ) {
			final Join join = (Join) itr.next();
			final String secondaryTableName = join.getTable().getQualifiedName(
					factory.getDialect(),
					factory.getSettings().getDefaultCatalogName(),
					factory.getSettings().getDefaultSchemaName()
			);
			associateSubclassNamesToSubclassTableIndex( secondaryTableName, classNames, mapping );
		}
	}

	private void associateSubclassNamesToSubclassTableIndexes(
			EntityBinding entityBinding,
			Set<String> classNames,
			String[][] mapping,
			SessionFactoryImplementor factory) {

		final String tableName = entityBinding.getPrimaryTable().getQualifiedName( factory.getDialect() );

		associateSubclassNamesToSubclassTableIndex( tableName, classNames, mapping );

		for ( SecondaryTable secondaryTable : entityBinding.getSecondaryTables().values() ) {
			final String secondaryTableName =
					secondaryTable.getSecondaryTableReference().getQualifiedName( factory.getDialect() );
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
				mapping[index] = classNames.toArray( new String[ classNames.size() ] );
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
				coreTableSpan,
				naturalOrderSubclassTableNameClosure,
				coreTableSpan + subclassSpan,
				secondaryTableSpan
		);
		System.arraycopy(
				naturalOrderTableKeyColumns,
				coreTableSpan,
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
			final TableSpecification table;
			if ( valueBindings.isEmpty() ) {
				table = attributeBinding.getContainer().seekEntityBinding().getPrimaryTable();
			}
			else {
				// TODO: Can relational value bindings for an attribute binding be in more than one table?
				// For now, just get the table from the first one.
				table = valueBindings.get( 0 ).getTable();
			}

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
			final EntityDiscriminator discriminator = entityBinding.getHierarchyDetails().getEntityDiscriminator();
			if ( discriminator != null ) {
				log.debug( "Encountered explicit discriminator mapping for joined inheritance" );

				final org.hibernate.metamodel.spi.relational.Value relationalValue = discriminator.getRelationalValue();
				if ( DerivedValue.class.isInstance( relationalValue ) ) {
					throw new MappingException( "Discriminator formulas on joined inheritance hierarchies not supported at this time" );
				}
				else {
					final org.hibernate.metamodel.spi.relational.Column column =
							(org.hibernate.metamodel.spi.relational.Column) relationalValue;
					explicitDiscriminatorColumnName = column.getColumnName().getText( factory.getDialect() );
					discriminatorAlias = column.getAlias( factory.getDialect(), entityBinding.getPrimaryTable() );
				}
				discriminatorType =
						(DiscriminatorType) discriminator.getExplicitHibernateTypeDescriptor().getResolvedTypeMapping();
				if ( entityBinding.isDiscriminatorMatchValueNull() ) {
					discriminatorValue = NULL_DISCRIMINATOR;
					discriminatorSQLString = InFragment.NULL;
				}
				else if ( entityBinding.isDiscriminatorMatchValueNotNull() ) {
					discriminatorValue = NOT_NULL_DISCRIMINATOR;
					discriminatorSQLString = InFragment.NOT_NULL;
				}
				else {
					try {
						discriminatorValue = discriminatorType.stringToObject( entityBinding.getDiscriminatorMatchValue() );
						discriminatorSQLString = discriminatorType.objectToSQLString( discriminatorValue, factory.getDialect() );
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
				discriminatorType = StandardBasicTypes.INTEGER;
				try {
					discriminatorValue = entityBinding.getSubEntityBindingId();
					discriminatorSQLString = discriminatorValue.toString();
				}
				catch ( Exception e ) {
					throw new MappingException( "Could not format discriminator value to SQL string", e );
				}
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
			explicitDiscriminatorColumnName = null;
			discriminatorAlias = IMPLICIT_DISCRIMINATOR_ALIAS;
			discriminatorValues = null;
			notNullColumnTableNumbers = null;
			notNullColumnNames = null;
			discriminatorType = StandardBasicTypes.INTEGER;
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

		subclassNamesBySubclassTable = buildSubclassNamesBySubclassTableMapping( entityBinding, factory );

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

	private void assertOptimisticLockStyle() {
		if ( optimisticLockStyle() == OptimisticLockStyle.ALL || optimisticLockStyle() == OptimisticLockStyle.DIRTY ) {
			throw new MappingException( "optimistic-lock=all|dirty not supported for joined-subclass mappings [" + getEntityName() + "]" );
		}
	}

	protected boolean isNullableTable(int j) {
		if ( j < coreTableSpan ) {
			return false;
		}
		return isNullableTable[j - coreTableSpan];
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

	protected String getDiscriminatorAlias() {
		return discriminatorAlias;
	}

	public String getSubclassForDiscriminatorValue(Object value) {
		return (String) subclassesByDiscriminatorValue.get( value );
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
	private static final void reverse(Object[] objects, int len) {
		Object[] temp = new Object[len];
		for ( int i = 0; i < len; i++ ) {
			temp[i] = objects[len - i - 1];
		}
		for ( int i = 0; i < len; i++ ) {
			objects[i] = temp[i];
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

	public void addDiscriminatorToSelect(SelectFragment select, String name, String suffix) {
		if ( hasSubclasses() ) {
			if ( explicitDiscriminatorColumnName == null ) {
				select.setExtraSelectList( discriminatorFragment( name ), getDiscriminatorAlias() );
			}
			else {
				select.addColumn( name, explicitDiscriminatorColumnName, discriminatorAlias );
			}
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

	@Override
	public String filterFragment(String alias) {
		return hasWhere()
				? " and " + getSQLWhereString( generateFilterConditionAlias( alias ) )
				: "";
	}

	@Override
	public String filterFragment(String alias, Set<String> treatAsDeclarations) {
		return filterFragment( alias );
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

		if ( index > subclassNamesBySubclassTable.length ) {
			throw new IllegalArgumentException(
					"Given subclass table number is outside expected range [" + subclassNamesBySubclassTable.length
							+ "] as defined by subclassTableNameClosure/subclassClosure"
			);
		}

		return subclassNamesBySubclassTable[index];
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
	public FilterAliasGenerator getFilterAliasGenerator(String rootAlias) {
		return new DynamicFilterAliasGenerator(subclassTableNameClosure, rootAlias);
	}
}
