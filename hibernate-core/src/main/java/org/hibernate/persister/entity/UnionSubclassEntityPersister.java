/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

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
import java.util.function.Function;
import java.util.function.Supplier;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;

import org.hibernate.Internal;
import org.hibernate.MappingException;
import org.hibernate.action.queue.spi.meta.ColumnDescriptor;
import org.hibernate.action.queue.spi.meta.EntityTableDescriptor;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.NaturalIdDataAccess;
import org.hibernate.dialect.Dialect;
import org.hibernate.id.IdentityGenerator;
import org.hibernate.mapping.Table;
import org.hibernate.persister.filter.FilterAliasGenerator;
import org.hibernate.persister.filter.internal.StaticFilterAliasGenerator;
import org.hibernate.internal.util.collections.JoinedList;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metamodel.mapping.AuditMapping;
import org.hibernate.metamodel.mapping.DiscriminatorValue;
import org.hibernate.metamodel.mapping.EntityDiscriminatorMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.TableDetails;
import org.hibernate.metamodel.mapping.internal.SqlTypedMappingImpl;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.persister.state.spi.StateManagement;
import org.hibernate.query.sqm.SetOperator;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.creation.SqlAliasBase;
import org.hibernate.sql.ast.spi.creation.SqlAstCreationState;
import org.hibernate.sql.ast.spi.query.from.NamedTableReference;
import org.hibernate.sql.ast.spi.query.from.TableGroup;
import org.hibernate.sql.ast.spi.query.from.UnionTableGroup;
import org.hibernate.sql.ast.spi.query.from.UnionTableReference;
import org.hibernate.sql.ast.spi.query.from.UnknownTableReferenceException;
import org.hibernate.sql.ast.spi.query.predicate.Predicate;
import org.hibernate.type.BasicType;
import org.hibernate.type.StandardBasicTypes;

