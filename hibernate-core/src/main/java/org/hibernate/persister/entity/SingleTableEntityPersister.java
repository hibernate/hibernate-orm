/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.persister.entity;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy;
import org.hibernate.engine.spi.ExecuteUpdateResultCheckStyle;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.DynamicFilterAliasGenerator;
import org.hibernate.internal.FilterAliasGenerator;
import org.hibernate.internal.util.MarkerObject;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Formula;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.Subclass;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.Value;
import org.hibernate.metamodel.binding.AttributeBinding;
import org.hibernate.metamodel.binding.CustomSQL;
import org.hibernate.metamodel.binding.EntityBinding;
import org.hibernate.metamodel.binding.SimpleValueBinding;
import org.hibernate.metamodel.binding.SingularAttributeBinding;
import org.hibernate.metamodel.relational.DerivedValue;
import org.hibernate.metamodel.relational.SimpleValue;
import org.hibernate.metamodel.relational.TableSpecification;
import org.hibernate.sql.InFragment;
import org.hibernate.sql.Insert;
import org.hibernate.sql.SelectFragment;
import org.hibernate.type.AssociationType;
import org.hibernate.type.DiscriminatorType;
import org.hibernate.type.Type;

/**
 * The default implementation of the <tt>EntityPersister</tt> interface.
 * Implements the "table-per-class-hierarchy" or "roll-up" mapping strategy
 * for an entity class and its inheritence hierarchy.  This is implemented
 * as a single table holding all classes in the hierarchy with a discrimator
 * column used to determine which concrete class is referenced.
 *
 * @author Gavin King
 */
public class SingleTableEntityPersister extends AbstractEntityPersister {

	// the class hierarchy structure
	private final int joinSpan;
	private final String[] qualifiedTableNames;
	private final boolean[] isInverseTable;
	private final boolean[] isNullableTable;
	private final String[][] keyColumnNames;
	private final boolean[] cascadeDeleteEnabled;
	private final boolean hasSequentialSelects;
	
	private final String[] spaces;

	private final String[] subclassClosure;

	private final String[] subclassTableNameClosure;
	private final boolean[] subclassTableIsLazyClosure;
	private final boolean[] isInverseSubclassTable;
	private final boolean[] isNullableSubclassTable;
	private final boolean[] subclassTableSequentialSelect;
	private final String[][] subclassTableKeyColumnClosure;
	private final boolean[] isClassOrSuperclassTable;

	// properties of this class, including inherited properties
	private final int[] propertyTableNumbers;

	// the closure of all columns used by the entire hierarchy including
	// subclasses and superclasses of this class
	private final int[] subclassPropertyTableNumberClosure;

	private final int[] subclassColumnTableNumberClosure;
	private final int[] subclassFormulaTableNumberClosure;

	// discriminator column
	private final Map subclassesByDiscriminatorValue = new HashMap();
	private final boolean forceDiscriminator;
	private final String discriminatorColumnName;
	private final String discriminatorColumnReaders;
	private final String discriminatorColumnReaderTemplate;
	private final String discriminatorFormula;
	private final String discriminatorFormulaTemplate;
	private final String discriminatorAlias;
	private final Type discriminatorType;
	private final Object discriminatorValue;
	private final String discriminatorSQLValue;
	private final boolean discriminatorInsertable;

	private final String[] constraintOrderedTableNames;
	private final String[][] constraintOrderedKeyColumnNames;

	//private final Map propertyTableNumbersByName = new HashMap();
	private final Map propertyTableNumbersByNameAndSubclass = new HashMap();
	
	private final Map sequentialSelectStringsByEntityName = new HashMap();

	private static final Object NULL_DISCRIMINATOR = new MarkerObject("<null discriminator>");
	private static final Object NOT_NULL_DISCRIMINATOR = new MarkerObject("<not null discriminator>");
	private static final String NULL_STRING = "null";
	private static final String NOT_NULL_STRING = "not null";

	//INITIALIZATION:

