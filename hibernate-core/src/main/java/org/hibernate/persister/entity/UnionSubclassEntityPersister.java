/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.Internal;
import org.hibernate.MappingException;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.NaturalIdDataAccess;
import org.hibernate.dialect.Dialect;
import org.hibernate.id.IdentityGenerator;
import org.hibernate.internal.FilterAliasGenerator;
import org.hibernate.internal.StaticFilterAliasGenerator;
import org.hibernate.internal.util.collections.JoinedList;
import org.hibernate.jdbc.Expectation;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metamodel.mapping.EntityDiscriminatorMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.TableDetails;
import org.hibernate.metamodel.mapping.internal.SqlTypedMappingImpl;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.SqlAliasBase;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.UnionTableGroup;
import org.hibernate.sql.ast.tree.from.UnionTableReference;
import org.hibernate.sql.ast.tree.from.UnknownTableReferenceException;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.type.BasicType;
import org.hibernate.type.StandardBasicTypes;

import static java.util.Collections.addAll;
import static java.util.Collections.unmodifiableList;
import static org.hibernate.internal.util.collections.ArrayHelper.to2DStringArray;
import static org.hibernate.internal.util.collections.ArrayHelper.toStringArray;
import static org.hibernate.jdbc.Expectations.createExpectation;

/**
 * An {@link EntityPersister} implementing the
 * {@link jakarta.persistence.InheritanceType#TABLE_PER_CLASS}
 * mapping strategy for an entity and its inheritance hierarchy.
 * <p>
 * This is implemented as a separate table for each concrete class,
 * with all inherited attributes persisted as columns of that table.
 *
 * @author Gavin King
 */
