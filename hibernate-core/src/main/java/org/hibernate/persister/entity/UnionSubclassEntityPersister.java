/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.NaturalIdDataAccess;
import org.hibernate.cfg.Settings;
import org.hibernate.dialect.Dialect;
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
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Subclass;
import org.hibernate.mapping.Table;
import org.hibernate.metamodel.mapping.EntityDiscriminatorMapping;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.internal.MappingModelCreationProcess;
import org.hibernate.persister.spi.PersisterCreationContext;
import org.hibernate.query.spi.NavigablePath;
import org.hibernate.sql.ast.spi.FromClauseAccess;
import org.hibernate.sql.ast.spi.SqlAliasBase;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.UnionTableGroup;
import org.hibernate.sql.ast.tree.from.UnionTableReference;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.type.BasicType;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;

/**
 * Implementation of the "table-per-concrete-class" or "roll-down" mapping
 * strategy for an entity and its inheritance hierarchy.
 *
 * @author Gavin King
 */
public class UnionSubclassEntityPersister extends AbstractEntityPersister {

	// the class hierarchy structure
	private final String subquery;
	private final String tableName;
	//private final String rootTableName;
	private final String[] subclassTableNames;
	private final String[] spaces;
	private final String[] subclassSpaces;
	private final String[] subclassTableExpressions;
	private final Object discriminatorValue;
	private final String discriminatorSQLValue;
	private final BasicType<?> discriminatorType;
	private final Map<Object,String> subclassByDiscriminatorValue = new HashMap<>();

	private final String[] constraintOrderedTableNames;
	private final String[][] constraintOrderedKeyColumnNames;

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

		// TABLE

		tableName = determineTableName( persistentClass.getTable() );
		subclassTableNames = new String[]{tableName};
		//Custom SQL

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
		customSQLInsert = new String[] {sql};
		insertCallable = new boolean[] {callable};
		insertResultCheckStyles = new ExecuteUpdateResultCheckStyle[] {checkStyle};

		sql = persistentClass.getCustomSQLUpdate();
		callable = sql != null && persistentClass.isCustomUpdateCallable();
		checkStyle = sql == null
				? ExecuteUpdateResultCheckStyle.COUNT
				: persistentClass.getCustomSQLUpdateCheckStyle() == null
				? ExecuteUpdateResultCheckStyle.determineDefault( sql, callable )
				: persistentClass.getCustomSQLUpdateCheckStyle();
		customSQLUpdate = new String[] {sql};
		updateCallable = new boolean[] {callable};
		updateResultCheckStyles = new ExecuteUpdateResultCheckStyle[] {checkStyle};

		sql = persistentClass.getCustomSQLDelete();
		callable = sql != null && persistentClass.isCustomDeleteCallable();
		checkStyle = sql == null
				? ExecuteUpdateResultCheckStyle.COUNT
				: persistentClass.getCustomSQLDeleteCheckStyle() == null
				? ExecuteUpdateResultCheckStyle.determineDefault( sql, callable )
				: persistentClass.getCustomSQLDeleteCheckStyle();
		customSQLDelete = new String[] {sql};
		deleteCallable = new boolean[] {callable};
		deleteResultCheckStyles = new ExecuteUpdateResultCheckStyle[] {checkStyle};

		discriminatorValue = persistentClass.getSubclassId();
		discriminatorSQLValue = String.valueOf( persistentClass.getSubclassId() );
		discriminatorType = factory.getTypeConfiguration()
				.getBasicTypeRegistry()
				.resolve( StandardBasicTypes.INTEGER );

		// PROPERTIES

		// SUBCLASSES
		subclassByDiscriminatorValue.put(
				persistentClass.getSubclassId(),
				persistentClass.getEntityName()
		);
		if ( persistentClass.isPolymorphic() ) {
			Iterator<Subclass> subclassIter = persistentClass.getSubclassIterator();
			while ( subclassIter.hasNext() ) {
				Subclass subclass = subclassIter.next();
				subclassByDiscriminatorValue.put( subclass.getSubclassId(), subclass.getEntityName() );
			}
		}

		//SPACES
		//TODO: I'm not sure, but perhaps we should exclude
		//      abstract denormalized tables?

