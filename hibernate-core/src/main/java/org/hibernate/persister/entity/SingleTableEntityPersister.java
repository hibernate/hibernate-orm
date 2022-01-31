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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.NaturalIdDataAccess;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.ExecuteUpdateResultCheckStyle;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.DynamicFilterAliasGenerator;
import org.hibernate.internal.FilterAliasGenerator;
import org.hibernate.internal.util.MarkerObject;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Formula;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.Subclass;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.Value;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.persister.spi.PersisterCreationContext;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.query.spi.NavigablePath;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.sql.InFragment;
import org.hibernate.sql.Insert;
import org.hibernate.sql.ast.spi.FromClauseAccess;
import org.hibernate.sql.ast.spi.SqlAliasBase;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.QueryLiteral;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.predicate.InListPredicate;
import org.hibernate.sql.ast.tree.predicate.Junction;
import org.hibernate.sql.ast.tree.predicate.NegatedPredicate;
import org.hibernate.sql.ast.tree.predicate.NullnessPredicate;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.type.BasicType;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;

/**
 * The default implementation of the {@link EntityPersister} interface.
 * Implements the {@link jakarta.persistence.InheritanceType#SINGLE_TABLE}
 * mapping strategy for an entity class and its inheritance hierarchy.
 * <p>
 * This is implemented as a single table for all classes of the hierarchy,
 * with a discriminator column used to determine which concrete class a
 * row represents.
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

	private final String[] spaces;

	private final String[] subclassClosure;

	private final String[] subclassTableNameClosure;
//	private final boolean[] subclassTableIsLazyClosure;
	private final boolean[] isInverseSubclassTable;
	private final boolean[] isNullableSubclassTable;
	private final boolean[] subclassTableSequentialSelect;
	private final String[][] subclassTableKeyColumnClosure;
	private final boolean[] isClassOrSuperclassTable;
	private final boolean[] isClassOrSuperclassJoin;

	// properties of this class, including inherited properties
	private final int[] propertyTableNumbers;

	// the closure of all columns used by the entire hierarchy including
	// subclasses and superclasses of this class
	private final int[] subclassPropertyTableNumberClosure;

//	private final int[] subclassColumnTableNumberClosure;
//	private final int[] subclassFormulaTableNumberClosure;

	// discriminator column
	private final Map<Object, String> subclassesByDiscriminatorValue;
	private final boolean forceDiscriminator;
	private final String discriminatorColumnName;
	private final String discriminatorColumnReaders;
	private final String discriminatorColumnReaderTemplate;
//	private final String discriminatorFormula;
	private final String discriminatorFormulaTemplate;
	private final String discriminatorAlias;
	private final BasicType<?> discriminatorType;
	private final Object discriminatorValue;
	private final String discriminatorSQLValue;
	private final boolean discriminatorInsertable;

	private final String[] constraintOrderedTableNames;
	private final String[][] constraintOrderedKeyColumnNames;

	//private final Map propertyTableNumbersByName = new HashMap();
//	private final Map<String, Integer> propertyTableNumbersByNameAndSubclass;

	private static final Object NULL_DISCRIMINATOR = new MarkerObject( "<null discriminator>" );
	private static final Object NOT_NULL_DISCRIMINATOR = new MarkerObject( "<not null discriminator>" );

	//INITIALIZATION:

	@Deprecated(since = "6.0")
	public SingleTableEntityPersister(
			final PersistentClass persistentClass,
			final EntityDataAccess cacheAccessStrategy,
			final NaturalIdDataAccess naturalIdRegionAccessStrategy,
			final PersisterCreationContext creationContext) throws HibernateException {
		this( persistentClass,cacheAccessStrategy,naturalIdRegionAccessStrategy,
				(RuntimeModelCreationContext) creationContext );
	}

	public SingleTableEntityPersister(
			final PersistentClass persistentClass,
			final EntityDataAccess cacheAccessStrategy,
			final NaturalIdDataAccess naturalIdRegionAccessStrategy,
			final RuntimeModelCreationContext creationContext) throws HibernateException {

		super( persistentClass, cacheAccessStrategy, naturalIdRegionAccessStrategy, creationContext );

		final SessionFactoryImplementor factory = creationContext.getSessionFactory();
		final Dialect dialect = factory.getJdbcServices().getDialect();

		// CLASS + TABLE

		joinSpan = persistentClass.getJoinClosureSpan() + 1;
		qualifiedTableNames = new String[joinSpan];
		isInverseTable = new boolean[joinSpan];
		isNullableTable = new boolean[joinSpan];
		keyColumnNames = new String[joinSpan][];
		final Table table = persistentClass.getRootTable();
		qualifiedTableNames[0] = determineTableName( table );

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

		int j = 1;
		for ( Join join : persistentClass.getJoinClosure() ) {
			qualifiedTableNames[j] = determineTableName( join.getTable() );
			isInverseTable[j] = join.isInverse();
			isNullableTable[j] = join.isOptional();
			cascadeDeleteEnabled[j] = join.getKey().isCascadeDeleteEnabled() && dialect.supportsCascadeDelete();

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

			keyColumnNames[j] = new String[join.getKey().getColumnSpan()];
			int i = 0;
			for ( Column col : join.getKey().getColumns() ) {
				keyColumnNames[j][i++] = col.getQuotedName( dialect );
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

//		final boolean lazyAvailable = isInstrumented();

		ArrayList<String> subclassTables = new ArrayList<>();
		ArrayList<String[]> joinKeyColumns = new ArrayList<>();
		ArrayList<Boolean> isConcretes = new ArrayList<>();
		ArrayList<Boolean> isClassOrSuperclassJoins = new ArrayList<>();
		ArrayList<Boolean> isDeferreds = new ArrayList<>();
		ArrayList<Boolean> isInverses = new ArrayList<>();
		ArrayList<Boolean> isNullables = new ArrayList<>();
//		ArrayList<Boolean> isLazies = new ArrayList<>();
		subclassTables.add( qualifiedTableNames[0] );
		joinKeyColumns.add( getIdentifierColumnNames() );
		isConcretes.add( Boolean.TRUE );
		isClassOrSuperclassJoins.add( Boolean.TRUE );
		isDeferreds.add( Boolean.FALSE );
		isInverses.add( Boolean.FALSE );
		isNullables.add( Boolean.FALSE );
//		isLazies.add( Boolean.FALSE );
		for ( Join join : persistentClass.getSubclassJoinClosure() ) {
			isConcretes.add( persistentClass.isClassOrSuperclassTable( join.getTable() ) );
			isClassOrSuperclassJoins.add( persistentClass.isClassOrSuperclassJoin( join ) );
			isInverses.add( join.isInverse() );
			isNullables.add( join.isOptional() );
//			isLazies.add( lazyAvailable && join.isLazy() );

			boolean isDeferred = join.isSequentialSelect() && !persistentClass.isClassOrSuperclassJoin( join ) ;
			isDeferreds.add( isDeferred );

			String joinTableName = determineTableName( join.getTable() );
			subclassTables.add( joinTableName );

			String[] keyCols = new String[join.getKey().getColumnSpan()];
			int i = 0;
			for ( Column col : join.getKey().getColumns() ) {
				keyCols[i++] = col.getQuotedName( dialect );
			}
			joinKeyColumns.add( keyCols );
		}

		subclassTableSequentialSelect = ArrayHelper.toBooleanArray( isDeferreds );
		subclassTableNameClosure = ArrayHelper.toStringArray( subclassTables );
//		subclassTableIsLazyClosure = ArrayHelper.toBooleanArray( isLazies );
		subclassTableKeyColumnClosure = ArrayHelper.to2DStringArray( joinKeyColumns );
		isClassOrSuperclassTable = ArrayHelper.toBooleanArray( isConcretes );
		isClassOrSuperclassJoin = ArrayHelper.toBooleanArray( isClassOrSuperclassJoins );
		isInverseSubclassTable = ArrayHelper.toBooleanArray( isInverses );
		isNullableSubclassTable = ArrayHelper.toBooleanArray( isNullables );

		// DISCRIMINATOR

		if ( persistentClass.isPolymorphic() ) {
			Value discrimValue = persistentClass.getDiscriminator();
			if ( discrimValue == null ) {
				throw new MappingException( "discriminator mapping required for single table polymorphic persistence" );
			}
			forceDiscriminator = persistentClass.isForceDiscriminator();
			Selectable selectable = discrimValue.getSelectables().get(0);
			SqmFunctionRegistry functionRegistry = factory.getQueryEngine().getSqmFunctionRegistry();
			if ( discrimValue.hasFormula() ) {
				Formula formula = (Formula) selectable;
//				discriminatorFormula = formula.getFormula();
				discriminatorFormulaTemplate = formula.getTemplate( dialect, functionRegistry );
				discriminatorColumnName = null;
				discriminatorColumnReaders = null;
				discriminatorColumnReaderTemplate = null;
				discriminatorAlias = "clazz_";
			}
			else {
				Column column = (Column) selectable;
				discriminatorColumnName = column.getQuotedName( dialect );
				discriminatorColumnReaders = column.getReadExpr( dialect );
				discriminatorColumnReaderTemplate = column.getTemplate( dialect, functionRegistry );
				discriminatorAlias = column.getAlias( dialect, persistentClass.getRootTable() );
//				discriminatorFormula = null;
				discriminatorFormulaTemplate = null;
			}
			discriminatorType = (BasicType<?>) persistentClass.getDiscriminator().getType();
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
					discriminatorValue = discriminatorType.getJavaTypeDescriptor()
							.fromString( persistentClass.getDiscriminatorValue() );
					JdbcLiteralFormatter literalFormatter = discriminatorType.getJdbcType()
							.getJdbcLiteralFormatter( discriminatorType.getJavaTypeDescriptor() );
					discriminatorSQLValue = literalFormatter.toJdbcLiteral(
							discriminatorValue,
							dialect,
							factory.getWrapperOptions()
					);
				}
				catch (ClassCastException cce) {
					throw new MappingException( "Illegal discriminator type: " + discriminatorType.getName() );
				}
				catch (Exception e) {
					throw new MappingException( "Could not format discriminator value to SQL string", e );
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
//			discriminatorFormula = null;
			discriminatorFormulaTemplate = null;
		}

		// PROPERTIES

		propertyTableNumbers = new int[getPropertySpan()];
		int i = 0;
		for ( Property property : persistentClass.getPropertyClosure() ) {
			propertyTableNumbers[i++] = persistentClass.getJoinNumber( property );
		}

		//TODO: code duplication with JoinedSubclassEntityPersister

//		ArrayList<Integer> columnJoinNumbers = new ArrayList<>();
//		ArrayList<Integer> formulaJoinedNumbers = new ArrayList<>();
		ArrayList<Integer> propertyJoinNumbers = new ArrayList<>();

//		final HashMap<String, Integer> propertyTableNumbersByNameAndSubclassLocal = new HashMap<>();
		final Map<Object, String> subclassesByDiscriminatorValueLocal = new HashMap<>();

		for ( Property property : persistentClass.getSubclassPropertyClosure() ) {
			Integer join = persistentClass.getJoinNumber( property );
			propertyJoinNumbers.add( join );

			//propertyTableNumbersByName.put( prop.getName(), join );
//			propertyTableNumbersByNameAndSubclassLocal.put(
//					property.getPersistentClass().getEntityName() + '.' + property.getName(),
//					join
//			);

//			for ( Selectable selectable : property.getSelectables() ) {
//				if ( selectable.isFormula() ) {
//					formulaJoinedNumbers.add( join );
//				}
//				else {
//					columnJoinNumbers.add( join );
//				}
//			}
		}

//		propertyTableNumbersByNameAndSubclass = CollectionHelper.toSmallMap( propertyTableNumbersByNameAndSubclassLocal );

//		subclassColumnTableNumberClosure = ArrayHelper.toIntArray( columnJoinNumbers );
//		subclassFormulaTableNumberClosure = ArrayHelper.toIntArray( formulaJoinedNumbers );
		subclassPropertyTableNumberClosure = ArrayHelper.toIntArray( propertyJoinNumbers );

		int subclassSpan = persistentClass.getSubclassSpan() + 1;
		subclassClosure = new String[subclassSpan];
		subclassClosure[0] = getEntityName();
		if ( persistentClass.isPolymorphic() ) {
			addSubclassByDiscriminatorValue(
					subclassesByDiscriminatorValueLocal,
					discriminatorValue,
					getEntityName()
			);
		}

		// SUBCLASSES
		if ( persistentClass.isPolymorphic() ) {
			int k = 1;
			for ( Subclass subclass : persistentClass.getSubclasses() ) {
				subclassClosure[k++] = subclass.getEntityName();
				if ( subclass.isDiscriminatorValueNull() ) {
					addSubclassByDiscriminatorValue(
							subclassesByDiscriminatorValueLocal,
							NULL_DISCRIMINATOR,
							subclass.getEntityName()
					);
				}
				else if ( subclass.isDiscriminatorValueNotNull() ) {
					addSubclassByDiscriminatorValue(
							subclassesByDiscriminatorValueLocal,
							NOT_NULL_DISCRIMINATOR,
							subclass.getEntityName()
					);
				}
				else {
					try {
						addSubclassByDiscriminatorValue(
								subclassesByDiscriminatorValueLocal,
								discriminatorType.getJavaTypeDescriptor()
										.fromString( subclass.getDiscriminatorValue() ),
								subclass.getEntityName()
						);
					}
					catch (ClassCastException cce) {
						throw new MappingException( "Illegal discriminator type: " + discriminatorType.getName() );
					}
					catch (Exception e) {
						throw new MappingException( "Error parsing discriminator value", e );
					}
				}
			}
		}

		// Don't hold a reference to an empty HashMap:
		subclassesByDiscriminatorValue = CollectionHelper.toSmallMap( subclassesByDiscriminatorValueLocal );

		initSubclassPropertyAliasesMap( persistentClass );

		postConstruct( creationContext.getMetadata() );

	}

	private static void addSubclassByDiscriminatorValue(Map<Object, String> subclassesByDiscriminatorValue, Object discriminatorValue, String entityName) {
		String mappedEntityName = subclassesByDiscriminatorValue.put( discriminatorValue, entityName );
		if ( mappedEntityName != null ) {
			throw new MappingException(
					"Entities [" + entityName + "] and [" + mappedEntityName
							+ "] are mapped with the same discriminator value '" + discriminatorValue + "'."
			);
		}
	}

	@Override
	public boolean isInverseTable(int j) {
		return isInverseTable[j];
	}

	@Override
	protected boolean isInverseSubclassTable(int j) {
		return isInverseSubclassTable[j];
	}

	@Override
	public String getDiscriminatorColumnName() {
		return discriminatorColumnName;
	}

	@Override
	public String getDiscriminatorColumnReaders() {
		return discriminatorColumnReaders;
	}

	@Override
	public String getDiscriminatorColumnReaderTemplate() {
		return discriminatorColumnReaderTemplate;
	}

	@Override
	public String getDiscriminatorAlias() {
		return discriminatorAlias;
	}

	@Override
	public String getDiscriminatorFormulaTemplate() {
		return discriminatorFormulaTemplate;
	}

	@Override
	public String getTableName() {
		return qualifiedTableNames[0];
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

	public String[] getSubclassClosure() {
		return subclassClosure;
	}

	@Override
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
	public Serializable[] getPropertySpaces() {
		return spaces;
	}

	private boolean isDiscriminatorFormula() {
		return discriminatorColumnName == null;
	}

	@Override
	public String getTableName(int j) {
		return qualifiedTableNames[j];
	}

	@Override
	public String[] getKeyColumns(int j) {
		return keyColumnNames[j];
	}

	@Override
	public boolean isTableCascadeDeleteEnabled(int j) {
		return cascadeDeleteEnabled[j];
	}

	@Override
	public boolean isPropertyOfTable(int property, int j) {
		return propertyTableNumbers[property] == j;
	}

	@Override
	protected boolean isSubclassTableSequentialSelect(int j) {
		return subclassTableSequentialSelect[j] && !isClassOrSuperclassTable[j];
	}

	// Execute the SQL:

	@Override
	public String fromTableFragment(String name) {
		return getTableName() + ' ' + name;
	}

	@Override
	protected String filterFragment(String alias) throws MappingException {
		if ( hasWhere() ) {
			return discriminatorFilterFragment( alias ) + " and " + getSQLWhereString( alias );
		}
		else {
			return "";
		}
	}

	private String discriminatorFilterFragment(String alias) throws MappingException {
		return discriminatorFilterFragment( alias, null );
	}

	@Override
	protected String filterFragment(String alias, Set<String> treatAsDeclarations) {
		if ( hasWhere() ) {
			final String discriminatorFilterFragment = discriminatorFilterFragment( alias, treatAsDeclarations );
			if ( StringHelper.isNotEmpty( discriminatorFilterFragment ) ) {
				return discriminatorFilterFragment + " and " + getSQLWhereString( alias );
			}
			return getSQLWhereString( alias );
		}
		else {
			return "";
		}
	}

	private String discriminatorFilterFragment(String alias, Set<String> treatAsDeclarations) {
		final boolean hasTreatAs = treatAsDeclarations != null && !treatAsDeclarations.isEmpty();

		if ( !needsDiscriminator() && !hasTreatAs ) {
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
			frag.addValues( fullDiscriminatorSQLValues() );
		}

		return frag.toFragmentString();
	}

	private boolean needsDiscriminator() {
		return forceDiscriminator || isInherited();
	}

	private String[] decodeTreatAsRequests(Set<String> treatAsDeclarations) {
		final List<String> values = new ArrayList<>();
		for ( String subclass : treatAsDeclarations ) {
			final Queryable queryable = (Queryable) getFactory()
					.getRuntimeMetamodels()
					.getMappingMetamodel()
					.getEntityDescriptor( subclass );
			if ( !queryable.isAbstract() ) {
				values.add( queryable.getDiscriminatorSQLValue() );
			}
			if ( queryable.hasSubclasses() ) {
				// if the treat is an abstract class, add the concrete implementations to values if any
				Set<String> actualSubClasses = queryable.getEntityMetamodel().getSubclassEntityNames();

				for ( String actualSubClass : actualSubClasses ) {
					if ( actualSubClass.equals( subclass ) ) {
						continue;
					}

					final Queryable actualQueryable = (Queryable) getFactory()
							.getRuntimeMetamodels()
							.getMappingMetamodel()
							.getEntityDescriptor( actualSubClass );
					if ( !actualQueryable.hasSubclasses() ) {
						values.add( actualQueryable.getDiscriminatorSQLValue() );
					}
				}
			}
		}
		return ArrayHelper.toStringArray( values );
	}

	private String[] fullDiscriminatorSQLValues;

	private String[] fullDiscriminatorSQLValues() {
		String[] fullDiscriminatorSQLValues = this.fullDiscriminatorSQLValues;
		if ( fullDiscriminatorSQLValues == null ) {
			// first access; build it
			final List<String> values = new ArrayList<>();
			for ( String subclass : getSubclassClosure() ) {
				final Queryable queryable = (Queryable) getFactory().getRuntimeMetamodels()
						.getMappingMetamodel()
						.getEntityDescriptor( subclass );
				if ( !queryable.isAbstract() ) {
					values.add( queryable.getDiscriminatorSQLValue() );
				}
			}
			this.fullDiscriminatorSQLValues = fullDiscriminatorSQLValues = ArrayHelper.toStringArray( values );
		}

		return fullDiscriminatorSQLValues;
	}

	private Object[] fullDiscriminatorValues;

	private Object[] fullDiscriminatorValues() {
		Object[] fullDiscriminatorValues = this.fullDiscriminatorValues;
		if ( fullDiscriminatorValues == null ) {
			// first access; build it
			final List<Object> values = new ArrayList<>();
			for ( String subclass : getSubclassClosure() ) {
				final Queryable queryable = (Queryable) getFactory().getRuntimeMetamodels()
						.getMappingMetamodel()
						.getEntityDescriptor( subclass );
				if ( !queryable.isAbstract() ) {
					values.add( queryable.getDiscriminatorValue() );
				}
			}
			this.fullDiscriminatorValues = fullDiscriminatorValues = values.toArray(new Object[0]);
		}

		return fullDiscriminatorValues;
	}

	@Override
	public String getSubclassPropertyTableName(int i) {
		return subclassTableNameClosure[subclassPropertyTableNumberClosure[i]];
	}

	@Override
	protected int getSubclassPropertyTableNumber(int i) {
		return subclassPropertyTableNumberClosure[i];
	}

	@Override
	public int getTableSpan() {
		return joinSpan;
	}

	@Override
	protected void addDiscriminatorToInsert(Insert insert) {

		if ( discriminatorInsertable ) {
			insert.addColumn( getDiscriminatorColumnName(), discriminatorSQLValue );
		}

	}

	@Override
	protected int[] getPropertyTableNumbers() {
		return propertyTableNumbers;
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
	protected boolean isClassOrSuperclassTable(int j) {
		return isClassOrSuperclassTable[j];
	}

	@Override
	protected boolean isClassOrSuperclassJoin(int j) {
		return isClassOrSuperclassJoin[j];
	}

	@Override
	public boolean isNullableTable(int j) {
		return isNullableTable[j];
	}

	@Override
	protected boolean isNullableSubclassTable(int j) {
		return isNullableSubclassTable[j];
	}

	@Override
	public String getPropertyTableName(String propertyName) {
		Integer index = getEntityMetamodel().getPropertyIndexOrNull( propertyName );
		return index == null ? null : qualifiedTableNames[propertyTableNumbers[index]];
	}

	@Override @Deprecated
	public boolean isMultiTable() {
		return getTableSpan() > 1;
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
		return new DynamicFilterAliasGenerator( qualifiedTableNames, rootAlias );
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
		final TableGroup tableGroup = super.createRootTableGroup(
				canUseInnerJoins,
				navigablePath,
				explicitSourceAlias,
				additionalPredicateCollectorAccess,
				sqlAliasBase,
				expressionResolver,
				fromClauseAccess,
				creationContext
		);

		if ( additionalPredicateCollectorAccess != null && needsDiscriminator() ) {
			final Predicate discriminatorPredicate = createDiscriminatorPredicate(
					tableGroup.getPrimaryTableReference().getIdentificationVariable(),
					tableGroup,
					expressionResolver
			);
			additionalPredicateCollectorAccess.get().accept( discriminatorPredicate );
		}

		return tableGroup;
	}

	@Override
	public void applyDiscriminator(
			Consumer<Predicate> predicateConsumer,
			String alias,
			TableGroup tableGroup,
			SqlAstCreationState creationState) {
		if ( needsDiscriminator() ) {
			predicateConsumer.accept(
					createDiscriminatorPredicate(
							alias,
							tableGroup,
							creationState.getSqlExpressionResolver()
					)
			);
		}
		super.applyDiscriminator( predicateConsumer, alias, tableGroup, creationState );
	}

	private Predicate createDiscriminatorPredicate(
			String alias,
			TableGroup tableGroup,
			SqlExpressionResolver sqlExpressionResolver) {
		final String columnReferenceKey;
		final String discriminatorExpression;
		if ( isDiscriminatorFormula() ) {
			discriminatorExpression = getDiscriminatorFormulaTemplate();
			columnReferenceKey = SqlExpressionResolver.createColumnReferenceKey(
					tableGroup.getPrimaryTableReference(),
					getDiscriminatorFormulaTemplate()
			);
		}
		else {
			discriminatorExpression = getDiscriminatorColumnName();
			columnReferenceKey = SqlExpressionResolver.createColumnReferenceKey(
					tableGroup.getPrimaryTableReference(),
					getDiscriminatorColumnName()
			);
		}

		final BasicType<?> discriminatorType = (BasicType<?>) getDiscriminatorType();
		final Expression sqlExpression = sqlExpressionResolver.resolveSqlExpression(
				columnReferenceKey,
				sqlAstProcessingState -> new ColumnReference(
						alias,
						discriminatorExpression,
						isDiscriminatorFormula(),
						null,
						null,
						discriminatorType.getJdbcMapping(),
						getFactory()
				)
		);

		if ( hasSubclasses() ) {
			final Object[] discriminatorValues = fullDiscriminatorValues();
			final List<Expression> values = new ArrayList<>( discriminatorValues.length );
			boolean hasNull = false, hasNonNull = false;
			for ( Object discriminatorValue : discriminatorValues ) {
				if ( discriminatorValue == NULL_DISCRIMINATOR ) {
					hasNull = true;
				}
				else if ( discriminatorValue == NOT_NULL_DISCRIMINATOR ) {
					hasNonNull = true;
				}
				else {
					values.add( new QueryLiteral<>( discriminatorValue, discriminatorType ) );
				}
			}
			final Predicate p = new InListPredicate( sqlExpression, values );
			if ( hasNull || hasNonNull ) {
				final Junction junction = new Junction(
						Junction.Nature.DISJUNCTION
				);

				// This essentially means we need to select everything, so we don't need a predicate at all
				// so we return an empty Junction
				if ( hasNull && hasNonNull ) {
					return junction;
				}

				junction.add( new NullnessPredicate( sqlExpression ) );
				junction.add( p );
				return junction;
			}
			return p;
		}

		final Object value = getDiscriminatorValue();
		final boolean hasNotNullDiscriminator = value == NOT_NULL_DISCRIMINATOR;
		final boolean hasNullDiscriminator = value == NULL_DISCRIMINATOR;
		if ( hasNotNullDiscriminator || hasNullDiscriminator ) {
			final NullnessPredicate nullnessPredicate = new NullnessPredicate( sqlExpression );
			if ( hasNotNullDiscriminator ) {
				return new NegatedPredicate( nullnessPredicate );
			}

			return nullnessPredicate;
		}
		return new ComparisonPredicate(
				sqlExpression,
				ComparisonOperator.EQUAL,
				new QueryLiteral<>( value, discriminatorType )
		);
	}

	@Override
	public void pruneForSubclasses(TableGroup tableGroup, Set<String> treatedEntityNames) {
		// If the base type is part of the treatedEntityNames this means we can't optimize this,
		// as the table group is e.g. returned through a select
		if ( treatedEntityNames.contains( getEntityName() ) ) {
			return;
		}
		// The optimization is to simply add the discriminator filter fragment for all treated entity names
		final NamedTableReference tableReference = (NamedTableReference) tableGroup.getPrimaryTableReference();
		tableReference.setPrunedTableExpression(
				"(select * from " + getTableName() + " t where " + discriminatorFilterFragment( "t", treatedEntityNames ) + ")"
		);
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
}
