/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.NaturalIdDataAccess;
import org.hibernate.cfg.Settings;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.spi.ExecuteUpdateResultCheckStyle;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.id.IdentityGenerator;
import org.hibernate.internal.FilterAliasGenerator;
import org.hibernate.internal.StaticFilterAliasGenerator;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.internal.util.collections.JoinedIterator;
import org.hibernate.internal.util.collections.SingletonIterator;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.Subclass;
import org.hibernate.mapping.Table;
import org.hibernate.persister.spi.PersisterCreationContext;
import org.hibernate.sql.SelectFragment;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;

/**
 * Implementation of the "table-per-concrete-class" or "roll-down" mapping
 * strategy for an entity and its inheritence hierarchy.
 *
 * @author Gavin King
 */
public class UnionSubclassEntityPersister extends AbstractEntityPersister {

	// the class hierarchy structure
	private final String subquery;
	private final String tableName;
	private final int tableSpan;
	private final String[] tableNames;
	private final boolean[] isInverseTable;
	private final boolean[] isNullableTable;
	private final String[][] keyColumnNames;
	private final boolean[] cascadeDeleteEnabled;
	//private final String rootTableName;
	private final String[] subclassClosure;
	private final String[] spaces;
	private final String[] subclassSpaces;
	private final Object discriminatorValue;
	private final String discriminatorSQLValue;
	private final Map subclassByDiscriminatorValue = new HashMap();

	private final String[] constraintOrderedTableNames;
	private final String[][] constraintOrderedKeyColumnNames;

	// properties of this class, including inherited properties
	private final int[] propertyTableNumbers;

	// the closure of all columns used by the entire hierarchy including
	// subclasses and superclasses of this class
	private final int[] subclassPropertyTableNumberClosure;
	private final String[][] subclassTableKeyColumnClosure;
	private final String[] subclassTableNameClosure;
	private final boolean[] subclassTableIsLazyClosure;
	private final boolean[] subclassTableSequentialSelect;
	private final int[] subclassColumnTableNumberClosure;
	private final int[] subclassFormulaTableNumberClosure;

	private final boolean[] isClassOrSuperclassTable;
	private final boolean[] isInverseSubclassTable;
	private final boolean[] isNullableSubclassTable;

	//INITIALIZATION:

	public UnionSubclassEntityPersister(
			final PersistentClass persistentClass,
			final EntityDataAccess cacheAccessStrategy,
			final NaturalIdDataAccess naturalIdRegionAccessStrategy,
			final PersisterCreationContext creationContext) throws HibernateException {

		super( persistentClass, cacheAccessStrategy, naturalIdRegionAccessStrategy, creationContext );

		if ( getIdentifierGenerator() instanceof IdentityGenerator ) {
			throw new MappingException(
					"Cannot use identity column key generation with <union-subclass> mapping for: " +
							getEntityName()
			);
		}

		final SessionFactoryImplementor factory = creationContext.getSessionFactory();
		final Database database = creationContext.getMetadata().getDatabase();
		final JdbcEnvironment jdbcEnvironment = database.getJdbcEnvironment();

		// TABLE

		tableName = determineTableName( persistentClass.getTable(), jdbcEnvironment );
		tableSpan = persistentClass.getJoinClosureSpan() + 1;
		tableNames = new String[tableSpan];
		isInverseTable = new boolean[tableSpan];
		isNullableTable = new boolean[tableSpan];
		keyColumnNames = new String[tableSpan][];
		cascadeDeleteEnabled = new boolean[tableSpan];

		keyColumnNames[0] = getIdentifierColumnNames();
		tableNames[0] = tableName;
		isInverseTable[0] = false;
		isNullableTable[0] = false;


		//Custom SQL
		customSQLInsert = new String[tableSpan];
		customSQLUpdate = new String[tableSpan];
		customSQLDelete = new String[tableSpan];
		insertCallable = new boolean[tableSpan];
		updateCallable = new boolean[tableSpan];
		deleteCallable = new boolean[tableSpan];
		insertResultCheckStyles = new ExecuteUpdateResultCheckStyle[tableSpan];
		updateResultCheckStyles = new ExecuteUpdateResultCheckStyle[tableSpan];
		deleteResultCheckStyles = new ExecuteUpdateResultCheckStyle[tableSpan];

		String sql;
		boolean callable = false;
		ExecuteUpdateResultCheckStyle checkStyle = null;
		sql = persistentClass.getCustomSQLInsert();
		callable = sql != null && persistentClass.isCustomInsertCallable();
		checkStyle = sql == null
				? ExecuteUpdateResultCheckStyle.COUNT
				: persistentClass.getCustomSQLInsertCheckStyle() == null
				? ExecuteUpdateResultCheckStyle.determineDefault( sql, callable )
				: persistentClass.getCustomSQLInsertCheckStyle();
		customSQLInsert[0] = sql;
		insertCallable[0] = callable;
		insertResultCheckStyles[0] = checkStyle;

		sql = persistentClass.getCustomSQLUpdate();
		callable = sql != null && persistentClass.isCustomUpdateCallable();
		checkStyle = sql == null
				? ExecuteUpdateResultCheckStyle.COUNT
				: persistentClass.getCustomSQLUpdateCheckStyle() == null
				? ExecuteUpdateResultCheckStyle.determineDefault( sql, callable )
				: persistentClass.getCustomSQLUpdateCheckStyle();
		customSQLUpdate[0] = sql;
		updateCallable[0] = callable;
		updateResultCheckStyles[0] = checkStyle;

		sql = persistentClass.getCustomSQLDelete();
		callable = sql != null && persistentClass.isCustomDeleteCallable();
		checkStyle = sql == null
				? ExecuteUpdateResultCheckStyle.COUNT
				: persistentClass.getCustomSQLDeleteCheckStyle() == null
				? ExecuteUpdateResultCheckStyle.determineDefault( sql, callable )
				: persistentClass.getCustomSQLDeleteCheckStyle();
		customSQLDelete[0] = sql;
		deleteCallable[0] = callable;
		deleteResultCheckStyles[0] = checkStyle;

		discriminatorValue = persistentClass.getSubclassId();
		discriminatorSQLValue = String.valueOf( persistentClass.getSubclassId() );

		// JOINS

		Iterator joinIter = persistentClass.getJoinClosureIterator();
		for ( int j = 1; joinIter.hasNext(); j++ ) {
			Join join = (Join) joinIter.next();
			tableNames[j] = determineTableName( join.getTable(), jdbcEnvironment );
			isInverseTable[j] = join.isInverse();
			isNullableTable[j] = join.isOptional()
					|| creationContext.getSessionFactory()
					.getSessionFactoryOptions()
					.getJpaCompliance()
					.isJpaCacheComplianceEnabled();
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
			keyColumnNames[j] = new String[join.getKey().getColumnSpan()];
			int i = 0;
			while ( iter.hasNext() ) {
				Column col = (Column) iter.next();
				keyColumnNames[j][i++] = col.getQuotedName( factory.getDialect() );
			}
		}

		final boolean lazyAvailable = isInstrumented();

		boolean hasDeferred = false;
		ArrayList<String> subclassTableClosure = new ArrayList<String>();
		ArrayList<String[]> joinKeyColumns = new ArrayList<String[]>();
		ArrayList<Boolean> isConcretes = new ArrayList<Boolean>();
		ArrayList<Boolean> isDeferreds = new ArrayList<Boolean>();
		ArrayList<Boolean> isInverses = new ArrayList<Boolean>();
		ArrayList<Boolean> isNullables = new ArrayList<Boolean>();
		ArrayList<Boolean> isLazies = new ArrayList<Boolean>();
		subclassTableClosure.add( tableNames[0] );
		joinKeyColumns.add( getIdentifierColumnNames() );
		isConcretes.add( Boolean.TRUE );
		isDeferreds.add( Boolean.FALSE );
		isInverses.add( Boolean.FALSE );
		isNullables.add( Boolean.FALSE );
		isLazies.add( Boolean.FALSE );
		joinIter = persistentClass.getSubclassJoinClosureIterator();
		while ( joinIter.hasNext() ) {
			Join join = (Join) joinIter.next();
			isConcretes.add( persistentClass.isClassOrSuperclassJoin( join ) );
			isDeferreds.add( join.isSequentialSelect() );
			isInverses.add( join.isInverse() );
			isNullables.add(
					join.isOptional() || creationContext.getSessionFactory()
							.getSessionFactoryOptions()
							.getJpaCompliance()
							.isJpaCacheComplianceEnabled()
			);
			isLazies.add( lazyAvailable && join.isLazy() );
			if ( join.isSequentialSelect() && !persistentClass.isClassOrSuperclassJoin( join ) ) {
				hasDeferred = true;
			}
			subclassTableClosure.add( determineTableName( join.getTable(), jdbcEnvironment ) );
			Iterator iter = join.getKey().getColumnIterator();
			String[] keyCols = new String[join.getKey().getColumnSpan()];
			int i = 0;
			while ( iter.hasNext() ) {
				Column col = (Column) iter.next();
				keyCols[i++] = col.getQuotedName( factory.getDialect() );
			}
			joinKeyColumns.add( keyCols );
		}

		subclassTableSequentialSelect = ArrayHelper.toBooleanArray( isDeferreds );
		subclassTableNameClosure = ArrayHelper.toStringArray( subclassTableClosure );
		subclassTableIsLazyClosure = ArrayHelper.toBooleanArray( isLazies );
		subclassTableKeyColumnClosure = ArrayHelper.to2DStringArray( joinKeyColumns );
		isClassOrSuperclassTable = ArrayHelper.toBooleanArray( isConcretes );
		isInverseSubclassTable = ArrayHelper.toBooleanArray( isInverses );
		isNullableSubclassTable = ArrayHelper.toBooleanArray( isNullables );
//		hasSequentialSelects = hasDeferred; TODO

		// PROPERTIES
		int subclassSpan = persistentClass.getSubclassSpan() + 1;
		subclassClosure = new String[subclassSpan];
		subclassClosure[0] = getEntityName();

		propertyTableNumbers = new int[getPropertySpan()];
		Iterator propertyClosureIterator = persistentClass.getPropertyClosureIterator();
		for ( int i = 0; propertyClosureIterator.hasNext(); i++ ) {
			Property prop = (Property) propertyClosureIterator.next();
			propertyTableNumbers[i] = persistentClass.getJoinNumber( prop );
		}

		ArrayList<Integer> columnJoinNumbers = new ArrayList<Integer>();
		ArrayList<Integer> formulaJoinedNumbers = new ArrayList<Integer>();
		ArrayList<Integer> propertyJoinNumbers = new ArrayList<Integer>();

		Iterator subclassPropertyClosureIterator = persistentClass.getSubclassPropertyClosureIterator();
		while ( subclassPropertyClosureIterator.hasNext() ) {
			Property prop = (Property) subclassPropertyClosureIterator.next();
			Integer join = persistentClass.getJoinNumber( prop );
			propertyJoinNumbers.add( join );

			Iterator citer = prop.getColumnIterator();
			while ( citer.hasNext() ) {
				Selectable thing = (Selectable) citer.next();
				if ( thing.isFormula() ) {
					formulaJoinedNumbers.add( join );
				}
				else {
					columnJoinNumbers.add( join );
				}
			}
		}
		subclassColumnTableNumberClosure = ArrayHelper.toIntArray( columnJoinNumbers );
		subclassFormulaTableNumberClosure = ArrayHelper.toIntArray( formulaJoinedNumbers );
		subclassPropertyTableNumberClosure = ArrayHelper.toIntArray( propertyJoinNumbers );

		// SUBCLASSES
		subclassByDiscriminatorValue.put(
				persistentClass.getSubclassId(),
				persistentClass.getEntityName()
		);
		if ( persistentClass.isPolymorphic() ) {
			Iterator iter = persistentClass.getSubclassIterator();
			int k = 1;
			while ( iter.hasNext() ) {
				Subclass sc = (Subclass) iter.next();
				subclassClosure[k++] = sc.getEntityName();
				subclassByDiscriminatorValue.put( sc.getSubclassId(), sc.getEntityName() );
			}
		}

		//SPACES
		spaces = ArrayHelper.join(
				tableNames,
				ArrayHelper.toStringArray( persistentClass.getSynchronizedTables() )
		);

		HashSet subclassTables = new HashSet();
		Iterator subclassTableClosureIterator = persistentClass.getSubclassTableClosureIterator();
		while ( subclassTableClosureIterator.hasNext() ) {
			final Table table = (Table) subclassTableClosureIterator.next();
			subclassTables.add( determineTableName( table, jdbcEnvironment ) );
		}
		subclassSpaces = ArrayHelper.toStringArray( subclassTables );

		subquery = generateSubquery( persistentClass, creationContext.getMetadata() );

		if ( isMultiTable() ) {
			int idColumnSpan = getIdentifierColumnSpan();
			ArrayList tableNames = new ArrayList();
			ArrayList keyColumns = new ArrayList();
			if ( !isAbstract() ) {
				tableNames.add( tableName );
				keyColumns.add( getIdentifierColumnNames() );
			}
			Iterator iter = persistentClass.getSubclassTableClosureIterator();
			while ( iter.hasNext() ) {
				Table tab = (Table) iter.next();
				if ( !tab.isAbstractUnionTable() ) {
					final String tableName = determineTableName( tab, jdbcEnvironment );
					tableNames.add( tableName );
					String[] key = new String[idColumnSpan];
					Iterator citer = tab.getPrimaryKey().getColumnIterator();
					for ( int k = 0; k < idColumnSpan; k++ ) {
						key[k] = ( (Column) citer.next() ).getQuotedName( factory.getDialect() );
					}
					keyColumns.add( key );
				}
			}

			constraintOrderedTableNames = ArrayHelper.toStringArray( tableNames );
			constraintOrderedKeyColumnNames = ArrayHelper.to2DStringArray( keyColumns );
		}
		else {
			constraintOrderedTableNames = new String[] {tableName};
			constraintOrderedKeyColumnNames = new String[][] {getIdentifierColumnNames()};
		}

		initLockers();

		initSubclassPropertyAliasesMap( persistentClass );

		postConstruct( creationContext.getMetadata() );

	}