import static java.util.Collections.addAll;
import static java.util.Collections.unmodifiableList;
import static java.util.function.Function.identity;
import static org.hibernate.internal.util.collections.ArrayHelper.to2DStringArray;
import static org.hibernate.internal.util.collections.ArrayHelper.toStringArray;

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
	private final DiscriminatorValue discriminatorValue;
	private final String discriminatorSQLValue;
	private final BasicType<?> discriminatorType;
	private final Map<DiscriminatorValue, String> subclassByDiscriminatorValue = new HashMap<>();

	private final String[] constraintOrderedTableNames;
	private final String[][] constraintOrderedKeyColumnNames;

	public UnionSubclassEntityPersister(
			@Nonnull final PersistentClass persistentClass,
			@Nullable final EntityDataAccess cacheAccessStrategy,
			@Nullable final NaturalIdDataAccess naturalIdRegionAccessStrategy,
			@Nonnull final RuntimeModelCreationContext creationContext)
			throws HibernateException {
		this( persistentClass, cacheAccessStrategy, naturalIdRegionAccessStrategy, creationContext, identity() );
	}

	protected UnionSubclassEntityPersister(
			@Nonnull final PersistentClass persistentClass,
			@Nullable final EntityDataAccess cacheAccessStrategy,
			@Nullable final NaturalIdDataAccess naturalIdRegionAccessStrategy,
			@Nonnull final RuntimeModelCreationContext creationContext,
			@Nonnull final Function<StateManagement, StateManagement> stateManagementConverter)
					throws HibernateException {
		super( persistentClass, cacheAccessStrategy, naturalIdRegionAccessStrategy, creationContext, stateManagementConverter );

		validateGenerator();

		final var dialect = creationContext.getDialect();

		// TABLE

		tableName = determineTableName( persistentClass.getTable() );
		subclassTableNames = new String[]{tableName};

		discriminatorValue = new DiscriminatorValue.Literal( persistentClass.getSubclassId() );
		discriminatorSQLValue = String.valueOf( persistentClass.getSubclassId() );
		discriminatorType = creationContext.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.INTEGER );

		// PROPERTIES

		// SUBCLASSES
		subclassByDiscriminatorValue.put( new DiscriminatorValue.Literal( persistentClass.getSubclassId() ), persistentClass.getEntityName() );
		if ( persistentClass.isPolymorphic() ) {
			for ( var subclass : persistentClass.getSubclasses() ) {
				subclassByDiscriminatorValue.put( new DiscriminatorValue.Literal( subclass.getSubclassId() ), subclass.getEntityName() );
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
			for ( var persistentSubclass : persistentClass.getPersistentClassClosure() ) {
				final Table table = persistentSubclass.getTable();
				if ( !table.isAbstractUnionTable() ) {
					tableNames.add( determineTableName( table ) );
					final String[] key = new String[idColumnSpan];
					final List<Column> columns = persistentSubclass.getKey().getColumns();
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
	}

	@Nonnull
	protected EntityTableDescriptor[] buildTableDescriptors() {
		var builder = createTableDescriptorBuilder(
				tableName,
				0,
				() -> columnConsumer -> columnConsumer.accept(
						getIdentifierMapping(),
						tableName,
						getIdentifierColumnNames()
				)
		);

		visitAttributeMappings( (attribute) -> {
			builder.addAttribute( attribute );
			attribute.forEachSelectable( (selectableIndex, selectable)
					-> builder.addColumn( attribute, ColumnDescriptor.from( selectable ) ) );
		} );

		// Union subclass has only one table, so entity-wide flag equals table flag
		return new EntityTableDescriptor[] { builder.build( builder.isSelfReferential ) };
	}

	protected void validateGenerator() {
		if ( getGenerator() instanceof IdentityGenerator ) {
			throw new MappingException( "Cannot use identity column key generation with <union-subclass> mapping for: " + getEntityName() );
		}
	}

	@Override
	public boolean containsTableReference(@Nonnull String tableExpression) {
		for ( String subclassTableExpression : subclassTableExpressions ) {
			if ( subclassTableExpression.equals( tableExpression ) ) {
				return true;
			}
		}
		return false;
	}


	@Nonnull
	@Override
	public UnionTableReference createPrimaryTableReference(
			@Nullable SqlAliasBase sqlAliasBase,
			@Nonnull SqlAstCreationState creationState) {
		final var loadQueryInfluencers = creationState.getLoadQueryInfluencers();
		final var auxMapping = getAuxiliaryMapping();
		final boolean useAuxiliaryTable =
				auxMapping != null
						&& auxMapping.useAuxiliaryTable( loadQueryInfluencers );
		final String resolvedTableName = useAuxiliaryTable
				? auxMapping.resolveTableName( getTableName() )
				: getTableName();
		final String[] resolvedTableExpressions;
		if ( useAuxiliaryTable ) {
			// Include both original and audit table expressions for resolution
			final var resolved = new ArrayList<String>( subclassTableExpressions.length * 2 );
			for ( String expr : subclassTableExpressions ) {
				resolved.add( expr );
				resolved.add( auxMapping.resolveTableName( expr ) );
			}
			resolvedTableExpressions = resolved.toArray( String[]::new );
		}
		else {
			resolvedTableExpressions = subclassTableExpressions;
		}
		final var tableReference = new UnionTableReference(
				resolvedTableName,
				resolvedTableExpressions,
				SqlAliasBase.from(
						sqlAliasBase,
						null,
						this,
						creationState.getSqlAliasBaseGenerator()
				).generateNewAlias()
		);
		tableReference.applyAuxiliaryTable( auxMapping, loadQueryInfluencers );
		return tableReference;
	}

	@Nonnull
	@Override
	public TableGroup createRootTableGroup(
			boolean canUseInnerJoins,
			@Nonnull NavigablePath navigablePath,
			@Nullable String explicitSourceAlias,
			@Nullable SqlAliasBase sqlAliasBase,
			@Nullable Supplier<Consumer<Predicate>> additionalPredicateCollectorAccess,
			@Nonnull SqlAstCreationState creationState) {
		final var tableGroup = new UnionTableGroup(
				canUseInnerJoins,
				navigablePath,
				createPrimaryTableReference( sqlAliasBase, creationState ),
				this,
				explicitSourceAlias
		);
		if ( additionalPredicateCollectorAccess != null ) {
			final var auxMapping = getAuxiliaryMapping();
			if ( auxMapping != null ) {
				auxMapping.applyPredicate(
						additionalPredicateCollectorAccess,
						creationState,
						tableGroup,
						tableGroup.getPrimaryTableReference(),
						this
				);
			}
		}
		return tableGroup;
	}

	@Override
	public boolean needsDiscriminator() {
		return false;
	}

	@Nonnull
	@Override
	public Serializable[] getQuerySpaces() {
		return subclassSpaces;
	}

	@Nonnull
	@Override
	public String getRootTableName() {
		return tableName;
	}

	@Nonnull
	@Override
	public String getTableName() {
		return hasSubclasses() ? subquery : tableName;
	}

	@Nullable
	@Override
	public BasicType<?> getDiscriminatorType() {
		return discriminatorType;
	}

	@Nonnull
	@Override
	public Map<DiscriminatorValue, String> getSubclassByDiscriminatorValue() {
		return subclassByDiscriminatorValue;
	}

	@Nonnull
	@Override
	public TableDetails getMappedTableDetails() {
		return getTableMapping( 0 );
	}

	@Nonnull
	@Override
	public TableDetails getIdentifierTableDetails() {
		return getTableMapping( 0 );
	}

	@Nonnull
	@Override
	public DiscriminatorValue getDiscriminatorValue() {
		return discriminatorValue;
	}

	@Nullable
	@Override
	public String getDiscriminatorSQLValue() {
		return discriminatorSQLValue;
	}

	@Nonnull
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

	@Nonnull
	@Override
	public String getTableName(int j) {
		return tableName;
	}

	@Nonnull
	@Override
	public String[] getKeyColumns(int j) {
		return getIdentifierColumnNames();
	}

	@Override
	public boolean isTableCascadeDeleteEnabled(int j) {
		return false;
	}

	@Nonnull
	@Override
	protected TableMutationDetails createTableMutationDetails(@Nonnull PersistentClass bootEntityDescriptor, int relativePosition) {
		return createTableMutationDetails( bootEntityDescriptor );
	}

	@Override
	public boolean isPropertyOfTable(int property, int j) {
		return true;
	}

	// Execute the SQL:

	@Nonnull
	@Override
	public String getAttributeMutationTableName(int i) {
		return getTableName();//ie. the subquery! yuck!
	}

	@Nonnull
	@Override
	public String physicalTableNameForMutation(@Nonnull SelectableMapping selectableMapping) {
		assert !selectableMapping.isFormula();
		return tableName;
	}

	@Override
	protected boolean isIdentifierTable(@Nonnull String tableExpression) {
		return tableExpression.equals( getRootTableName() );
	}

	@Override
	public boolean hasMultipleTables() {
		// This could also just be true all the time...
		return isAbstract() || hasSubclasses();
	}

	@Override
	public void pruneForSubclasses(@Nonnull TableGroup tableGroup, @Nonnull Map<String, EntityNameUse> entityNameUses) {
		final var tableReference = (NamedTableReference) tableGroup.getTableReference( getRootTableName() );
		if ( tableReference == null ) {
			throw new UnknownTableReferenceException( getRootTableName(), "Couldn't find table reference" );
		}
		tableReference.setPrunedTableExpression( generateSubquery(
				entityNameUses,
				tableReference.getTableExpression()
		) );
	}

	@Override
	public void visitConstraintOrderedTables(@Nonnull ConstraintOrderedTableConsumer consumer) {
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
	protected void visitMutabilityOrderedTables(@Nonnull MutabilityOrderedTableConsumer consumer) {
		consumer.consume(
				tableName,
				0,
				() -> columnConsumer -> columnConsumer.accept(
						getIdentifierMapping(),
						tableName,
						getIdentifierColumnNames()
				)
		);
	}

	@Override
	protected boolean isPhysicalDiscriminator() {
		return false;
	}

	@Nullable
	@Override
	protected EntityDiscriminatorMapping generateDiscriminatorMapping(@Nonnull PersistentClass bootEntityDescriptor) {
		return hasSubclasses() ? super.generateDiscriminatorMapping( bootEntityDescriptor ) : null;
	}

	@Override
	public int getTableSpan() {
		return 1;
	}


	@Nonnull
	@Override
	protected int[] getPropertyTableNumbers() {
		return new int[getPropertySpan()];
	}

	@Nonnull
	protected String generateSubquery(@Nonnull PersistentClass model) {
		return generateSubquery( model, null, null );
	}

	/**
	 * Generate a union subquery for the given model.
	 *
	 * @param tableNameResolver when non-null, resolves original table names to
	 *                          alternative names (e.g. audit table names)
	 * @param extraSelectExpressions additional column expressions to include in
	 *                               each SELECT of the union (e.g. REV, REVTYPE)
	 */
	@Nonnull
	public String generateSubquery(
			@Nonnull PersistentClass model,
			@Nullable Function<String, String> tableNameResolver,
			@Nullable List<String> extraSelectExpressions) {
		final var factory = getFactory();
		final var sqlStringGenerationContext = factory.getSqlStringGenerationContext();
		if ( !model.hasSubclasses() ) {
			final String qualifiedName = model.getTable().getQualifiedName( sqlStringGenerationContext );
			return tableNameResolver != null ? tableNameResolver.apply( qualifiedName ) : qualifiedName;
		}
		else {
			final var classes =
					new JoinedList<>( List.of( model ),
							unmodifiableList( model.getSubclasses() ) );
			final Set<Column> columns = new LinkedHashSet<>();
			for ( var persistentClass : classes ) {
				final var table = persistentClass.getTable();
				if ( !table.isAbstractUnionTable() ) {
					columns.addAll( table.getColumns() );
				}
			}
			final var dialect = factory.getJdbcServices().getDialect();
			final var subquery = new StringBuilder().append( "(" );
			// A non-null table name resolver means we use auxiliary-table columns for the projection
			final boolean useAuxiliaryTables = tableNameResolver != null;
			for ( var persistentClass : classes ) {
				final var table = persistentClass.getTable();
				if ( !table.isAbstractUnionTable() ) {
					final var projectionTable = useAuxiliaryTables ? persistentClass.getAuxiliaryTable() : table;
					//TODO: move to .sql package!!
					if ( subquery.length() > 1 ) {
						subquery.append( " union " );
						if ( dialect.getSetOperationSupport().supports( SetOperator.UNION_ALL ) ) {
							subquery.append( "all " );
						}
					}
					subquery.append( "select " );
					for ( var column : columns ) {
						if ( !projectionTable.containsColumn( column ) ) {
							subquery.append( getSelectClauseNullString( column, dialect ) )
									.append( " as " );
						}
						subquery.append( column.getQuotedName( dialect ) )
								.append( ", " );
					}
					if ( extraSelectExpressions != null ) {
						for ( var expr : extraSelectExpressions ) {
							subquery.append( expr ).append( ", " );
						}
					}
					subquery.append( persistentClass.getSubclassId() )
							.append( " as clazz_" );
					final String qualifiedName = table.getQualifiedName( sqlStringGenerationContext );
					subquery.append( " from " )
							.append( tableNameResolver != null
									? tableNameResolver.apply( qualifiedName )
									: qualifiedName );
				}
			}
			return subquery.append( ")" ).toString();
		}
	}

	@Nonnull
	private String getSelectClauseNullString(@Nonnull Column column, @Nonnull Dialect dialect) {
		return dialect.getSelectClauseNullString(
				new SqlTypedMappingImpl(
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

	@Nonnull
	protected String generateSubquery(@Nonnull Map<String, EntityNameUse> entityNameUses, @Nonnull String currentTableExpression) {
		final var auxMapping = currentTableExpression.equals( getTableName() ) ? null : getAuxiliaryMapping();
		if ( !hasSubclasses() ) {
			return currentTableExpression;
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
			persister.collectSelectableOwners( selectables, auxMapping instanceof AuditMapping );
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
				return currentTableExpression;
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
					if ( dialect.getSetOperationSupport().supports( SetOperator.UNION_ALL ) ) {
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
					final String selectable =
							selectableMapping.isFormula()
									? selectableMapping.getSelectableName()
									: selectableMapping.getSelectionExpression();
					unionSubquery.append( selectable )
							.append( ", " );
				}
				if ( auxMapping != null ) {
					for ( var expr : auxMapping.getExtraSelectExpressions() ) {
						unionSubquery.append( expr ).append( ", " );
					}
				}
				unionSubquery.append( persister.getDiscriminatorSQLValue() )
						.append( " as clazz_" );
				unionSubquery.append( " from " )
						.append( auxMapping != null
								? auxMapping.resolveTableName( subclassTableName )
								: subclassTableName );
			}
		}
		return unionSubquery.append( ")" ).toString();
	}

	private void collectSelectableOwners(
			@Nonnull LinkedHashMap<String, Map<String, SelectableMapping>> selectables,
			boolean auditMapping) {
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
				final var attributeMapping = attributeMappings.get( i );
				// If this entity is audited, skip collecting audit-excluded selectable mappings
				if ( !auditMapping || !isPropertyAuditedExcluded( attributeMapping.getStateArrayPosition() ) ) {
					attributeMapping.forEachSelectable( selectableConsumer );
				}
			}
		}
	}

	@Nonnull
	@Override
	protected String[] getSubclassTableKeyColumns(int j) {
		if ( j != 0 ) {
			throw new AssertionFailure( "only one table" );
		}
		return getIdentifierColumnNames();
	}

	@Nonnull
	@Override
	public String getSubclassTableName(int j) {
		if ( j != 0 ) {
			throw new AssertionFailure( "only one table" );
		}
		return tableName;
	}

	@Nonnull
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

	@Nonnull
	@Override
	public String[] getConstraintOrderedTableNameClosure() {
		return constraintOrderedTableNames;
	}

	@Nonnull
	@Override
	public String[][] getConstraintOrderedTableKeyColumnClosure() {
		return constraintOrderedKeyColumnNames;
	}

	@Nonnull
	@Override
	public FilterAliasGenerator getFilterAliasGenerator(@Nonnull String rootAlias) {
		return new StaticFilterAliasGenerator( rootAlias );
	}
}
