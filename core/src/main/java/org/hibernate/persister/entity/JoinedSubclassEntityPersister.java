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
import java.util.Iterator;
import java.util.Map;

import org.hibernate.AssertionFailure;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.QueryException;
import org.hibernate.cache.access.EntityRegionAccessStrategy;
import org.hibernate.engine.ExecuteUpdateResultCheckStyle;
import org.hibernate.engine.Mapping;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.Versioning;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.Subclass;
import org.hibernate.mapping.Table;
import org.hibernate.sql.CaseFragment;
import org.hibernate.sql.SelectFragment;
import org.hibernate.type.AbstractType;
import org.hibernate.type.Type;
import org.hibernate.util.ArrayHelper;

/**
 * An <tt>EntityPersister</tt> implementing the normalized "table-per-subclass"
 * mapping strategy
 *
 * @author Gavin King
 */
public class JoinedSubclassEntityPersister extends AbstractEntityPersister {

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

	// subclass discrimination works by assigning particular
	// values to certain combinations of null primary key
	// values in the outer join using an SQL CASE
	private final Map subclassesByDiscriminatorValue = new HashMap();
	private final String[] discriminatorValues;
	private final String[] notNullColumnNames;
	private final int[] notNullColumnTableNumbers;

	private final String[] constraintOrderedTableNames;
	private final String[][] constraintOrderedKeyColumnNames;

	private final String discriminatorSQLString;

	//INITIALIZATION:

	public JoinedSubclassEntityPersister(
			final PersistentClass persistentClass,
			final EntityRegionAccessStrategy cacheAccessStrategy,
			final SessionFactoryImplementor factory,
			final Mapping mapping) throws HibernateException {

		super( persistentClass, cacheAccessStrategy, factory );

		// DISCRIMINATOR

		final Object discriminatorValue;
		if ( persistentClass.isPolymorphic() ) {
			try {
				discriminatorValue = new Integer( persistentClass.getSubclassId() );
				discriminatorSQLString = discriminatorValue.toString();
			}
			catch (Exception e) {
				throw new MappingException("Could not format discriminator value to SQL string", e );
			}
		}
		else {
			discriminatorValue = null;
			discriminatorSQLString = null;
		}

		if ( optimisticLockMode() > Versioning.OPTIMISTIC_LOCK_VERSION ) {
			throw new MappingException( "optimistic-lock=all|dirty not supported for joined-subclass mappings [" + getEntityName() + "]" );
		}

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
			tables.add(tabname);
			String[] keyCols = new String[idColumnSpan];
			String[] keyColReaders = new String[idColumnSpan];
			String[] keyColReaderTemplates = new String[idColumnSpan];
			Iterator citer = key.getColumnIterator();
			for ( int k=0; k<idColumnSpan; k++ ) {
				Column column = (Column) citer.next();
				keyCols[k] = column.getQuotedName( factory.getDialect() );
				keyColReaders[k] = column.getReadExpr( factory.getDialect() );
				keyColReaderTemplates[k] = column.getTemplate( factory.getDialect(), factory.getSqlFunctionRegistry() );
			}
			keyColumns.add(keyCols);
			keyColumnReaders.add(keyColReaders);
			keyColumnReaderTemplates.add(keyColReaderTemplates);
			cascadeDeletes.add( new Boolean( key.isCascadeDeleteEnabled() && factory.getDialect().supportsCascadeDelete() ) );
		}
		naturalOrderTableNames = ArrayHelper.toStringArray(tables);
		naturalOrderTableKeyColumns = ArrayHelper.to2DStringArray(keyColumns);
		naturalOrderTableKeyColumnReaders = ArrayHelper.to2DStringArray(keyColumnReaders);
		naturalOrderTableKeyColumnReaderTemplates = ArrayHelper.to2DStringArray(keyColumnReaderTemplates);
		naturalOrderCascadeDeleteEnabled = ArrayHelper.toBooleanArray(cascadeDeletes);