	public Serializable[] getQuerySpaces() {
		return subclassSpaces;
	}

	public String getTableName() {
		return subquery;
	}

	public Type getDiscriminatorType() {
		return StandardBasicTypes.INTEGER;
	}

	public Object getDiscriminatorValue() {
		return discriminatorValue;
	}

	public String getDiscriminatorSQLValue() {
		return discriminatorSQLValue;
	}

	public String getSubclassForDiscriminatorValue(Object value) {
		return (String) subclassByDiscriminatorValue.get( value );
	}

	public Serializable[] getPropertySpaces() {
		return spaces;
	}

	protected String getTableName(int j) {
		return tableNames[j];
	}

	protected String[] getKeyColumns(int j) {
		return keyColumnNames[j];
	}

	@Override
	protected boolean isTableCascadeDeleteEnabled(int j) {
		return cascadeDeleteEnabled[j];
	}

	@Override
	protected boolean isPropertyOfTable(int property, int j) {
		return propertyTableNumbers[property] == j;
	}

	@Override
	protected boolean isSubclassTableSequentialSelect(int j) {
		return subclassTableSequentialSelect[j] && !isClassOrSuperclassTable[j];
	}

	// Execute the SQL:

	public String fromTableFragment(String name) {
		return getTableName() + ' ' + name;
	}