@Internal
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

	public UnionSubclassEntityPersister(
			final PersistentClass persistentClass,
			final EntityDataAccess cacheAccessStrategy,
			final NaturalIdDataAccess naturalIdRegionAccessStrategy,
			final RuntimeModelCreationContext creationContext) throws HibernateException {
		super( persistentClass, cacheAccessStrategy, naturalIdRegionAccessStrategy, creationContext );

		validateGenerator();

		final var dialect = creationContext.getDialect();

		// TABLE

		tableName = determineTableName( persistentClass.getTable() );
		subclassTableNames = new String[]{tableName};
		//Custom SQL

		customSQLInsert = new String[] { persistentClass.getCustomSQLInsert() };
		insertCallable = new boolean[] { persistentClass.isCustomInsertCallable() };
		insertExpectations = new Expectation[] { createExpectation( persistentClass.getInsertExpectation(),
				persistentClass.isCustomInsertCallable() ) };

		customSQLUpdate = new String[] { persistentClass.getCustomSQLUpdate() };
		updateCallable = new boolean[] { persistentClass.isCustomUpdateCallable() };
		updateExpectations = new Expectation[] { createExpectation( persistentClass.getUpdateExpectation(),
				persistentClass.isCustomUpdateCallable() ) };

		customSQLDelete = new String[] { persistentClass.getCustomSQLDelete() };
		deleteCallable = new boolean[] { persistentClass.isCustomDeleteCallable() };
		deleteExpectations = new Expectation[] { createExpectation( persistentClass.getDeleteExpectation(),
				persistentClass.isCustomDeleteCallable() ) };

		discriminatorValue = persistentClass.getSubclassId();
		discriminatorSQLValue = String.valueOf( persistentClass.getSubclassId() );
		discriminatorType = creationContext.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.INTEGER );

		// PROPERTIES

		// SUBCLASSES
		subclassByDiscriminatorValue.put( persistentClass.getSubclassId(), persistentClass.getEntityName() );
		if ( persistentClass.isPolymorphic() ) {
			for ( var subclass : persistentClass.getSubclasses() ) {
				subclassByDiscriminatorValue.put( subclass.getSubclassId(), subclass.getEntityName() );
			}
		}

		//SPACES
		//TODO: I'm not sure, but perhaps we should exclude
		//      abstract denormalized tables?

		final int spacesSize = 1 + persistentClass.getSynchronizedTables().size();
		spaces = new String[spacesSize];
		spaces[0] = tableName;
		final var iter = persistentClass.getSynchronizedTables().iterator();
		for ( int i = 1; i < spacesSize; i++ ) {
			spaces[i] = iter.next();
		}

		final HashSet<String> subclassTables = new HashSet<>();
		for ( var table : persistentClass.getSubclassTableClosure() ) {
			subclassTables.add( determineTableName( table ) );
		}
		subclassSpaces = toStringArray( subclassTables );

		subquery = generateSubquery( persistentClass );
		final List<String> tableExpressions = new ArrayList<>( subclassSpaces.length * 2 );
		addAll( tableExpressions, subclassSpaces );
		tableExpressions.add( subquery );
		var parentPersistentClass = persistentClass.getSuperclass();
		while ( parentPersistentClass != null ) {
			tableExpressions.add( generateSubquery( parentPersistentClass ) );
			parentPersistentClass = parentPersistentClass.getSuperclass();
		}
		for ( var subclassPersistentClass : persistentClass.getSubclassClosure() ) {
			if ( subclassPersistentClass.hasSubclasses() ) {
				tableExpressions.add( generateSubquery( subclassPersistentClass ) );
			}
		}
		subclassTableExpressions = toStringArray( tableExpressions );

		if ( hasMultipleTables() ) {
			final int idColumnSpan = getIdentifierColumnSpan();
			final ArrayList<String> tableNames = new ArrayList<>();
			final ArrayList<String[]> keyColumns = new ArrayList<>();
			for ( var table : persistentClass.getSubclassTableClosure() ) {
				if ( !table.isAbstractUnionTable() ) {
					tableNames.add( determineTableName( table ) );
					final String[] key = new String[idColumnSpan];
					final List<Column> columns = table.getPrimaryKey().getColumnsInOriginalOrder();
					for ( int k = 0; k < idColumnSpan; k++ ) {
						key[k] = columns.get(k).getQuotedName( dialect );
					}
					keyColumns.add( key );
				}
			}

			constraintOrderedTableNames = toStringArray( tableNames );
			constraintOrderedKeyColumnNames = to2DStringArray( keyColumns );
		}
		else {
			constraintOrderedTableNames = new String[] { tableName };
			constraintOrderedKeyColumnNames = new String[][] { getIdentifierColumnNames() };
		}

		initSubclassPropertyAliasesMap( persistentClass );

		postConstruct( creationContext.getMetadata() );
	}

	protected void validateGenerator() {
		if ( getGenerator() instanceof IdentityGenerator ) {
			throw new MappingException( "Cannot use identity column key generation with <union-subclass> mapping for: " + getEntityName() );
		}
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
	public UnionTableReference createPrimaryTableReference(
			SqlAliasBase sqlAliasBase,
			SqlAstCreationState creationState) {
		return new UnionTableReference(
				getTableName(),
				subclassTableExpressions,
				SqlAliasBase.from(
						sqlAliasBase,
						null,
						this,
						creationState.getSqlAliasBaseGenerator()
				).generateNewAlias()
		);
	}

	@Override
	public TableGroup createRootTableGroup(
			boolean canUseInnerJoins,
			NavigablePath navigablePath,
			String explicitSourceAlias,
			SqlAliasBase sqlAliasBase,
			Supplier<Consumer<Predicate>> additionalPredicateCollectorAccess,
			SqlAstCreationState creationState) {
		return new UnionTableGroup(
				canUseInnerJoins,
				navigablePath,
				createPrimaryTableReference( sqlAliasBase, creationState ),
				this,
				explicitSourceAlias
		);
	}

	@Override
	public boolean needsDiscriminator() {
		return false;
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
		return hasSubclasses() ? subquery : tableName;
	}

	@Override
	public BasicType<?> getDiscriminatorType() {
		return discriminatorType;
	}

	@Override
	public Map<Object, String> getSubclassByDiscriminatorValue() {
		return subclassByDiscriminatorValue;
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

	@Override
	public String[] getPropertySpaces() {
		return spaces;
	}

	@Override
	protected boolean shouldProcessSuperMapping() {
		return false;
	}

	@Override
	public boolean hasDuplicateTables() {
		return false;
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
	public String getAttributeMutationTableName(int i) {
		return getTableName();//ie. the subquery! yuck!
	}

	@Override
	public String physicalTableNameForMutation(SelectableMapping selectableMapping) {
		assert !selectableMapping.isFormula();
		return tableName;
	}

	@Override
	protected boolean isIdentifierTable(String tableExpression) {
		return tableExpression.equals( getRootTableName() );
	}

	@Override
	public boolean hasMultipleTables() {
		// This could also just be true all the time...
		return isAbstract() || hasSubclasses();
	}

	@Override
	public void pruneForSubclasses(TableGroup tableGroup, Map<String, EntityNameUse> entityNameUses) {
		final var tableReference = (NamedTableReference) tableGroup.getTableReference( getRootTableName() );
		if ( tableReference == null ) {
			throw new UnknownTableReferenceException( getRootTableName(), "Couldn't find table reference" );
		}
		// Replace the default union sub-query with a specially created one that only selects the tables for the treated entity names
		tableReference.setPrunedTableExpression( generateSubquery( entityNameUses ) );
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
		consumer.consume(
				tableName,
				0,
				() -> columnConsumer -> columnConsumer.accept( tableName, getIdentifierMapping(), getIdentifierColumnNames() )
		);
	}

	@Override
	protected boolean isPhysicalDiscriminator() {
		return false;
	}

	@Override
	protected EntityDiscriminatorMapping generateDiscriminatorMapping(PersistentClass bootEntityDescriptor) {
		return hasSubclasses() ? super.generateDiscriminatorMapping( bootEntityDescriptor ) : null;
	}

	@Override
	public int getTableSpan() {
		return 1;
	}

	@Override
	protected int[] getPropertyTableNumbers() {
		return new int[getPropertySpan()];
	}

	protected String generateSubquery(PersistentClass model) {
		final var factory = getFactory();
		final var sqlStringGenerationContext = factory.getSqlStringGenerationContext();
		if ( !model.hasSubclasses() ) {
			return model.getTable().getQualifiedName( sqlStringGenerationContext );
		}
		else {
			final Set<Column> columns = new LinkedHashSet<>();
			for ( var table : model.getSubclassTableClosure() ) {
				if ( !table.isAbstractUnionTable() ) {
					columns.addAll( table.getColumns() );
				}
			}
			final var dialect = factory.getJdbcServices().getDialect();
			final var subquery = new StringBuilder().append( "(" );
			final var classes =
					new JoinedList<>( List.of( model ),
							unmodifiableList( model.getSubclasses() ) );
			for ( var persistentClass : classes ) {
				final var table = persistentClass.getTable();
				if ( !table.isAbstractUnionTable() ) {
					//TODO: move to .sql package!!
					if ( subquery.length() > 1 ) {
						subquery.append( " union " );
						if ( dialect.supportsUnionAll() ) {
							subquery.append( "all " );
						}
					}
					subquery.append( "select " );
					for ( var column : columns ) {
						if ( !table.containsColumn( column ) ) {
							subquery.append( getSelectClauseNullString( column, dialect ) )
									.append( " as " );
						}
						subquery.append( column.getQuotedName( dialect ) )
								.append( ", " );
					}
					subquery.append( persistentClass.getSubclassId() )
							.append( " as clazz_ from " )
							.append( table.getQualifiedName( sqlStringGenerationContext ) );
				}
			}
			return subquery.append( ")" ).toString();
		}
	}

	private String getSelectClauseNullString(Column column, Dialect dialect) {
		return dialect.getSelectClauseNullString(
				new SqlTypedMappingImpl(
						column.getTypeName(),
						column.getLength(),
						column.getArrayLength(),
						column.getPrecision(),
						column.getScale(),
						column.getTemporalPrecision(),
						column.getType()
				),
				getFactory().getTypeConfiguration()
		);
	}

	protected String generateSubquery(Map<String, EntityNameUse> entityNameUses) {
		if ( !hasSubclasses() ) {
			return getTableName();
		}

		final var factory = getFactory();
		final var dialect = factory.getJdbcServices().getDialect();
		final var metamodel = factory.getMappingMetamodel();
		// Collect all selectables of every entity subtype and group by selection expression as well as table name
		final LinkedHashMap<String, Map<String, SelectableMapping>> selectables = new LinkedHashMap<>();
		final Set<String> tablesToUnion = new HashSet<>( entityNameUses.size() );
		// Check if there are filter uses and if so, we know the set of tables to union already
		for ( var entry : entityNameUses.entrySet() ) {
			final var persister = (UnionSubclassEntityPersister) metamodel.getEntityDescriptor( entry.getKey() );
			if ( entry.getValue().getKind() == EntityNameUse.UseKind.FILTER && !persister.isAbstract() ) {
				tablesToUnion.add( persister.getRootTableName() );
			}
			// Collect selectables grouped by the table names in which they appear
			persister.collectSelectableOwners( selectables );
		}

		if ( tablesToUnion.isEmpty() ) {
			// If there are no filter uses, we try to find the most specific treat uses and union all their subclass tables
			for ( var entry : entityNameUses.entrySet() ) {
				if ( entry.getValue().getKind() == EntityNameUse.UseKind.TREAT ) {
					// Collect all the real (non-abstract) table names
					final var persister = (UnionSubclassEntityPersister) metamodel.getEntityDescriptor( entry.getKey() );
					tablesToUnion.addAll( Arrays.asList( persister.getConstraintOrderedTableNameClosure() ) );
				}
			}
			if ( tablesToUnion.isEmpty() ) {
				// If there are only projection or expression uses, we can't optimize anything
				return getTableName();
			}
		}

		// Create a union subquery for the table names,
		// like generateSubquery(PersistentClass model)
		final var unionSubquery = new StringBuilder( subquery.length() ).append( "(" );
		final var typeConfiguration = factory.getTypeConfiguration();
		final var subMappingTypes = getSubMappingTypes();
		final ArrayList<EntityMappingType> subMappingTypesAndThis =
				new ArrayList<>( subMappingTypes.size() + 1 );
		subMappingTypesAndThis.add( this );
		subMappingTypesAndThis.addAll( subMappingTypes );
		for ( var mappingType : subMappingTypesAndThis ) {
			final var persister = (EntityPersister) mappingType;
			final String subclassTableName =
					mappingType.hasSubclasses()
							? persister.getRootTableName()
							: persister.getTableName();
			if ( tablesToUnion.contains( subclassTableName ) ) {
				if ( unionSubquery.length() > 1 ) {
					unionSubquery.append(" union ");
					if ( dialect.supportsUnionAll() ) {
						unionSubquery.append("all ");
					}
				}
				unionSubquery.append( "select " );
				for ( var selectableMappings : selectables.values() ) {
					var selectableMapping = selectableMappings.get( subclassTableName );
					if ( selectableMapping == null ) {
						// If there is no selectable mapping for a table name, we render a null expression
						selectableMapping = selectableMappings.values().iterator().next();
						unionSubquery.append( dialect.getSelectClauseNullString( selectableMapping, typeConfiguration ) )
								.append( " as " );
					}
					if ( selectableMapping.isFormula() ) {
						unionSubquery.append( selectableMapping.getSelectableName() );
					}
					else {
						unionSubquery.append( selectableMapping.getSelectionExpression() );
					}
					unionSubquery.append( ", " );
				}
				unionSubquery.append( persister.getDiscriminatorSQLValue() )
						.append( " as clazz_ from " )
						.append( subclassTableName );
			}
		}
		return unionSubquery.append( ")" ).toString();
	}

	private void collectSelectableOwners(LinkedHashMap<String, Map<String, SelectableMapping>> selectables) {
		if ( !isAbstract() ) {
			final SelectableConsumer selectableConsumer = (i, selectable) -> {
				var selectableMapping = selectables.computeIfAbsent(
						selectable.getSelectionExpression(),
						k -> new HashMap<>()
				);
				final String subclassTableName = hasSubclasses() ? getRootTableName() : getTableName();
				selectableMapping.put( subclassTableName, selectable );
			};
			getIdentifierMapping().forEachSelectable( selectableConsumer );
			final var versionMapping = getVersionMapping();
			if ( versionMapping != null ) {
				versionMapping.forEachSelectable( selectableConsumer );
			}
			final var attributeMappings = getAttributeMappings();
			for ( int i = 0, size = attributeMappings.size(); i < size; i++ ) {
				attributeMappings.get( i ).forEachSelectable( selectableConsumer );
			}
		}
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
	public String[] getConstraintOrderedTableNameClosure() {
		return constraintOrderedTableNames;
	}

	@Override
	public String[][] getConstraintOrderedTableKeyColumnClosure() {
		return constraintOrderedKeyColumnNames;
	}

	@Override
	public FilterAliasGenerator getFilterAliasGenerator(String rootAlias) {
		return new StaticFilterAliasGenerator( rootAlias );
	}
}