		int spacesSize = 1 + persistentClass.getSynchronizedTables().size();
		spaces = new String[spacesSize];
		spaces[0] = tableName;
		Iterator<String> iter = persistentClass.getSynchronizedTables().iterator();
		for ( int i = 1; i < spacesSize; i++ ) {
			spaces[i] = iter.next();
		}

		HashSet<String> subclassTables = new HashSet<>();
		Iterator<Table> subclassTableIter = persistentClass.getSubclassTableClosureIterator();
		while ( subclassTableIter.hasNext() ) {
			subclassTables.add( determineTableName( subclassTableIter.next() ) );
		}
		subclassSpaces = ArrayHelper.toStringArray( subclassTables );

		subquery = generateSubquery( persistentClass, creationContext.getMetadata() );
		final List<String> tableExpressions = new ArrayList<>( subclassSpaces.length * 2 );
		Collections.addAll( tableExpressions, subclassSpaces );
		tableExpressions.add( subquery );
		PersistentClass parentPersistentClass = persistentClass.getSuperclass();
		while ( parentPersistentClass != null ) {
			tableExpressions.add( generateSubquery( parentPersistentClass, creationContext.getMetadata() ) );
			parentPersistentClass = parentPersistentClass.getSuperclass();
		}
		final Iterator<PersistentClass> subclassClosureIterator = persistentClass.getSubclassClosureIterator();
		while ( subclassClosureIterator.hasNext() ) {
			final PersistentClass subPersistentClass = subclassClosureIterator.next();
			if ( subPersistentClass.hasSubclasses() ) {
				tableExpressions.add( generateSubquery( subPersistentClass, creationContext.getMetadata() ) );
			}
		}
		subclassTableExpressions = ArrayHelper.toStringArray( tableExpressions );