		ArrayList subtables = new ArrayList();
		ArrayList isConcretes = new ArrayList();
		keyColumns = new ArrayList();
		titer = persistentClass.getSubclassTableClosureIterator();
		while ( titer.hasNext() ) {
			Table tab = (Table) titer.next();
			isConcretes.add( new Boolean( persistentClass.isClassOrSuperclassTable(tab) ) );
			String tabname = tab.getQualifiedName(
					factory.getDialect(),
					factory.getSettings().getDefaultCatalogName(),
					factory.getSettings().getDefaultSchemaName()
			);
			subtables.add(tabname);
			String[] key = new String[idColumnSpan];
			Iterator citer = tab.getPrimaryKey().getColumnIterator();
			for ( int k=0; k<idColumnSpan; k++ ) {
				key[k] = ( (Column) citer.next() ).getQuotedName( factory.getDialect() );
			}
			keyColumns.add(key);
		}
		subclassTableNameClosure = ArrayHelper.toStringArray(subtables);
		subclassTableKeyColumnClosure = ArrayHelper.to2DStringArray(keyColumns);
		isClassOrSuperclassTable = ArrayHelper.toBooleanArray(isConcretes);

		constraintOrderedTableNames = new String[subclassTableNameClosure.length];
		constraintOrderedKeyColumnNames = new String[subclassTableNameClosure.length][];
		int currentPosition = 0;
		for ( int i = subclassTableNameClosure.length - 1; i >= 0 ; i--, currentPosition++ ) {
			constraintOrderedTableNames[currentPosition] = subclassTableNameClosure[i];
			constraintOrderedKeyColumnNames[currentPosition] = subclassTableKeyColumnClosure[i];
		}

