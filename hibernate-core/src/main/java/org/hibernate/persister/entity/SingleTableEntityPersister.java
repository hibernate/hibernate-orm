/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.Internal;
import org.hibernate.MappingException;
import org.hibernate.Remove;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.NaturalIdDataAccess;
import org.hibernate.internal.DynamicFilterAliasGenerator;
import org.hibernate.internal.FilterAliasGenerator;
import org.hibernate.jdbc.Expectation;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Formula;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.metamodel.mapping.TableDetails;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.model.ast.builder.MutationGroupBuilder;
import org.hibernate.sql.model.ast.builder.TableInsertBuilder;
import org.hibernate.type.BasicType;

import static org.hibernate.internal.util.collections.ArrayHelper.indexOf;
import static org.hibernate.internal.util.collections.ArrayHelper.join;
import static org.hibernate.internal.util.collections.ArrayHelper.to2DStringArray;
import static org.hibernate.internal.util.collections.ArrayHelper.toBooleanArray;
import static org.hibernate.internal.util.collections.ArrayHelper.toIntArray;
import static org.hibernate.internal.util.collections.ArrayHelper.toStringArray;
import static org.hibernate.internal.util.collections.CollectionHelper.toSmallMap;
import static org.hibernate.jdbc.Expectations.createExpectation;
import static org.hibernate.persister.entity.DiscriminatorHelper.NULL_DISCRIMINATOR;
import static org.hibernate.sql.model.ast.builder.TableMutationBuilder.NULL;

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
@Internal
public class SingleTableEntityPersister extends AbstractEntityPersister {

	// the class hierarchy structure
	private final int joinSpan;
	private final boolean hasDuplicateTables;

	/**
	 * todo (6.2) - this assumes duplicates are included which we are trying to do away wi
	 */
	private final String[] qualifiedTableNames;

	private final boolean[] isInverseTable;
	private final boolean[] isNullableTable;
	private final String[][] keyColumnNames;
	private final boolean[] cascadeDeleteEnabled;

	private final String[] spaces;

	private final String[] subclassClosure;

	private final String[] subclassTableNameClosure;
	private final boolean[] isNullableSubclassTable;
	private final String[][] subclassTableKeyColumnClosure;
	private final boolean[] isClassOrSuperclassTable;
	private final boolean[] isClassOrSuperclassJoin;

	// properties of this class, including inherited properties
	private final int[] propertyTableNumbers;

	// the closure of all columns used by the entire hierarchy including
	// subclasses and superclasses of this class
	private final int[] subclassPropertyTableNumberClosure;

	// discriminator column
	private final Map<Object, String> subclassesByDiscriminatorValue;
	private final boolean forceDiscriminator;
	private final String discriminatorColumnName;
	private final String discriminatorColumnReaders;
	private final String discriminatorColumnReaderTemplate;
	private final String discriminatorFormulaTemplate;
	private final BasicType<?> discriminatorType;
	private final Object discriminatorValue;
	private final String discriminatorSQLValue;
	private final boolean discriminatorInsertable;

	private final String[] constraintOrderedTableNames;
	private final String[][] constraintOrderedKeyColumnNames;

