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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.spi.ExecuteUpdateResultCheckStyle;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.DynamicFilterAliasGenerator;
import org.hibernate.internal.FilterAliasGenerator;
import org.hibernate.internal.util.MarkerObject;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.metamodel.spi.binding.AttributeBinding;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.binding.EntityDiscriminator;
import org.hibernate.metamodel.spi.binding.RelationalValueBinding;
import org.hibernate.metamodel.spi.binding.SecondaryTable;
import org.hibernate.metamodel.spi.binding.SingularAttributeBinding;
import org.hibernate.metamodel.spi.relational.DerivedValue;
import org.hibernate.metamodel.spi.relational.TableSpecification;
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
	private final Map<String, Integer> propertyTableNumbersByNameAndSubclass = new HashMap<String, Integer>();
	
	private final Map<String, String> sequentialSelectStringsByEntityName = new HashMap<String, String>();

	private static final Object NULL_DISCRIMINATOR = new MarkerObject("<null discriminator>");
	private static final Object NOT_NULL_DISCRIMINATOR = new MarkerObject("<not null discriminator>");

	//INITIALIZATION:

	private void addSubclassByDiscriminatorValue(Object discriminatorValue, String entityName) {
		String mappedEntityName = (String) subclassesByDiscriminatorValue.put( discriminatorValue, entityName );
		if ( mappedEntityName != null ) {
			throw new MappingException(
					"Entities [" + entityName + "] and [" + mappedEntityName
							+ "] are mapped with the same discriminator value '" + discriminatorValue + "'."
			);
		}
	}

	public SingleTableEntityPersister(
			final EntityBinding entityBinding,
			final EntityRegionAccessStrategy cacheAccessStrategy,
			final NaturalIdRegionAccessStrategy naturalIdRegionAccessStrategy,
			final SessionFactoryImplementor factory,
			final Mapping mapping) throws HibernateException {

		super( entityBinding, cacheAccessStrategy, naturalIdRegionAccessStrategy, factory );

		// CLASS + TABLE

		joinSpan = entityBinding.getSecondaryTableClosureSpan() + 1;
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

		int j = 1;
		for ( SecondaryTable join : entityBinding.getSecondaryTableClosure() ) {
			qualifiedTableNames[j] = join.getSecondaryTableReference().getQualifiedName( factory.getDialect() );
			isInverseTable[j] = join.isInverse();
			isNullableTable[j] = join.isOptional();
			cascadeDeleteEnabled[j] = join.isCascadeDeleteEnabled() &&
					factory.getDialect().supportsCascadeDelete();

			initializeCustomSql( join.getCustomInsert(), j, customSQLInsert, insertCallable, insertResultCheckStyles );
			initializeCustomSql( join.getCustomUpdate(), j, customSQLUpdate, updateCallable, updateResultCheckStyles );
			initializeCustomSql( join.getCustomDelete(), j, customSQLDelete, deleteCallable, deleteResultCheckStyles );

			final List<org.hibernate.metamodel.spi.relational.Column> joinColumns = join.getForeignKeyReference().getSourceColumns();
			keyColumnNames[j] = new String[ joinColumns.size() ];
			int i = 0;
			for ( org.hibernate.metamodel.spi.relational.Column joinColumn : joinColumns ) {
				keyColumnNames[j][i++] = joinColumn.getColumnName().getText( factory.getDialect() );
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
				entityBinding.getSynchronizedTableNames()
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

		for ( SecondaryTable join : entityBinding.getEntitiesSecondaryTableClosure() ) {
			final boolean isConcrete = entityBinding.isClassOrSuperclassSecondaryTable( join );
			isConcretes.add( isConcrete );
			final boolean isDeferred = join.getFetchStyle() != FetchStyle.JOIN;
			isDeferreds.add( isDeferred );
			isInverses.add( join.isInverse() );
			isNullables.add( join.isOptional() );
			isLazies.add( lazyAvailable && join.isLazy() );
			hasDeferred = isDeferred && !isConcrete;
			subclassTables.add( join.getSecondaryTableReference().getQualifiedName( factory.getDialect() ) );
			final List<org.hibernate.metamodel.spi.relational.Column> joinColumns = join.getForeignKeyReference().getSourceColumns();
			String[] keyCols = new String[ joinColumns.size() ];
			int i = 0;
			for ( org.hibernate.metamodel.spi.relational.Column joinColumn : joinColumns ) {
				keyCols[i++] = joinColumn.getColumnName().getText( factory.getDialect() );
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

		if ( entityBinding.isPolymorphic() ) {
			EntityDiscriminator discriminator = entityBinding.getHierarchyDetails().getEntityDiscriminator();
			org.hibernate.metamodel.spi.relational.Value discriminatorRelationalValue = discriminator.getRelationalValue();
			if ( discriminatorRelationalValue == null ) {
				throw new MappingException("discriminator mapping required for single table polymorphic persistence");
			}
			forceDiscriminator = discriminator.isForced();
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
				org.hibernate.metamodel.spi.relational.Column column = (org.hibernate.metamodel.spi.relational.Column) discriminatorRelationalValue;
				discriminatorColumnName = column.getColumnName().getText( factory.getDialect() );
				discriminatorColumnReaders =
						column.getReadFragment() == null ?
								column.getColumnName().getText( factory.getDialect() ) :
								column.getReadFragment();
				discriminatorColumnReaderTemplate = getTemplateFromColumn( column, factory );
				discriminatorAlias = column.getAlias( factory.getDialect(), entityBinding.getPrimaryTable() );
				discriminatorFormula = null;
				discriminatorFormulaTemplate = null;
			}

			discriminatorType = discriminator
					.getExplicitHibernateTypeDescriptor()
					.getResolvedTypeMapping();

			if ( entityBinding.isDiscriminatorMatchValueNull() ) {
				discriminatorValue = NULL_DISCRIMINATOR;
				discriminatorSQLValue = InFragment.NULL;
				discriminatorInsertable = false;
			}
			else if ( entityBinding.isDiscriminatorMatchValueNotNull() ) {
				discriminatorValue = NOT_NULL_DISCRIMINATOR;
				discriminatorSQLValue = InFragment.NOT_NULL;
				discriminatorInsertable = false;
			}
			else {
				discriminatorInsertable = discriminator.isInserted()
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
		for( AttributeBinding attributeBinding : entityBinding.getNonIdAttributeBindingClosure() ) {
			// TODO: fix when joins are working (HHH-6391)
			//propertyTableNumbers[i++] = entityBinding.getJoinNumber( attributeBinding);
			final int tableNumber;
			if ( attributeBinding.getAttribute().isSingular() ) {
				SingularAttributeBinding singularAttributeBinding = (SingularAttributeBinding) attributeBinding;
				tableNumber = entityBinding.getSecondaryTableNumber( singularAttributeBinding );
			}
			else {
				tableNumber = 0;
			}
			propertyTableNumbers[ i++ ] = tableNumber;
		}

		//TODO: code duplication with JoinedSubclassEntityPersister

		ArrayList<Integer> columnJoinNumbers = new ArrayList<Integer>();
		ArrayList<Integer> formulaJoinedNumbers = new ArrayList<Integer>();
		ArrayList<Integer> propertyJoinNumbers = new ArrayList<Integer>();

		for ( AttributeBinding attributeBinding : entityBinding.getNonIdEntitiesAttributeBindingClosure() ) {
			final String entityName = attributeBinding.getContainer().seekEntityBinding().getEntityName();
			final String path = entityName + '.' + attributeBinding.getAttributePath().getFullPath();
			if ( attributeBinding.getAttribute().isSingular() ) {
				SingularAttributeBinding singularAttributeBinding = (SingularAttributeBinding) attributeBinding;
				int join = entityBinding.getSecondaryTableNumber( singularAttributeBinding );
				propertyJoinNumbers.add( join );
				// We need the name of the actual entity that contains this attribute binding.
				//TODO it should be folder.children but now it is ".children"
				propertyTableNumbersByNameAndSubclass.put( path, join );
				for ( RelationalValueBinding relationalValueBinding : singularAttributeBinding.getRelationalValueBindings() ) {
					if ( relationalValueBinding.isDerived() ) {
						formulaJoinedNumbers.add( join );
					}
					else {
						columnJoinNumbers.add( join );
					}
				}
			}
			else {
				propertyJoinNumbers.add( 0 );
				propertyTableNumbersByNameAndSubclass.put( path, 0 );
			}
		}
		subclassColumnTableNumberClosure = ArrayHelper.toIntArray(columnJoinNumbers);
		subclassFormulaTableNumberClosure = ArrayHelper.toIntArray(formulaJoinedNumbers);
		subclassPropertyTableNumberClosure = ArrayHelper.toIntArray(propertyJoinNumbers);

		int subclassSpan = entityBinding.getSubEntityBindingClosureSpan() + 1;
		subclassClosure = new String[subclassSpan];
		subclassClosure[0] = getEntityName();
		if ( entityBinding.isPolymorphic() ) {
			addSubclassByDiscriminatorValue( discriminatorValue, getEntityName() );
		}

		// SUBCLASSES
		if ( entityBinding.isPolymorphic() ) {
			int k=1;
			for ( EntityBinding subEntityBinding : entityBinding.getPostOrderSubEntityBindingClosure() ) {
				subclassClosure[k++] = subEntityBinding.getEntityName();
				if ( subEntityBinding.isDiscriminatorMatchValueNull() ) {
					addSubclassByDiscriminatorValue( NULL_DISCRIMINATOR, subEntityBinding.getEntityName() );
				}
				else if ( subEntityBinding.isDiscriminatorMatchValueNotNull() ) {
					addSubclassByDiscriminatorValue( NOT_NULL_DISCRIMINATOR, subEntityBinding.getEntityName() );
				}
				else {
					try {
						DiscriminatorType dtype = (DiscriminatorType) discriminatorType;
						addSubclassByDiscriminatorValue(
							dtype.stringToObject( subEntityBinding.getDiscriminatorMatchValue() ),
							subEntityBinding.getEntityName()
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

	@Override
	public String filterFragment(String alias) throws MappingException {
		String result = discriminatorFilterFragment(alias);
		if ( hasWhere() ) result += " and " + getSQLWhereString(alias);
		return result;
	}

	private String discriminatorFilterFragment(String alias) throws MappingException {
		return discriminatorFilterFragment( alias, null );
	}
	
	public String oneToManyFilterFragment(String alias) throws MappingException {
		return forceDiscriminator
				? discriminatorFilterFragment( alias, null )
				: "";
	}

	@Override
	public String oneToManyFilterFragment(String alias, Set<String> treatAsDeclarations) {
		return needsDiscriminator()
				? discriminatorFilterFragment( alias, treatAsDeclarations )
				: "";
	}

	@Override
	public String filterFragment(String alias, Set<String> treatAsDeclarations) {
		String result = discriminatorFilterFragment( alias, treatAsDeclarations );
		if ( hasWhere() ) {
			result += " and " + getSQLWhereString( alias );
		}
		return result;
	}

	private String discriminatorFilterFragment(String alias, Set<String> treatAsDeclarations)  {
		final boolean hasTreatAs = treatAsDeclarations != null && !treatAsDeclarations.isEmpty();

		if ( !needsDiscriminator() && !hasTreatAs) {
			return "";
		}

		final InFragment frag = new InFragment();
		if ( isDiscriminatorFormula() ) {
			frag.setFormula( alias, getDiscriminatorFormulaTemplate() );
		}
		else {
			frag.setColumn( alias, getDiscriminatorColumnName() );
		}

		if ( hasTreatAs ) {
			frag.addValues( decodeTreatAsRequests( treatAsDeclarations ) );
		}
		else {
			frag.addValues( fullDiscriminatorValues() );
		}

		return " and " + frag.toFragmentString();
	}

	private boolean needsDiscriminator() {
		return forceDiscriminator || isInherited();
	}

	private String[] decodeTreatAsRequests(Set<String> treatAsDeclarations) {
		final List<String> values = new ArrayList<String>();
		for ( String subclass : treatAsDeclarations ) {
			final Queryable queryable = (Queryable) getFactory().getEntityPersister( subclass );
			if ( !queryable.isAbstract() ) {
				values.add( queryable.getDiscriminatorSQLValue() );
			}
		}
		return values.toArray( new String[ values.size() ] );
	}

	private String[] fullDiscriminatorValues;

	private String[] fullDiscriminatorValues() {
		if ( fullDiscriminatorValues == null ) {
			// first access; build it
			final List<String> values = new ArrayList<String>();
			for ( String subclass : getSubclassClosure() ) {
				final Queryable queryable = (Queryable) getFactory().getEntityPersister( subclass );
				if ( !queryable.isAbstract() ) {
					values.add( queryable.getDiscriminatorSQLValue() );
				}
			}
			fullDiscriminatorValues = values.toArray( new String[values.size() ] );
		}

		return fullDiscriminatorValues;
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
		return tabnum==null ? 0 : tabnum;
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
		return qualifiedTableNames[ propertyTableNumbers[index] ];
	}
	
	protected void doPostInstantiate() {
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