	public SingleTableEntityPersister(
			final PersistentClass persistentClass, 
			final EntityRegionAccessStrategy cacheAccessStrategy,
			final NaturalIdRegionAccessStrategy naturalIdRegionAccessStrategy,
			final SessionFactoryImplementor factory,
			final Mapping mapping) throws HibernateException {

		super( persistentClass, cacheAccessStrategy, naturalIdRegionAccessStrategy, factory );

		// CLASS + TABLE

		joinSpan = persistentClass.getJoinClosureSpan()+1;
		qualifiedTableNames = new String[joinSpan];
		isInverseTable = new boolean[joinSpan];
		isNullableTable = new boolean[joinSpan];
		keyColumnNames = new String[joinSpan][];
		final Table table = persistentClass.getRootTable();
		qualifiedTableNames[0] = table.getQualifiedName( 
				factory.getDialect(), 
				factory.getSettings().getDefaultCatalogName(), 
				factory.getSettings().getDefaultSchemaName() 
		);
		isInverseTable[0] = false;
		isNullableTable[0] = false;
		keyColumnNames[0] = getIdentifierColumnNames();
		cascadeDeleteEnabled = new boolean[joinSpan];

		// Custom sql
		customSQLInsert = new String[joinSpan];
		customSQLUpdate = new String[joinSpan];
		customSQLDelete = new String[joinSpan];
		insertCallable = new boolean[joinSpan];
		updateCallable = new boolean[joinSpan];
		deleteCallable = new boolean[joinSpan];
		insertResultCheckStyles = new ExecuteUpdateResultCheckStyle[joinSpan];
		updateResultCheckStyles = new ExecuteUpdateResultCheckStyle[joinSpan];
		deleteResultCheckStyles = new ExecuteUpdateResultCheckStyle[joinSpan];

		customSQLInsert[0] = persistentClass.getCustomSQLInsert();
		insertCallable[0] = customSQLInsert[0] != null && persistentClass.isCustomInsertCallable();
		insertResultCheckStyles[0] = persistentClass.getCustomSQLInsertCheckStyle() == null
									  ? ExecuteUpdateResultCheckStyle.determineDefault( customSQLInsert[0], insertCallable[0] )
									  : persistentClass.getCustomSQLInsertCheckStyle();
		customSQLUpdate[0] = persistentClass.getCustomSQLUpdate();
		updateCallable[0] = customSQLUpdate[0] != null && persistentClass.isCustomUpdateCallable();
		updateResultCheckStyles[0] = persistentClass.getCustomSQLUpdateCheckStyle() == null
									  ? ExecuteUpdateResultCheckStyle.determineDefault( customSQLUpdate[0], updateCallable[0] )
									  : persistentClass.getCustomSQLUpdateCheckStyle();
		customSQLDelete[0] = persistentClass.getCustomSQLDelete();
		deleteCallable[0] = customSQLDelete[0] != null && persistentClass.isCustomDeleteCallable();
		deleteResultCheckStyles[0] = persistentClass.getCustomSQLDeleteCheckStyle() == null
									  ? ExecuteUpdateResultCheckStyle.determineDefault( customSQLDelete[0], deleteCallable[0] )
									  : persistentClass.getCustomSQLDeleteCheckStyle();

		// JOINS

		Iterator joinIter = persistentClass.getJoinClosureIterator();
		int j = 1;
		while ( joinIter.hasNext() ) {
			Join join = (Join) joinIter.next();
			qualifiedTableNames[j] = join.getTable().getQualifiedName( 
					factory.getDialect(), 
					factory.getSettings().getDefaultCatalogName(), 
					factory.getSettings().getDefaultSchemaName() 
			);
			isInverseTable[j] = join.isInverse();
			isNullableTable[j] = join.isOptional();
			cascadeDeleteEnabled[j] = join.getKey().isCascadeDeleteEnabled() && 
				factory.getDialect().supportsCascadeDelete();

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

			Iterator iter = join.getKey().getColumnIterator();
			keyColumnNames[j] = new String[ join.getKey().getColumnSpan() ];
			int i = 0;
			while ( iter.hasNext() ) {
				Column col = (Column) iter.next();
				keyColumnNames[j][i++] = col.getQuotedName( factory.getDialect() );
			}

			j++;
		}

		constraintOrderedTableNames = new String[qualifiedTableNames.length];
		constraintOrderedKeyColumnNames = new String[qualifiedTableNames.length][];
		for ( int i = qualifiedTableNames.length - 1, position = 0; i >= 0; i--, position++ ) {
			constraintOrderedTableNames[position] = qualifiedTableNames[i];
			constraintOrderedKeyColumnNames[position] = keyColumnNames[i];
		}

		spaces = ArrayHelper.join(
				qualifiedTableNames, 
				ArrayHelper.toStringArray( persistentClass.getSynchronizedTables() )
		);
		
		final boolean lazyAvailable = isInstrumented();

		boolean hasDeferred = false;
		ArrayList subclassTables = new ArrayList();
		ArrayList joinKeyColumns = new ArrayList();
		ArrayList<Boolean> isConcretes = new ArrayList<Boolean>();
		ArrayList<Boolean> isDeferreds = new ArrayList<Boolean>();
		ArrayList<Boolean> isInverses = new ArrayList<Boolean>();
		ArrayList<Boolean> isNullables = new ArrayList<Boolean>();
		ArrayList<Boolean> isLazies = new ArrayList<Boolean>();
		subclassTables.add( qualifiedTableNames[0] );
		joinKeyColumns.add( getIdentifierColumnNames() );
		isConcretes.add(Boolean.TRUE);
		isDeferreds.add(Boolean.FALSE);
		isInverses.add(Boolean.FALSE);
		isNullables.add(Boolean.FALSE);
		isLazies.add(Boolean.FALSE);
		joinIter = persistentClass.getSubclassJoinClosureIterator();
		while ( joinIter.hasNext() ) {
			Join join = (Join) joinIter.next();
			isConcretes.add( persistentClass.isClassOrSuperclassJoin(join) );
			isDeferreds.add( join.isSequentialSelect() );
			isInverses.add( join.isInverse() );
			isNullables.add( join.isOptional() );
			isLazies.add( lazyAvailable && join.isLazy() );
			if ( join.isSequentialSelect() && !persistentClass.isClassOrSuperclassJoin(join) ) hasDeferred = true;
			subclassTables.add( join.getTable().getQualifiedName( 
					factory.getDialect(), 
					factory.getSettings().getDefaultCatalogName(), 
					factory.getSettings().getDefaultSchemaName() 
			) );
			Iterator iter = join.getKey().getColumnIterator();
			String[] keyCols = new String[ join.getKey().getColumnSpan() ];
			int i = 0;
			while ( iter.hasNext() ) {
				Column col = (Column) iter.next();
				keyCols[i++] = col.getQuotedName( factory.getDialect() );
			}
			joinKeyColumns.add(keyCols);
		}
		
		subclassTableSequentialSelect = ArrayHelper.toBooleanArray(isDeferreds);
		subclassTableNameClosure = ArrayHelper.toStringArray(subclassTables);
		subclassTableIsLazyClosure = ArrayHelper.toBooleanArray(isLazies);
		subclassTableKeyColumnClosure = ArrayHelper.to2DStringArray( joinKeyColumns );
		isClassOrSuperclassTable = ArrayHelper.toBooleanArray(isConcretes);
		isInverseSubclassTable = ArrayHelper.toBooleanArray(isInverses);
		isNullableSubclassTable = ArrayHelper.toBooleanArray(isNullables);
		hasSequentialSelects = hasDeferred;

		// DISCRIMINATOR

		if ( persistentClass.isPolymorphic() ) {
			Value discrimValue = persistentClass.getDiscriminator();
			if (discrimValue==null) {
				throw new MappingException("discriminator mapping required for single table polymorphic persistence");
			}
			forceDiscriminator = persistentClass.isForceDiscriminator();
			Selectable selectable = (Selectable) discrimValue.getColumnIterator().next();
			if ( discrimValue.hasFormula() ) {
				Formula formula = (Formula) selectable;
				discriminatorFormula = formula.getFormula();
				discriminatorFormulaTemplate = formula.getTemplate( factory.getDialect(), factory.getSqlFunctionRegistry() );
				discriminatorColumnName = null;
				discriminatorColumnReaders = null;
				discriminatorColumnReaderTemplate = null;
				discriminatorAlias = "clazz_";
			}
			else {
				Column column = (Column) selectable;
				discriminatorColumnName = column.getQuotedName( factory.getDialect() );
				discriminatorColumnReaders = column.getReadExpr( factory.getDialect() );
				discriminatorColumnReaderTemplate = column.getTemplate( factory.getDialect(), factory.getSqlFunctionRegistry() );
				discriminatorAlias = column.getAlias( factory.getDialect(), persistentClass.getRootTable() );
				discriminatorFormula = null;
				discriminatorFormulaTemplate = null;
			}
			discriminatorType = persistentClass.getDiscriminator().getType();
			if ( persistentClass.isDiscriminatorValueNull() ) {
				discriminatorValue = NULL_DISCRIMINATOR;
				discriminatorSQLValue = InFragment.NULL;
				discriminatorInsertable = false;
			}
			else if ( persistentClass.isDiscriminatorValueNotNull() ) {
				discriminatorValue = NOT_NULL_DISCRIMINATOR;
				discriminatorSQLValue = InFragment.NOT_NULL;
				discriminatorInsertable = false;
			}
			else {
				discriminatorInsertable = persistentClass.isDiscriminatorInsertable() && !discrimValue.hasFormula();
				try {
					DiscriminatorType dtype = (DiscriminatorType) discriminatorType;
					discriminatorValue = dtype.stringToObject( persistentClass.getDiscriminatorValue() );
					discriminatorSQLValue = dtype.objectToSQLString( discriminatorValue, factory.getDialect() );
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
			forceDiscriminator = false;
			discriminatorInsertable = false;
			discriminatorColumnName = null;
			discriminatorColumnReaders = null;
			discriminatorColumnReaderTemplate = null;
			discriminatorAlias = null;
			discriminatorType = null;
			discriminatorValue = null;
			discriminatorSQLValue = null;
			discriminatorFormula = null;
			discriminatorFormulaTemplate = null;
		}

		// PROPERTIES

		propertyTableNumbers = new int[ getPropertySpan() ];
		Iterator iter = persistentClass.getPropertyClosureIterator();
		int i=0;
		while( iter.hasNext() ) {
			Property prop = (Property) iter.next();
			propertyTableNumbers[i++] = persistentClass.getJoinNumber(prop);

		}

		//TODO: code duplication with JoinedSubclassEntityPersister
		
		ArrayList columnJoinNumbers = new ArrayList();
		ArrayList formulaJoinedNumbers = new ArrayList();
		ArrayList propertyJoinNumbers = new ArrayList();
		
		iter = persistentClass.getSubclassPropertyClosureIterator();
		while ( iter.hasNext() ) {
			Property prop = (Property) iter.next();
			Integer join = persistentClass.getJoinNumber(prop);
			propertyJoinNumbers.add(join);

			//propertyTableNumbersByName.put( prop.getName(), join );
			propertyTableNumbersByNameAndSubclass.put( 
					prop.getPersistentClass().getEntityName() + '.' + prop.getName(), 
					join 
			);

			Iterator citer = prop.getColumnIterator();
			while ( citer.hasNext() ) {
				Selectable thing = (Selectable) citer.next();
				if ( thing.isFormula() ) {
					formulaJoinedNumbers.add(join);
				}
				else {
					columnJoinNumbers.add(join);
				}
			}
		}
		subclassColumnTableNumberClosure = ArrayHelper.toIntArray(columnJoinNumbers);
		subclassFormulaTableNumberClosure = ArrayHelper.toIntArray(formulaJoinedNumbers);
		subclassPropertyTableNumberClosure = ArrayHelper.toIntArray(propertyJoinNumbers);

		int subclassSpan = persistentClass.getSubclassSpan() + 1;
		subclassClosure = new String[subclassSpan];
		subclassClosure[0] = getEntityName();
		if ( persistentClass.isPolymorphic() ) {
			subclassesByDiscriminatorValue.put( discriminatorValue, getEntityName() );
		}

		// SUBCLASSES
		if ( persistentClass.isPolymorphic() ) {
			iter = persistentClass.getSubclassIterator();
			int k=1;
			while ( iter.hasNext() ) {
				Subclass sc = (Subclass) iter.next();
				subclassClosure[k++] = sc.getEntityName();
				if ( sc.isDiscriminatorValueNull() ) {
					subclassesByDiscriminatorValue.put( NULL_DISCRIMINATOR, sc.getEntityName() );
				}
				else if ( sc.isDiscriminatorValueNotNull() ) {
					subclassesByDiscriminatorValue.put( NOT_NULL_DISCRIMINATOR, sc.getEntityName() );
				}
				else {
					try {
						DiscriminatorType dtype = (DiscriminatorType) discriminatorType;
						subclassesByDiscriminatorValue.put(
							dtype.stringToObject( sc.getDiscriminatorValue() ),
							sc.getEntityName()
						);
					}
					catch (ClassCastException cce) {
						throw new MappingException("Illegal discriminator type: " + discriminatorType.getName() );
					}
					catch (Exception e) {
						throw new MappingException("Error parsing discriminator value", e);
					}
				}
			}
		}

		initLockers();

		initSubclassPropertyAliasesMap(persistentClass);
		
		postConstruct(mapping);

	}

	public SingleTableEntityPersister(
			final EntityBinding entityBinding,
			final EntityRegionAccessStrategy cacheAccessStrategy,
			final NaturalIdRegionAccessStrategy naturalIdRegionAccessStrategy,
			final SessionFactoryImplementor factory,
			final Mapping mapping) throws HibernateException {

		super( entityBinding, cacheAccessStrategy, naturalIdRegionAccessStrategy, factory );

		// CLASS + TABLE

		// TODO: fix when joins are working (HHH-6391)
		//joinSpan = entityBinding.getJoinClosureSpan() + 1;
		joinSpan = 1;
		qualifiedTableNames = new String[joinSpan];
		isInverseTable = new boolean[joinSpan];
		isNullableTable = new boolean[joinSpan];
		keyColumnNames = new String[joinSpan][];

		final TableSpecification table = entityBinding.getPrimaryTable();
		qualifiedTableNames[0] = table.getQualifiedName( factory.getDialect() );
		isInverseTable[0] = false;
		isNullableTable[0] = false;
		keyColumnNames[0] = getIdentifierColumnNames();
		cascadeDeleteEnabled = new boolean[joinSpan];

		// Custom sql
		customSQLInsert = new String[joinSpan];
		customSQLUpdate = new String[joinSpan];
		customSQLDelete = new String[joinSpan];
		insertCallable = new boolean[joinSpan];
		updateCallable = new boolean[joinSpan];
		deleteCallable = new boolean[joinSpan];
		insertResultCheckStyles = new ExecuteUpdateResultCheckStyle[joinSpan];
		updateResultCheckStyles = new ExecuteUpdateResultCheckStyle[joinSpan];
		deleteResultCheckStyles = new ExecuteUpdateResultCheckStyle[joinSpan];

		initializeCustomSql( entityBinding.getCustomInsert(), 0, customSQLInsert, insertCallable, insertResultCheckStyles );
		initializeCustomSql( entityBinding.getCustomUpdate(), 0, customSQLUpdate, updateCallable, updateResultCheckStyles );
		initializeCustomSql( entityBinding.getCustomDelete(), 0, customSQLDelete, deleteCallable, deleteResultCheckStyles );

		// JOINS

		// TODO: add join stuff when HHH-6391 is working

		constraintOrderedTableNames = new String[qualifiedTableNames.length];
		constraintOrderedKeyColumnNames = new String[qualifiedTableNames.length][];
		for ( int i = qualifiedTableNames.length - 1, position = 0; i >= 0; i--, position++ ) {
			constraintOrderedTableNames[position] = qualifiedTableNames[i];
			constraintOrderedKeyColumnNames[position] = keyColumnNames[i];
		}

		spaces = ArrayHelper.join(
				qualifiedTableNames,
				ArrayHelper.toStringArray( entityBinding.getSynchronizedTableNames() )
		);

		final boolean lazyAvailable = isInstrumented();

		boolean hasDeferred = false;
		ArrayList subclassTables = new ArrayList();
		ArrayList joinKeyColumns = new ArrayList();
		ArrayList<Boolean> isConcretes = new ArrayList<Boolean>();
		ArrayList<Boolean> isDeferreds = new ArrayList<Boolean>();
		ArrayList<Boolean> isInverses = new ArrayList<Boolean>();
		ArrayList<Boolean> isNullables = new ArrayList<Boolean>();
		ArrayList<Boolean> isLazies = new ArrayList<Boolean>();
		subclassTables.add( qualifiedTableNames[0] );
		joinKeyColumns.add( getIdentifierColumnNames() );
		isConcretes.add(Boolean.TRUE);
		isDeferreds.add(Boolean.FALSE);
		isInverses.add(Boolean.FALSE);
		isNullables.add(Boolean.FALSE);
		isLazies.add(Boolean.FALSE);

		// TODO: add join stuff when HHH-6391 is working


		subclassTableSequentialSelect = ArrayHelper.toBooleanArray(isDeferreds);
		subclassTableNameClosure = ArrayHelper.toStringArray(subclassTables);
		subclassTableIsLazyClosure = ArrayHelper.toBooleanArray(isLazies);
		subclassTableKeyColumnClosure = ArrayHelper.to2DStringArray( joinKeyColumns );
		isClassOrSuperclassTable = ArrayHelper.toBooleanArray(isConcretes);
		isInverseSubclassTable = ArrayHelper.toBooleanArray(isInverses);
		isNullableSubclassTable = ArrayHelper.toBooleanArray(isNullables);
		hasSequentialSelects = hasDeferred;

		// DISCRIMINATOR

		if ( entityBinding.isPolymorphic() ) {
			SimpleValue discriminatorRelationalValue = entityBinding.getHierarchyDetails().getEntityDiscriminator().getBoundValue();
			if ( discriminatorRelationalValue == null ) {
				throw new MappingException("discriminator mapping required for single table polymorphic persistence");
			}
			forceDiscriminator = entityBinding.getHierarchyDetails().getEntityDiscriminator().isForced();
			if ( DerivedValue.class.isInstance( discriminatorRelationalValue ) ) {
				DerivedValue formula = ( DerivedValue ) discriminatorRelationalValue;
				discriminatorFormula = formula.getExpression();
				discriminatorFormulaTemplate = getTemplateFromString( formula.getExpression(), factory );
				discriminatorColumnName = null;
				discriminatorColumnReaders = null;
				discriminatorColumnReaderTemplate = null;
				discriminatorAlias = "clazz_";
			}
			else {
				org.hibernate.metamodel.relational.Column column = ( org.hibernate.metamodel.relational.Column ) discriminatorRelationalValue;
				discriminatorColumnName = column.getColumnName().encloseInQuotesIfQuoted( factory.getDialect() );
				discriminatorColumnReaders =
						column.getReadFragment() == null ?
								column.getColumnName().encloseInQuotesIfQuoted( factory.getDialect() ) :
								column.getReadFragment();
				discriminatorColumnReaderTemplate = getTemplateFromColumn( column, factory );
				discriminatorAlias = column.getAlias( factory.getDialect() );
				discriminatorFormula = null;
				discriminatorFormulaTemplate = null;
			}

			discriminatorType = entityBinding.getHierarchyDetails()
					.getEntityDiscriminator()
					.getExplicitHibernateTypeDescriptor()
					.getResolvedTypeMapping();
			if ( entityBinding.getDiscriminatorMatchValue() == null ) {
				discriminatorValue = NULL_DISCRIMINATOR;
				discriminatorSQLValue = InFragment.NULL;
				discriminatorInsertable = false;
			}
			else if ( entityBinding.getDiscriminatorMatchValue().equals( NULL_STRING ) ) {
				discriminatorValue = NOT_NULL_DISCRIMINATOR;
				discriminatorSQLValue = InFragment.NOT_NULL;
				discriminatorInsertable = false;
			}
			else if ( entityBinding.getDiscriminatorMatchValue().equals( NOT_NULL_STRING ) ) {
				discriminatorValue = NOT_NULL_DISCRIMINATOR;
				discriminatorSQLValue = InFragment.NOT_NULL;
				discriminatorInsertable = false;
			}
			else {
				discriminatorInsertable = entityBinding.getHierarchyDetails().getEntityDiscriminator().isInserted()
						&& ! DerivedValue.class.isInstance( discriminatorRelationalValue );
				try {
					DiscriminatorType dtype = ( DiscriminatorType ) discriminatorType;
					discriminatorValue = dtype.stringToObject( entityBinding.getDiscriminatorMatchValue() );
					discriminatorSQLValue = dtype.objectToSQLString( discriminatorValue, factory.getDialect() );
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
			forceDiscriminator = false;
			discriminatorInsertable = false;
			discriminatorColumnName = null;
			discriminatorColumnReaders = null;
			discriminatorColumnReaderTemplate = null;
			discriminatorAlias = null;
			discriminatorType = null;
			discriminatorValue = null;
			discriminatorSQLValue = null;
			discriminatorFormula = null;
			discriminatorFormulaTemplate = null;
		}

		// PROPERTIES

		propertyTableNumbers = new int[ getPropertySpan() ];
		int i=0;
		for( AttributeBinding attributeBinding : entityBinding.getAttributeBindingClosure() ) {
			// TODO: fix when joins are working (HHH-6391)
			//propertyTableNumbers[i++] = entityBinding.getJoinNumber( attributeBinding);
			if ( attributeBinding == entityBinding.getHierarchyDetails().getEntityIdentifier().getValueBinding() ) {
				continue; // skip identifier binding
			}
			if ( ! attributeBinding.getAttribute().isSingular() ) {
				continue;
			}
			propertyTableNumbers[ i++ ] = 0;
		}

		//TODO: code duplication with JoinedSubclassEntityPersister

		ArrayList columnJoinNumbers = new ArrayList();
		ArrayList formulaJoinedNumbers = new ArrayList();
		ArrayList propertyJoinNumbers = new ArrayList();

		for ( AttributeBinding attributeBinding : entityBinding.getSubEntityAttributeBindingClosure() ) {
			if ( ! attributeBinding.getAttribute().isSingular() ) {
				continue;
			}
			SingularAttributeBinding singularAttributeBinding = (SingularAttributeBinding) attributeBinding;

			// TODO: fix when joins are working (HHH-6391)
			//int join = entityBinding.getJoinNumber(singularAttributeBinding);
			int join = 0;
			propertyJoinNumbers.add(join);

			//propertyTableNumbersByName.put( singularAttributeBinding.getName(), join );
			propertyTableNumbersByNameAndSubclass.put(
					singularAttributeBinding.getContainer().getPathBase() + '.' + singularAttributeBinding.getAttribute().getName(),
					join
			);

			for ( SimpleValueBinding simpleValueBinding : singularAttributeBinding.getSimpleValueBindings() ) {
				if ( DerivedValue.class.isInstance( simpleValueBinding.getSimpleValue() ) ) {
					formulaJoinedNumbers.add( join );
				}
				else {
					columnJoinNumbers.add( join );
				}
			}
		}
		subclassColumnTableNumberClosure = ArrayHelper.toIntArray(columnJoinNumbers);
		subclassFormulaTableNumberClosure = ArrayHelper.toIntArray(formulaJoinedNumbers);
		subclassPropertyTableNumberClosure = ArrayHelper.toIntArray(propertyJoinNumbers);

		int subclassSpan = entityBinding.getSubEntityBindingClosureSpan() + 1;
		subclassClosure = new String[subclassSpan];
		subclassClosure[0] = getEntityName();
		if ( entityBinding.isPolymorphic() ) {
			subclassesByDiscriminatorValue.put( discriminatorValue, getEntityName() );
		}

		// SUBCLASSES
		if ( entityBinding.isPolymorphic() ) {
			int k=1;
			for ( EntityBinding subEntityBinding : entityBinding.getPostOrderSubEntityBindingClosure() ) {
				subclassClosure[k++] = subEntityBinding.getEntity().getName();
				if ( subEntityBinding.isDiscriminatorMatchValueNull() ) {
					subclassesByDiscriminatorValue.put( NULL_DISCRIMINATOR, subEntityBinding.getEntity().getName() );
				}
				else if ( subEntityBinding.isDiscriminatorMatchValueNotNull() ) {
					subclassesByDiscriminatorValue.put( NOT_NULL_DISCRIMINATOR, subEntityBinding.getEntity().getName() );
				}
				else {
					try {
						DiscriminatorType dtype = (DiscriminatorType) discriminatorType;
						subclassesByDiscriminatorValue.put(
							dtype.stringToObject( subEntityBinding.getDiscriminatorMatchValue() ),
							subEntityBinding.getEntity().getName()
						);
					}
					catch (ClassCastException cce) {
						throw new MappingException("Illegal discriminator type: " + discriminatorType.getName() );
					}
					catch (Exception e) {
						throw new MappingException("Error parsing discriminator value", e);
					}
				}
			}
		}

		initLockers();

		initSubclassPropertyAliasesMap( entityBinding );

		postConstruct( mapping );
	}

	private static void initializeCustomSql(
			CustomSQL customSql,
			int i,
			String[] sqlStrings,
			boolean[] callable,
			ExecuteUpdateResultCheckStyle[] checkStyles) {
		sqlStrings[i] = customSql != null ?  customSql.getSql(): null;
		callable[i] = sqlStrings[i] != null && customSql.isCallable();
		checkStyles[i] = customSql != null && customSql.getCheckStyle() != null ?
				customSql.getCheckStyle() :
				ExecuteUpdateResultCheckStyle.determineDefault( sqlStrings[i], callable[i] );
	}

	protected boolean isInverseTable(int j) {
		return isInverseTable[j];
	}

	protected boolean isInverseSubclassTable(int j) {
		return isInverseSubclassTable[j];
	}

	public String getDiscriminatorColumnName() {
		return discriminatorColumnName;
	}

	public String getDiscriminatorColumnReaders() {
		return discriminatorColumnReaders;
	}			
	
	public String getDiscriminatorColumnReaderTemplate() {
		return discriminatorColumnReaderTemplate;
	}	
	
	protected String getDiscriminatorAlias() {
		return discriminatorAlias;
	}

	protected String getDiscriminatorFormulaTemplate() {
		return discriminatorFormulaTemplate;
	}

	public String getTableName() {
		return qualifiedTableNames[0];
	}

	public Type getDiscriminatorType() {
		return discriminatorType;
	}

	public Object getDiscriminatorValue() {
		return discriminatorValue;
	}

	public String getDiscriminatorSQLValue() {
		return discriminatorSQLValue;
	}

	public String[] getSubclassClosure() {
		return subclassClosure;
	}

	public String getSubclassForDiscriminatorValue(Object value) {
		if (value==null) {
			return (String) subclassesByDiscriminatorValue.get(NULL_DISCRIMINATOR);
		}
		else {
			String result = (String) subclassesByDiscriminatorValue.get(value);
			if (result==null) result = (String) subclassesByDiscriminatorValue.get(NOT_NULL_DISCRIMINATOR);
			return result;
		}
	}

	public Serializable[] getPropertySpaces() {
		return spaces;
	}

	//Access cached SQL

	protected boolean isDiscriminatorFormula() {
		return discriminatorColumnName==null;
	}

	protected String getDiscriminatorFormula() {
		return discriminatorFormula;
	}

	protected String getTableName(int j) {
		return qualifiedTableNames[j];
	}
	
	protected String[] getKeyColumns(int j) {
		return keyColumnNames[j];
	}
	
	protected boolean isTableCascadeDeleteEnabled(int j) {
		return cascadeDeleteEnabled[j];
	}
	
	protected boolean isPropertyOfTable(int property, int j) {
		return propertyTableNumbers[property]==j;
	}

	protected boolean isSubclassTableSequentialSelect(int j) {
		return subclassTableSequentialSelect[j] && !isClassOrSuperclassTable[j];
	}
	
	// Execute the SQL:

	public String fromTableFragment(String name) {
		return getTableName() + ' ' + name;
	}

	public String filterFragment(String alias) throws MappingException {
		String result = discriminatorFilterFragment(alias);
		if ( hasWhere() ) result += " and " + getSQLWhereString(alias);
		return result;
	}
	
	public String oneToManyFilterFragment(String alias) throws MappingException {
		return forceDiscriminator ?
			discriminatorFilterFragment(alias) :
			"";
	}

	private String discriminatorFilterFragment(String alias) throws MappingException {
		if ( needsDiscriminator() ) {
			InFragment frag = new InFragment();

			if ( isDiscriminatorFormula() ) {
				frag.setFormula( alias, getDiscriminatorFormulaTemplate() );
			}
			else {
				frag.setColumn( alias, getDiscriminatorColumnName() );
			}

			String[] subclasses = getSubclassClosure();
			for ( int i=0; i<subclasses.length; i++ ) {
				final Queryable queryable = (Queryable) getFactory().getEntityPersister( subclasses[i] );
				if ( !queryable.isAbstract() ) frag.addValue( queryable.getDiscriminatorSQLValue() );
			}

			StringBuilder buf = new StringBuilder(50)
				.append(" and ")
				.append( frag.toFragmentString() );

			return buf.toString();
		}
		else {
			return "";
		}
	}

	private boolean needsDiscriminator() {
		return forceDiscriminator || isInherited();
	}

	public String getSubclassPropertyTableName(int i) {
		return subclassTableNameClosure[ subclassPropertyTableNumberClosure[i] ];
	}

	protected void addDiscriminatorToSelect(SelectFragment select, String name, String suffix) {
		if ( isDiscriminatorFormula() ) {
			select.addFormula( name, getDiscriminatorFormulaTemplate(), getDiscriminatorAlias() );
		}
		else {
			select.addColumn( name, getDiscriminatorColumnName(),  getDiscriminatorAlias() );
		}
	}
	
	protected int[] getPropertyTableNumbersInSelect() {
		return propertyTableNumbers;
	}

	protected int getSubclassPropertyTableNumber(int i) {
		return subclassPropertyTableNumberClosure[i];
	}

	public int getTableSpan() {
		return joinSpan;
	}

	protected void addDiscriminatorToInsert(Insert insert) {

		if (discriminatorInsertable) {
			insert.addColumn( getDiscriminatorColumnName(), discriminatorSQLValue );
		}

	}

	protected int[] getSubclassColumnTableNumberClosure() {
		return subclassColumnTableNumberClosure;
	}

	protected int[] getSubclassFormulaTableNumberClosure() {
		return subclassFormulaTableNumberClosure;
	}

	protected int[] getPropertyTableNumbers() {
		return propertyTableNumbers;
	}
		
	protected boolean isSubclassPropertyDeferred(String propertyName, String entityName) {
		return hasSequentialSelects && 
			isSubclassTableSequentialSelect( getSubclassPropertyTableNumber(propertyName, entityName) );
	}
	
	public boolean hasSequentialSelect() {
		return hasSequentialSelects;
	}
	
	private int getSubclassPropertyTableNumber(String propertyName, String entityName) {
		Type type = propertyMapping.toType(propertyName);
		if ( type.isAssociationType() && ( (AssociationType) type ).useLHSPrimaryKey() ) return 0;
		final Integer tabnum = (Integer) propertyTableNumbersByNameAndSubclass.get(entityName + '.' + propertyName);
		return tabnum==null ? 0 : tabnum.intValue();
	}
	
	protected String getSequentialSelect(String entityName) {
		return (String) sequentialSelectStringsByEntityName.get(entityName);
	}

	private String generateSequentialSelect(Loadable persister) {
		//if ( this==persister || !hasSequentialSelects ) return null;

		//note that this method could easily be moved up to BasicEntityPersister,
		//if we ever needed to reuse it from other subclasses
		
		//figure out which tables need to be fetched
		AbstractEntityPersister subclassPersister = (AbstractEntityPersister) persister;
		HashSet tableNumbers = new HashSet();
		String[] props = subclassPersister.getPropertyNames();
		String[] classes = subclassPersister.getPropertySubclassNames();
		for ( int i=0; i<props.length; i++ ) {
			int propTableNumber = getSubclassPropertyTableNumber( props[i], classes[i] );
			if ( isSubclassTableSequentialSelect(propTableNumber) && !isSubclassTableLazy(propTableNumber) ) {
				tableNumbers.add( propTableNumber);
			}
		}
		if ( tableNumbers.isEmpty() ) return null;
		
		//figure out which columns are needed
		ArrayList columnNumbers = new ArrayList();
		final int[] columnTableNumbers = getSubclassColumnTableNumberClosure();
		for ( int i=0; i<getSubclassColumnClosure().length; i++ ) {
			if ( tableNumbers.contains( columnTableNumbers[i] ) ) {
				columnNumbers.add( i );
			}
		}
		
		//figure out which formulas are needed
		ArrayList formulaNumbers = new ArrayList();
		final int[] formulaTableNumbers = getSubclassColumnTableNumberClosure();
		for ( int i=0; i<getSubclassFormulaTemplateClosure().length; i++ ) {
			if ( tableNumbers.contains( formulaTableNumbers[i] ) ) {
				formulaNumbers.add( i );
			}
		}
		
		//render the SQL
		return renderSelect( 
			ArrayHelper.toIntArray(tableNumbers),
			ArrayHelper.toIntArray(columnNumbers),
			ArrayHelper.toIntArray(formulaNumbers)
		);
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

	protected boolean isClassOrSuperclassTable(int j) {
		return isClassOrSuperclassTable[j];
	}

	protected boolean isSubclassTableLazy(int j) {
		return subclassTableIsLazyClosure[j];
	}
	
	protected boolean isNullableTable(int j) {
		return isNullableTable[j];
	}
	
	protected boolean isNullableSubclassTable(int j) {
		return isNullableSubclassTable[j];
	}

	public String getPropertyTableName(String propertyName) {
		Integer index = getEntityMetamodel().getPropertyIndexOrNull(propertyName);
		if (index==null) return null;
		return qualifiedTableNames[ propertyTableNumbers[ index.intValue() ] ];
	}
	
	public void postInstantiate() {
		super.postInstantiate();
		if (hasSequentialSelects) {
			String[] entityNames = getSubclassClosure();
			for ( int i=1; i<entityNames.length; i++ ) {
				Loadable loadable = (Loadable) getFactory().getEntityPersister( entityNames[i] );
				if ( !loadable.isAbstract() ) { //perhaps not really necessary...
					String sequentialSelect = generateSequentialSelect(loadable);
					sequentialSelectStringsByEntityName.put( entityNames[i], sequentialSelect );
				}
			}
		}
	}

	public boolean isMultiTable() {
		return getTableSpan() > 1;
	}

	public String[] getConstraintOrderedTableNameClosure() {
		return constraintOrderedTableNames;
	}

	public String[][] getContraintOrderedTableKeyColumnClosure() {
		return constraintOrderedKeyColumnNames;
	}

	@Override
	public FilterAliasGenerator getFilterAliasGenerator(String rootAlias) {
		return new DynamicFilterAliasGenerator(qualifiedTableNames, rootAlias);
	}
}