	public SingleTableEntityPersister(
			final PersistentClass persistentClass,
			final EntityDataAccess cacheAccessStrategy,
			final NaturalIdDataAccess naturalIdRegionAccessStrategy,
			final RuntimeModelCreationContext creationContext) throws HibernateException {
		super( persistentClass, cacheAccessStrategy, naturalIdRegionAccessStrategy, creationContext );

		final var dialect = creationContext.getDialect();
		final var typeConfiguration = creationContext.getTypeConfiguration();

		// CLASS + TABLE

		joinSpan = persistentClass.getJoinClosureSpan() + 1;
		// todo (6.2) : see note on AbstractEntityPersister#getTableName(int)
		qualifiedTableNames = new String[joinSpan];

		qualifiedTableNames[0] = determineTableName( persistentClass.getRootTable() );

		isInverseTable = new boolean[joinSpan];
		isNullableTable = new boolean[joinSpan];
		keyColumnNames = new String[joinSpan][];

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

		insertExpectations = new Expectation[joinSpan];
		updateExpectations = new Expectation[joinSpan];
		deleteExpectations = new Expectation[joinSpan];

		customSQLInsert[0] = persistentClass.getCustomSQLInsert();
		insertCallable[0] = persistentClass.isCustomInsertCallable();
		insertExpectations[0] = createExpectation( persistentClass.getInsertExpectation(), insertCallable[0] );

		customSQLUpdate[0] = persistentClass.getCustomSQLUpdate();
		updateCallable[0] = persistentClass.isCustomUpdateCallable();
		updateExpectations[0] = createExpectation( persistentClass.getUpdateExpectation(), updateCallable[0] );

		customSQLDelete[0] = persistentClass.getCustomSQLDelete();
		deleteCallable[0] = persistentClass.isCustomDeleteCallable();
		deleteExpectations[0] = createExpectation( persistentClass.getDeleteExpectation(), deleteCallable[0] );

		// JOINS

		final var joinClosure = persistentClass.getJoinClosure();
		boolean hasDuplicateTableName = false;
		for ( int j = 1; j - 1 < joinClosure.size(); j++ ) {
			final var join = joinClosure.get( j - 1 );
			qualifiedTableNames[j] = determineTableName( join.getTable() );
			hasDuplicateTableName = hasDuplicateTableName
					|| indexOf( qualifiedTableNames, j, qualifiedTableNames[j] ) != -1;
			isInverseTable[j] = join.isInverse();
			isNullableTable[j] = join.isOptional();
			cascadeDeleteEnabled[j] = join.getKey().isCascadeDeleteEnabled() && dialect.supportsCascadeDelete();

			customSQLInsert[j] = join.getCustomSQLInsert();
			insertCallable[j] = join.isCustomInsertCallable();
			insertExpectations[j] = createExpectation( join.getInsertExpectation(), insertCallable[j] );

			customSQLUpdate[j] = join.getCustomSQLUpdate();
			updateCallable[j] = join.isCustomUpdateCallable();
			updateExpectations[j] = createExpectation( join.getUpdateExpectation(), updateCallable[j] );

			customSQLDelete[j] = join.getCustomSQLDelete();
			deleteCallable[j] = join.isCustomDeleteCallable();
			deleteExpectations[j] = createExpectation( join.getDeleteExpectation(), deleteCallable[j] );

			keyColumnNames[j] = new String[join.getKey().getColumnSpan()];

			final var columns = join.getKey().getColumns();
			for ( int i = 0; i < columns.size(); i++ ) {
				keyColumnNames[j][i] = columns.get( i ).getQuotedName( dialect );
			}
		}

		hasDuplicateTables = hasDuplicateTableName;
		constraintOrderedTableNames = new String[qualifiedTableNames.length];
		constraintOrderedKeyColumnNames = new String[qualifiedTableNames.length][];
		for ( int i = qualifiedTableNames.length - 1, position = 0; i >= 0; i--, position++ ) {
			constraintOrderedTableNames[position] = qualifiedTableNames[i];
			constraintOrderedKeyColumnNames[position] = keyColumnNames[i];
		}

		spaces = join( qualifiedTableNames, toStringArray( persistentClass.getSynchronizedTables() ) );

		final ArrayList<String> subclassTables = new ArrayList<>();
		final ArrayList<String[]> joinKeyColumns = new ArrayList<>();
		final ArrayList<Boolean> isConcretes = new ArrayList<>();
		final ArrayList<Boolean> isClassOrSuperclassJoins = new ArrayList<>();
		final ArrayList<Boolean> isNullables = new ArrayList<>();
		subclassTables.add( qualifiedTableNames[0] );
		joinKeyColumns.add( getIdentifierColumnNames() );
		isConcretes.add( true );
		isClassOrSuperclassJoins.add( true );
		isNullables.add( false );
		for ( var join : persistentClass.getSubclassJoinClosure() ) {
			isConcretes.add( persistentClass.isClassOrSuperclassTable( join.getTable() ) );
			isClassOrSuperclassJoins.add( persistentClass.isClassOrSuperclassJoin( join ) );
			isNullables.add( join.isOptional() );

			subclassTables.add( determineTableName( join.getTable() ) );

			final String[] keyCols = new String[join.getKey().getColumnSpan()];
			final var columns = join.getKey().getColumns();
			for ( int i = 0; i < columns.size(); i++ ) {
				keyCols[i] = columns.get( i ).getQuotedName( dialect );
			}
			joinKeyColumns.add( keyCols );
		}

		subclassTableNameClosure = toStringArray( subclassTables );
		subclassTableKeyColumnClosure = to2DStringArray( joinKeyColumns );
		isClassOrSuperclassTable = toBooleanArray( isConcretes );
		isClassOrSuperclassJoin = toBooleanArray( isClassOrSuperclassJoins );
		isNullableSubclassTable = toBooleanArray( isNullables );

		// DISCRIMINATOR

		if ( persistentClass.isPolymorphic() ) {
			final var discriminator = persistentClass.getDiscriminator();
			if ( discriminator == null ) {
				throw new MappingException( "discriminator mapping required for single table polymorphic persistence" );
			}
			forceDiscriminator = persistentClass.isForceDiscriminator();
			final var selectable = discriminator.getSelectables().get( 0 );
			discriminatorType = DiscriminatorHelper.getDiscriminatorType( persistentClass );
			discriminatorValue = DiscriminatorHelper.getDiscriminatorValue( persistentClass );
			discriminatorSQLValue = DiscriminatorHelper.getDiscriminatorSQLValue( persistentClass, dialect );
			discriminatorInsertable = isDiscriminatorInsertable( persistentClass );
			if ( selectable instanceof Formula formula ) {
				discriminatorFormulaTemplate = formula.getTemplate( dialect, typeConfiguration );
				discriminatorColumnName = null;
				discriminatorColumnReaders = null;
				discriminatorColumnReaderTemplate = null;
				discriminatorAlias = "clazz_";
			}
			else if ( selectable instanceof Column column ) {
				discriminatorColumnName = column.getQuotedName( dialect );
				discriminatorColumnReaders = column.getReadExpr( dialect );
				discriminatorColumnReaderTemplate = column.getTemplate( dialect, typeConfiguration );
				discriminatorAlias = column.getAlias( dialect, persistentClass.getRootTable() );
				discriminatorFormulaTemplate = null;
			}
			else {
				throw new AssertionFailure( "Unrecognized selectable" );
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
			discriminatorFormulaTemplate = null;
		}

		// PROPERTIES

		propertyTableNumbers = new int[getPropertySpan()];
		final var propertyClosure = persistentClass.getPropertyClosure();
		for ( int k = 0; k < propertyClosure.size(); k++ ) {
			propertyTableNumbers[k] = persistentClass.getJoinNumber( propertyClosure.get( k ) );
		}

		//TODO: code duplication with JoinedSubclassEntityPersister

		final ArrayList<Integer> propertyJoinNumbers = new ArrayList<>();
		final Map<Object, String> subclassesByDiscriminatorValueLocal = new HashMap<>();

		for ( var property : persistentClass.getSubclassPropertyClosure() ) {
			propertyJoinNumbers.add( persistentClass.getJoinNumber( property ) );
		}

		subclassPropertyTableNumberClosure = toIntArray( propertyJoinNumbers );

		final int subclassSpan = persistentClass.getSubclassSpan() + 1;
		subclassClosure = new String[subclassSpan];
		subclassClosure[0] = getEntityName();
		if ( persistentClass.isPolymorphic() ) {
			addSubclassByDiscriminatorValue(
					subclassesByDiscriminatorValueLocal,
					discriminatorValue,
					getEntityName()
			);

			// SUBCLASSES
			final var subclasses = persistentClass.getSubclasses();
			for ( int k = 0; k < subclasses.size(); k++ ) {
				final var subclass = subclasses.get( k );
				subclassClosure[k] = subclass.getEntityName();
				addSubclassByDiscriminatorValue(
						subclassesByDiscriminatorValueLocal,
						DiscriminatorHelper.getDiscriminatorValue( subclass ),
						subclass.getEntityName()
				);
			}
		}

		// Don't hold a reference to an empty HashMap:
		subclassesByDiscriminatorValue = toSmallMap( subclassesByDiscriminatorValueLocal );

		initSubclassPropertyAliasesMap( persistentClass );

		postConstruct( creationContext.getMetadata() );
	}

	private static boolean isDiscriminatorInsertable(PersistentClass persistentClass) {
		return !persistentClass.isDiscriminatorValueNull()
			&& !persistentClass.isDiscriminatorValueNotNull()
			&& persistentClass.isDiscriminatorInsertable()
			&& !persistentClass.getDiscriminator().hasFormula();
	}

	private static void addSubclassByDiscriminatorValue(
			Map<Object, String> subclassesByDiscriminatorValue,
			Object discriminatorValue,
			String entityName) {
		final String mappedEntityName = subclassesByDiscriminatorValue.put( discriminatorValue, entityName );
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
	public String getDiscriminatorFormulaTemplate() {
		return discriminatorFormulaTemplate;
	}

	@Override
	public String getTableName() {
		return qualifiedTableNames[0];
	}

	@Override
	public BasicType<?> getDiscriminatorType() {
		return discriminatorType;
	}

	@Override
	public Map<Object, String> getSubclassByDiscriminatorValue() {
		return subclassesByDiscriminatorValue;
	}

	@Override
	public TableDetails getMappedTableDetails() {
		return getTableMapping( 0 );
	}

	@Override
	public TableDetails getIdentifierTableDetails() {
		return getTableMapping( 0 );
	}

	@Override
	public Object getDiscriminatorValue() {
		return discriminatorValue;
	}

	@Override
	public String getDiscriminatorSQLValue() {
		return discriminatorSQLValue;
	}

	/**
	 * @deprecated No longer used.
	 */
	@Deprecated(forRemoval = true)
	@Remove
	public String[] getSubclassClosure() {
		return subclassClosure;
	}

	@Override
	public String[] getPropertySpaces() {
		return spaces;
	}

	@Override
	protected boolean isDiscriminatorFormula() {
		return discriminatorColumnName == null;
	}

	@Override
	public boolean hasDuplicateTables() {
		return hasDuplicateTables;
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

//	@Override
//	protected boolean isSubclassTableSequentialSelect(int j) {
//		return subclassTableSequentialSelect[j] && !isClassOrSuperclassTable[j];
//	}

	// Execute the SQL:

	@Override
	public boolean needsDiscriminator() {
		return forceDiscriminator || isInherited();
	}

	public String getAttributeMutationTableName(int i) {
		return subclassTableNameClosure[subclassPropertyTableNumberClosure[i]];
	}

	@Override
	public int getTableSpan() {
		return joinSpan;
	}

	@Override
	public void addDiscriminatorToInsertGroup(MutationGroupBuilder insertGroupBuilder) {
		if ( discriminatorInsertable ) {
			final TableInsertBuilder tableInsertBuilder =
					insertGroupBuilder.getTableDetailsBuilder( getRootTableName() );
			tableInsertBuilder.addValueColumn(
					discriminatorValue == NULL_DISCRIMINATOR ? NULL : discriminatorSQLValue,
					getDiscriminatorMapping()
			);
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
	protected boolean isIdentifierTable(String tableExpression) {
		return tableExpression.equals( getRootTableName() );
	}

	@Override
	protected boolean isNullableSubclassTable(int j) {
		return isNullableSubclassTable[j];
	}

	@Override
	public boolean hasMultipleTables() {
		return getTableSpan() > 1;
	}

	@Override
	public String[] getConstraintOrderedTableNameClosure() {
		return constraintOrderedTableNames;
	}

	@Override
	public String[][] getConstraintOrderedTableKeyColumnClosure() {
		return constraintOrderedKeyColumnNames;
	}

	@Override
	public FilterAliasGenerator getFilterAliasGenerator(String rootAlias) {
		return new DynamicFilterAliasGenerator( qualifiedTableNames, rootAlias );
	}

	@Override
	public void pruneForSubclasses(TableGroup tableGroup, Map<String, EntityNameUse> entityNameUses) {
		if ( needsDiscriminator() || !entityNameUses.isEmpty() ) {// The following optimization is to add the discriminator filter fragment for all treated entity names
			final var mappingMetamodel = getFactory().getMappingMetamodel();
			if ( containsTreatUse( entityNameUses, mappingMetamodel ) ) {
				final String discriminatorPredicate =
						getPrunedDiscriminatorPredicate( entityNameUses, mappingMetamodel, "t" );
				if ( discriminatorPredicate != null ) {
					final var tableReference = (NamedTableReference) tableGroup.getPrimaryTableReference();
					tableReference.setPrunedTableExpression(
							"(select * from " + getTableName() + " t where " + discriminatorPredicate + ")" );
				}
			}
			// Otherwise, if we only have FILTER uses, we don't have to do anything here because
			// the BaseSqmToSqlAstConverter will already apply the type filter in the WHERE clause
		}
	}

	private boolean containsTreatUse(Map<String, EntityNameUse> entityNameUses, MappingMetamodel metamodel) {
		for ( var entry : entityNameUses.entrySet() ) {
			// We only care about treat uses which allow to reduce the amount of rows to select
			if ( entry.getValue().getKind() == EntityNameUse.UseKind.TREAT ) {
				final var persister = metamodel.getEntityDescriptor( entry.getKey() );
				// Filtering for abstract entities makes no sense, so ignore that
				if ( !persister.isAbstract() ) {
					// Also, it makes no sense to filter for any of the super types,
					// as the query will contain a filter for that already anyway
					final var superMappingType = getSuperMappingType();
					if ( superMappingType == null || !getSuperMappingType().isTypeOrSuperType( persister ) ) {
						return true;
					}
				}
			}
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

	@Override
	protected void visitMutabilityOrderedTables(MutabilityOrderedTableConsumer consumer) {
		for ( int i = 0; i < qualifiedTableNames.length; i++ ) {
			final String tableName = qualifiedTableNames[i];
			final int tableIndex = i;
			consumer.consume(
					tableName,
					tableIndex,
					() -> columnConsumer -> columnConsumer.accept(
							getIdentifierMapping(),
							tableName,
							keyColumnNames[tableIndex]
					)
			);
		}
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Deprecations
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Deprecated private final String discriminatorAlias;

	@Override
	public String getDiscriminatorAlias() {
		return discriminatorAlias;
	}
}