		if ( isMultiTable() ) {
			int idColumnSpan = getIdentifierColumnSpan();
			ArrayList<String> tableNames = new ArrayList<>();
			ArrayList<String[]> keyColumns = new ArrayList<>();
			Iterator<Table> tableIter = persistentClass.getSubclassTableClosureIterator();
			while ( tableIter.hasNext() ) {
				Table tab = tableIter.next();
				if ( !tab.isAbstractUnionTable() ) {
					final String tableName = determineTableName( tab );
					tableNames.add( tableName );
					String[] key = new String[idColumnSpan];
					Iterator<Column> citer = tab.getPrimaryKey().getColumnIterator();
					for ( int k = 0; k < idColumnSpan; k++ ) {
						key[k] = citer.next().getQuotedName( factory.getJdbcServices().getDialect() );
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

		initSubclassPropertyAliasesMap( persistentClass );

		postConstruct( creationContext.getMetadata() );
	}

	@Override
	public boolean containsTableReference(String tableExpression) {
		for ( String subclassTableExpression : subclassTableExpressions ) {
			if ( subclassTableExpression.equals( tableExpression ) ) {
				return true;
			}
		}
		return false;
	}

	@Override
	public TableGroup createRootTableGroup(
			boolean canUseInnerJoins,
			NavigablePath navigablePath,
			String explicitSourceAlias,
			Supplier<Consumer<Predicate>> additionalPredicateCollectorAccess,
			SqlAliasBase sqlAliasBase,
			SqlExpressionResolver expressionResolver,
			FromClauseAccess fromClauseAccess,
			SqlAstCreationContext creationContext) {
		final UnionTableReference tableReference = resolvePrimaryTableReference( sqlAliasBase );

		return new UnionTableGroup( canUseInnerJoins, navigablePath, tableReference, this, explicitSourceAlias );
	}

	@Override
	protected UnionTableReference resolvePrimaryTableReference(SqlAliasBase sqlAliasBase) {
		return new UnionTableReference(
				getTableName(),
				subclassTableExpressions,
				sqlAliasBase.generateNewAlias(),
				false,
				getFactory()
		);
	}

	@Override
	public Serializable[] getQuerySpaces() {
		return subclassSpaces;
	}

	@Override
	public String getRootTableName() {
		return tableName;
	}

	@Override
	public String getTableName() {
		if ( hasSubclasses() ) {
			return subquery;
		}
		else {
			return tableName;
		}
	}

	@Override
	public Type getDiscriminatorType() {
		return discriminatorType;
	}

	@Override
	public Object getDiscriminatorValue() {
		return discriminatorValue;
	}

	@Override
	public String getDiscriminatorSQLValue() {
		return discriminatorSQLValue;
	}

	@Override
	public String getSubclassForDiscriminatorValue(Object value) {
		return subclassByDiscriminatorValue.get( value );
	}

	@Override
	public Serializable[] getPropertySpaces() {
		return spaces;
	}

	protected boolean isDiscriminatorFormula() {
		return false;
	}

	@Override
	protected boolean shouldProcessSuperMapping() {
		return false;
	}

	protected String getDiscriminatorFormula() {
		return null;
	}

	@Override
	public String getTableName(int j) {
		return tableName;
	}

	@Override
	public String[] getKeyColumns(int j) {
		return getIdentifierColumnNames();
	}

	@Override
	public boolean isTableCascadeDeleteEnabled(int j) {
		return false;
	}

	@Override
	public boolean isPropertyOfTable(int property, int j) {
		return true;
	}

	// Execute the SQL:

	@Override
	public String fromTableFragment(String name) {
		return getTableName() + ' ' + name;
	}

	@Override
	protected String filterFragment(String name) {
		return hasWhere() ? getSQLWhereString( name ) : "";
	}

	@Override
	protected String filterFragment(String alias, Set<String> treatAsDeclarations) {
		return filterFragment( alias );
	}

	@Override
	public String getSubclassPropertyTableName(int i) {
		return getTableName();//ie. the subquery! yuck!
	}

	@Override
	protected int[] getPropertyTableNumbersInSelect() {
		return new int[getPropertySpan()];
	}

	@Override
	protected int getSubclassPropertyTableNumber(int i) {
		return 0;
	}

	@Override
	public int getSubclassPropertyTableNumber(String propertyName) {
		return 0;
	}

	@Override
	public boolean isMultiTable() {
		// This could also just be true all the time...
		return isAbstract() || hasSubclasses();
	}

	@Override
	public void pruneForSubclasses(TableGroup tableGroup, Set<String> treatedEntityNames) {
		// If the base type is part of the treatedEntityNames this means we can't optimize this,
		// as the table group is e.g. returned through a select
		if ( treatedEntityNames.contains( getEntityName() ) ) {
			return;
		}
		final NamedTableReference tableReference = (NamedTableReference) tableGroup.resolveTableReference( getRootTableName() );
		// Replace the default union sub-query with a specially created one that only selects the tables for the treated entity names
		tableReference.setPrunedTableExpression( generateSubquery( treatedEntityNames ) );
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

	@Override
	protected boolean isPhysicalDiscriminator() {
		return false;
	}

	@Override
	protected EntityDiscriminatorMapping generateDiscriminatorMapping(MappingModelCreationProcess modelCreationProcess) {
		if ( hasSubclasses() ) {
			return super.generateDiscriminatorMapping( modelCreationProcess );
		}
		return null;
	}

	@Override
	public int getTableSpan() {
		return 1;
	}

	@Override
	protected int[] getSubclassColumnTableNumberClosure() {
		return new int[getSubclassColumnClosure().length];
	}

	@Override
	protected int[] getSubclassFormulaTableNumberClosure() {
		return new int[getSubclassFormulaClosure().length];
	}

	protected boolean[] getTableHasColumns() {
		return ArrayHelper.TRUE;
	}

	@Override
	protected int[] getPropertyTableNumbers() {
		return new int[getPropertySpan()];
	}

	protected String generateSubquery(PersistentClass model, Mapping mapping) {

		Dialect dialect = getFactory().getJdbcServices().getDialect();
		SqlStringGenerationContext sqlStringGenerationContext = getFactory().getSqlStringGenerationContext();

		if ( !model.hasSubclasses() ) {
			return model.getTable().getQualifiedName(
					sqlStringGenerationContext
			);
		}

		HashSet<Column> columns = new LinkedHashSet<>();
		Iterator<Table> titer = model.getSubclassTableClosureIterator();
		while ( titer.hasNext() ) {
			Table table = titer.next();
			if ( !table.isAbstractUnionTable() ) {
				Iterator<Column> citer = table.getColumnIterator();
				while ( citer.hasNext() ) {
					columns.add( citer.next() );
				}
			}
		}

		StringBuilder buf = new StringBuilder()
				.append( "( " );

		Iterator<PersistentClass> siter = new JoinedIterator<>(
				new SingletonIterator<>( model ),
				model.getSubclassIterator()
		);

		while ( siter.hasNext() ) {
			PersistentClass clazz = siter.next();
			Table table = clazz.getTable();
			if ( !table.isAbstractUnionTable() ) {
				//TODO: move to .sql package!!
				buf.append( "select " );
				for ( Column col : columns ) {
					if ( !table.containsColumn(col) ) {
						int sqlType = col.getSqlTypeCode(mapping);
						buf.append( dialect.getSelectClauseNullString(sqlType) )
								.append(" as ");
					}
					buf.append(col.getQuotedName(dialect));
					buf.append(", ");
				}
				buf.append( clazz.getSubclassId() )
						.append( " as clazz_" );
				buf.append( " from " )
						.append(
								table.getQualifiedName(
										sqlStringGenerationContext
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

	protected String generateSubquery(Set<String> treated) {
		if ( !hasSubclasses() ) {
			return getTableName();
		}

		final Dialect dialect = getFactory().getJdbcServices().getDialect();

		// Collect all selectables of every entity subtype and group by selection expression as well as table name
		final LinkedHashMap<String, Map<String, SelectableMapping>> selectables = new LinkedHashMap<>();
		visitSubTypeAttributeMappings(
				attributeMapping -> attributeMapping.forEachSelectable(
						(i, selectable) -> selectables.computeIfAbsent( selectable.getSelectionExpression(), k -> new HashMap<>() )
								.put( selectable.getContainingTableExpression(), selectable )
				)
		);
		// Collect the concrete subclass table names for the treated entity names
		final Set<String> treatedTableNames = new HashSet<>( treated.size() );
		for ( String subclassName : treated ) {
			final UnionSubclassEntityPersister subPersister = (UnionSubclassEntityPersister) getSubclassMappingType( subclassName );
			for ( String subclassTableName : subPersister.getSubclassTableNames() ) {
				if ( ArrayHelper.indexOf( subclassSpaces, subclassTableName ) != -1 ) {
					treatedTableNames.add( subclassTableName );
				}
			}
		}

		// Create a union sub-query for the table names, like generateSubquery(PersistentClass model, Mapping mapping)
		final StringBuilder buf = new StringBuilder( subquery.length() )
				.append( "( " );

		for ( int i = 0; i < subclassTableNames.length; i++ ) {
			final String subclassTableName = subclassTableNames[i];
			if ( treatedTableNames.contains( subclassTableName ) ) {
				buf.append( "select " );
				for ( Map<String, SelectableMapping> selectableMappings : selectables.values() ) {
					SelectableMapping selectableMapping = selectableMappings.get( subclassTableName );
					if ( selectableMapping == null ) {
						// If there is no selectable mapping for a table name, we render a null expression
						selectableMapping = selectableMappings.values().iterator().next();
						final int sqlType = selectableMapping.getJdbcMapping().getJdbcType()
								.getDefaultSqlTypeCode();
						buf.append( dialect.getSelectClauseNullString( sqlType ) )
								.append( " as " );
					}
					buf.append(
							new ColumnReference( (String) null, selectableMapping, getFactory() ).getExpressionText()
					);
					buf.append( ", " );
				}
				buf.append( i ).append( " as clazz_" );
				buf.append( " from " ).append( subclassTableName );
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
		if ( j != 0 ) {
			throw new AssertionFailure( "only one table" );
		}
		return getIdentifierColumnNames();
	}

	@Override
	public String getSubclassTableName(int j) {
		if ( j != 0 ) {
			throw new AssertionFailure( "only one table" );
		}
		return tableName;
	}

	@Override
	protected String[] getSubclassTableNames(){
		return subclassTableNames;
	}

	@Override
	public int getSubclassTableSpan() {
		return 1;
	}

	@Override
	protected boolean isClassOrSuperclassTable(int j) {
		if ( j != 0 ) {
			throw new AssertionFailure( "only one table" );
		}
		return true;
	}

	@Override
	public String getPropertyTableName(String propertyName) {
		//TODO: check this....
		return getTableName();
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
	public FilterAliasGenerator getFilterAliasGenerator(String rootAlias) {
		return new StaticFilterAliasGenerator( rootAlias );
	}
}