		tableSpan = naturalOrderTableNames.length;
		tableNames = reverse(naturalOrderTableNames);
		tableKeyColumns = reverse(naturalOrderTableKeyColumns);
		tableKeyColumnReaders = reverse(naturalOrderTableKeyColumnReaders);
		tableKeyColumnReaderTemplates = reverse(naturalOrderTableKeyColumnReaderTemplates);
		reverse(subclassTableNameClosure, tableSpan);
		reverse(subclassTableKeyColumnClosure, tableSpan);

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
		int jk = tableSpan-1;
		while (pc!=null) {
			customSQLInsert[jk] = pc.getCustomSQLInsert();
			insertCallable[jk] = customSQLInsert[jk] != null && pc.isCustomInsertCallable();
			insertResultCheckStyles[jk] = pc.getCustomSQLInsertCheckStyle() == null
			                              ? ExecuteUpdateResultCheckStyle.determineDefault( customSQLInsert[jk], insertCallable[jk] )
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

		// PROPERTIES

		int hydrateSpan = getPropertySpan();
		naturalOrderPropertyTableNumbers = new int[hydrateSpan];
		propertyTableNumbers = new int[hydrateSpan];
		Iterator iter = persistentClass.getPropertyClosureIterator();
		int i=0;
		while( iter.hasNext() ) {
			Property prop = (Property) iter.next();
			String tabname = prop.getValue().getTable().getQualifiedName(
				factory.getDialect(),
				factory.getSettings().getDefaultCatalogName(),
				factory.getSettings().getDefaultSchemaName()
			);
			propertyTableNumbers[i] = getTableId(tabname, tableNames);
			naturalOrderPropertyTableNumbers[i] = getTableId(tabname, naturalOrderTableNames);
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
			Integer tabnum = new Integer( getTableId(tabname, subclassTableNameClosure) );
			propTableNumbers.add(tabnum);

			Iterator citer = prop.getColumnIterator();
			while ( citer.hasNext() ) {
				Selectable thing = (Selectable) citer.next();
				if ( thing.isFormula() ) {
					formulaTableNumbers.add(tabnum);
				}
				else {
					columnTableNumbers.add(tabnum);
				}
			}

		}

		subclassColumnTableNumberClosure = ArrayHelper.toIntArray(columnTableNumbers);
		subclassPropertyTableNumberClosure = ArrayHelper.toIntArray(propTableNumbers);
		subclassFormulaTableNumberClosure = ArrayHelper.toIntArray(formulaTableNumbers);

		// SUBCLASSES

		int subclassSpan = persistentClass.getSubclassSpan() + 1;
		subclassClosure = new String[subclassSpan];
		subclassClosure[subclassSpan-1] = getEntityName();
		if ( persistentClass.isPolymorphic() ) {
			subclassesByDiscriminatorValue.put( discriminatorValue, getEntityName() );
			discriminatorValues = new String[subclassSpan];
			discriminatorValues[subclassSpan-1] = discriminatorSQLString;
			notNullColumnTableNumbers = new int[subclassSpan];
			final int id = getTableId(
				persistentClass.getTable().getQualifiedName(
						factory.getDialect(),
						factory.getSettings().getDefaultCatalogName(),
						factory.getSettings().getDefaultSchemaName()
				),
				subclassTableNameClosure
			);
			notNullColumnTableNumbers[subclassSpan-1] = id;
			notNullColumnNames = new String[subclassSpan];
			notNullColumnNames[subclassSpan-1] =  subclassTableKeyColumnClosure[id][0]; //( (Column) model.getTable().getPrimaryKey().getColumnIterator().next() ).getName();
		}
		else {
			discriminatorValues = null;
			notNullColumnTableNumbers = null;
			notNullColumnNames = null;
		}

		iter = persistentClass.getSubclassIterator();
		int k=0;
		while ( iter.hasNext() ) {
			Subclass sc = (Subclass) iter.next();
			subclassClosure[k] = sc.getEntityName();
			try {
				if ( persistentClass.isPolymorphic() ) {
					// we now use subclass ids that are consistent across all
					// persisters for a class hierarchy, so that the use of
					// "foo.class = Bar" works in HQL
					Integer subclassId = new Integer( sc.getSubclassId() );//new Integer(k+1);
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
			catch (Exception e) {
				throw new MappingException("Error parsing discriminator value", e );
			}
			k++;
		}

		initLockers();

		initSubclassPropertyAliasesMap(persistentClass);

		postConstruct(mapping);

	}

	/*public void postInstantiate() throws MappingException {
		super.postInstantiate();
		//TODO: other lock modes?
		loader = createEntityLoader(LockMode.NONE, CollectionHelper.EMPTY_MAP);
	}*/

	public String getSubclassPropertyTableName(int i) {
		return subclassTableNameClosure[ subclassPropertyTableNumberClosure[i] ];
	}

	public Type getDiscriminatorType() {
		return Hibernate.INTEGER;
	}

	public String getDiscriminatorSQLValue() {
		return discriminatorSQLString;
	}


	public String getSubclassForDiscriminatorValue(Object value) {
		return (String) subclassesByDiscriminatorValue.get(value);
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
		return naturalOrderPropertyTableNumbers[property]==j;
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
		for (int i=0; i<len; i++) {
			temp[i] = objects[len-i-1];
		}
		for (int i=0; i<len; i++) {
			objects[i] = temp[i];
		}
	}

	private static final String[] reverse(String[] objects) {
		int len = objects.length;
		String[] temp = new String[len];
		for (int i=0; i<len; i++) {
			temp[i] = objects[len-i-1];
		}
		return temp;
	}

	private static final String[][] reverse(String[][] objects) {
		int len = objects.length;
		String[][] temp = new String[len][];
		for (int i=0; i<len; i++) {
			temp[i] = objects[len-i-1];
		}
		return temp;
	}

	public String fromTableFragment(String alias) {
		return getTableName() + ' ' + alias;
	}

	public String getTableName() {
		return tableNames[0];
	}

	private static int getTableId(String tableName, String[] tables) {
		for ( int j=0; j<tables.length; j++ ) {
			if ( tableName.equals( tables[j] ) ) {
				return j;
			}
		}
		throw new AssertionFailure("Table " + tableName + " not found");
	}

	public void addDiscriminatorToSelect(SelectFragment select, String name, String suffix) {
		if ( hasSubclasses() ) {
			select.setExtraSelectList( discriminatorFragment(name), getDiscriminatorAlias() );
		}
	}

	private CaseFragment discriminatorFragment(String alias) {
		CaseFragment cases = getFactory().getDialect().createCaseFragment();

		for ( int i=0; i<discriminatorValues.length; i++ ) {
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
		return generateTableAlias( rootAlias, tableSpan-1 );
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

		if ( ENTITY_CLASS.equals(propertyName) ) {
			// This doesn't actually seem to work but it *might*
			// work on some dbs. Also it doesn't work if there
			// are multiple columns of results because it
			// is not accounting for the suffix:
			// return new String[] { getDiscriminatorColumnName() };

			return new String[] { discriminatorFragment(alias).toFragmentString() };
		}
		else {
			return super.toColumns(alias, propertyName);
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

	protected boolean isClassOrSuperclassTable(int j) {
		return isClassOrSuperclassTable[j];
	}

	public String getPropertyTableName(String propertyName) {
		Integer index = getEntityMetamodel().getPropertyIndexOrNull(propertyName);
		if ( index == null ) {
			return null;
		}
		return tableNames[ propertyTableNumbers[ index.intValue() ] ];
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
			// special case where we need to force incloude all subclass joins
			return Declarer.SUBCLASS;
		}
		return super.getSubclassPropertyDeclarer( propertyPath );
	}
}