	@Override
	public String filterFragment(String name) {
		return hasWhere()
				? " and " + getSQLWhereString( name )
				: "";
	}

	@Override
	protected String filterFragment(String alias, Set<String> treatAsDeclarations) {
		return filterFragment( alias );
	}

	public String getSubclassPropertyTableName(int i) {
		return subclassTableNameClosure[subclassPropertyTableNumberClosure[i]];
	}

	protected void addDiscriminatorToSelect(SelectFragment select, String name, String suffix) {
		select.addColumn( name, getDiscriminatorColumnName(), getDiscriminatorAlias() );
	}

	protected int[] getPropertyTableNumbersInSelect() {
		return propertyTableNumbers;
	}

	protected int getSubclassPropertyTableNumber(int i) {
		return subclassPropertyTableNumberClosure[i];
	}

	public boolean isMultiTable() {
		// This could also just be true all the time...
		return isAbstract() || hasSubclasses();
	}

	public int getTableSpan() {
		return tableSpan;
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

	protected String generateSubquery(PersistentClass model, Mapping mapping) {
		Dialect dialect = getFactory().getDialect();
		Settings settings = getFactory().getSettings();

		if ( !model.hasSubclasses() ) {
			return model.getTable().getQualifiedName(
					dialect,
					settings.getDefaultCatalogName(),
					settings.getDefaultSchemaName()
			);
		}

		HashSet columns = new LinkedHashSet();
		Iterator titer = model.getSubclassTableClosureIterator();
		while ( titer.hasNext() ) {
			Table table = (Table) titer.next();
			if ( !table.isAbstractUnionTable() ) {
				Iterator citer = table.getColumnIterator();
				while ( citer.hasNext() ) {
					columns.add( citer.next() );
				}
			}
		}

		StringBuilder buf = new StringBuilder()
				.append( "( " );

		Iterator siter = new JoinedIterator(
				new SingletonIterator( model ),
				model.getSubclassIterator()
		);

		while ( siter.hasNext() ) {
			PersistentClass clazz = (PersistentClass) siter.next();
			Table table = clazz.getTable();
			if ( !table.isAbstractUnionTable() ) {
				//TODO: move to .sql package!!
				buf.append( "select " );
				Iterator citer = columns.iterator();
				while ( citer.hasNext() ) {
					Column col = (Column) citer.next();
					if ( !table.containsColumn( col ) ) {
						int sqlType = col.getSqlTypeCode( mapping );
						buf.append( dialect.getSelectClauseNullString( sqlType ) )
								.append( " as " );
					}
					buf.append( col.getQuotedName( dialect ) );
					buf.append( ", " );
				}
				buf.append( clazz.getSubclassId() )
						.append( " as clazz_" );
				buf.append( " from " )
						.append(
								table.getQualifiedName(
										dialect,
										settings.getDefaultCatalogName(),
										settings.getDefaultSchemaName()
								)
						);
				buf.append( " union " );
				if ( dialect.supportsUnionAll() ) {
					buf.append( "all " );
				}
			}
		}

		if ( buf.length() > 2 ) {
			//chop the last union (all)
			buf.setLength( buf.length() - ( dialect.supportsUnionAll() ? 11 : 7 ) );
		}

		return buf.append( " )" ).toString();
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
	public int getSubclassTableSpan() {
		return subclassTableNameClosure.length;
	}

	@Override
	protected boolean isClassOrSuperclassTable(int j) {
		return isClassOrSuperclassTable[j];
	}

	@Override
	protected boolean isNullableTable(int j) {
		return isNullableTable[j];
	}

	@Override
	protected boolean isInverseTable(int j) {
		return isInverseTable[j];
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
	protected boolean isSubclassTableLazy(int j) {
		return subclassTableIsLazyClosure[j];
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

	@Override
	public FilterAliasGenerator getFilterAliasGenerator(String rootAlias) {
		return new StaticFilterAliasGenerator( rootAlias );
	}
}
