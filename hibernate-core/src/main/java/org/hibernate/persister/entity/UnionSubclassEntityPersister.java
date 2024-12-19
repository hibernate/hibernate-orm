/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
import org.hibernate.Internal;
import org.hibernate.MappingException;
import org.hibernate.boot.Metadata;
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
import org.hibernate.mapping.Subclass;
import org.hibernate.mapping.Table;
import org.hibernate.metamodel.mapping.AttributeMappingsList;
import org.hibernate.metamodel.mapping.EntityDiscriminatorMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.TableDetails;
import org.hibernate.metamodel.mapping.internal.SqlTypedMappingImpl;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
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

		final Dialect dialect = creationContext.getDialect();

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
			for ( Subclass subclass : persistentClass.getSubclasses() ) {
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
		for ( Table table : persistentClass.getSubclassTableClosure() ) {
			subclassTables.add( determineTableName( table ) );
		}
		subclassSpaces = toStringArray( subclassTables );

		subquery = generateSubquery( persistentClass, creationContext.getMetadata() );
		final List<String> tableExpressions = new ArrayList<>( subclassSpaces.length * 2 );
		Collections.addAll( tableExpressions, subclassSpaces );
		tableExpressions.add( subquery );
		PersistentClass parentPersistentClass = persistentClass.getSuperclass();
		while ( parentPersistentClass != null ) {
			tableExpressions.add( generateSubquery( parentPersistentClass, creationContext.getMetadata() ) );
			parentPersistentClass = parentPersistentClass.getSuperclass();
		}
		for ( PersistentClass subPersistentClass : persistentClass.getSubclassClosure() ) {
			if ( subPersistentClass.hasSubclasses() ) {
				tableExpressions.add( generateSubquery( subPersistentClass, creationContext.getMetadata() ) );
			}
		}
		subclassTableExpressions = toStringArray( tableExpressions );

		if ( hasMultipleTables() ) {
			final int idColumnSpan = getIdentifierColumnSpan();
			final ArrayList<String> tableNames = new ArrayList<>();
			final ArrayList<String[]> keyColumns = new ArrayList<>();
			for ( Table table : persistentClass.getSubclassTableClosure() ) {
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
		sqlAliasBase = SqlAliasBase.from(
				sqlAliasBase,
				null,
				this,
				creationState.getSqlAliasBaseGenerator()
		);
		return new UnionTableReference( getTableName(), subclassTableExpressions, sqlAliasBase.generateNewAlias() );
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
	public int getSubclassPropertyTableNumber(String propertyName) {
		return 0;
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
		final NamedTableReference tableReference = (NamedTableReference) tableGroup.getTableReference( getRootTableName() );
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

	protected String generateSubquery(PersistentClass model, Metadata mapping) {

		final Dialect dialect = getFactory().getJdbcServices().getDialect();

		if ( !model.hasSubclasses() ) {
			return model.getTable().getQualifiedName( getFactory().getSqlStringGenerationContext() );
		}

		final Set<Column> columns = new LinkedHashSet<>();
		for ( Table table : model.getSubclassTableClosure() ) {
			if ( !table.isAbstractUnionTable() ) {
				columns.addAll( table.getColumns() );
			}
		}

		final StringBuilder subquery = new StringBuilder()
				.append( "(" );

		final List<PersistentClass> classes = new JoinedList<>(
				List.of( model ),
				Collections.unmodifiableList( model.getSubclasses() )
		);

		for ( PersistentClass clazz : classes ) {
			Table table = clazz.getTable();
			if ( !table.isAbstractUnionTable() ) {
				//TODO: move to .sql package!!
				if ( subquery.length() > 1 ) {
					subquery.append( " union " );
					if ( dialect.supportsUnionAll() ) {
						subquery.append( "all " );
					}
				}
				subquery.append( "select " );
				for ( Column col : columns ) {
					if ( !table.containsColumn( col ) ) {
						subquery.append( getSelectClauseNullString( col, dialect ) )
								.append(" as ");
					}
					subquery.append( col.getQuotedName( dialect ) )
							.append(", ");
				}
				subquery.append( clazz.getSubclassId() )
						.append( " as clazz_ from " )
						.append( table.getQualifiedName( getFactory().getSqlStringGenerationContext() ) );
			}
		}

		return subquery.append( ")" ).toString();
	}

	private String getSelectClauseNullString(Column col, Dialect dialect) {
		return dialect.getSelectClauseNullString(
				new SqlTypedMappingImpl(
						col.getTypeName(),
						col.getLength(),
						col.getPrecision(),
						col.getScale(),
						col.getTemporalPrecision(),
						col.getType()
				),
				getFactory().getTypeConfiguration()
		);
	}

	protected String generateSubquery(Map<String, EntityNameUse> entityNameUses) {
		if ( !hasSubclasses() ) {
			return getTableName();
		}

		final Dialect dialect = getFactory().getJdbcServices().getDialect();
		final MappingMetamodelImplementor metamodel = getFactory().getRuntimeMetamodels().getMappingMetamodel();
		// Collect all selectables of every entity subtype and group by selection expression as well as table name
		final LinkedHashMap<String, Map<String, SelectableMapping>> selectables = new LinkedHashMap<>();
		final Set<String> tablesToUnion = new HashSet<>( entityNameUses.size() );
		// Check if there are filter uses and if so, we know the set of tables to union already
		for ( Map.Entry<String, EntityNameUse> entry : entityNameUses.entrySet() ) {
			final UnionSubclassEntityPersister persister =
					(UnionSubclassEntityPersister) metamodel.getEntityDescriptor( entry.getKey() );
			if ( entry.getValue().getKind() == EntityNameUse.UseKind.FILTER && !persister.isAbstract() ) {
				tablesToUnion.add( persister.getRootTableName() );
			}
			// Collect selectables grouped by the table names in which they appear
			persister.collectSelectableOwners( selectables );
		}

		if ( tablesToUnion.isEmpty() ) {
			// If there are no filter uses, we try to find the most specific treat uses and union all their subclass tables
			for ( Map.Entry<String, EntityNameUse> entry : entityNameUses.entrySet() ) {
				if ( entry.getValue().getKind() == EntityNameUse.UseKind.TREAT ) {
					// Collect all the real (non-abstract) table names
					final UnionSubclassEntityPersister persister =
							(UnionSubclassEntityPersister) metamodel.getEntityDescriptor( entry.getKey() );
					tablesToUnion.addAll( Arrays.asList( persister.getConstraintOrderedTableNameClosure() ) );
				}
			}
			if ( tablesToUnion.isEmpty() ) {
				// If there are only projection or expression uses, we can't optimize anything
				return getTableName();
			}
		}

		// Create a union sub-query for the table names, like generateSubquery(PersistentClass model, Mapping mapping)
		final StringBuilder buf = new StringBuilder( subquery.length() ).append( "(" );

		final Collection<EntityMappingType> subMappingTypes = getSubMappingTypes();
		final ArrayList<EntityMappingType> subMappingTypesAndThis = new ArrayList<>( subMappingTypes.size() + 1 );
		subMappingTypesAndThis.add( this );
		subMappingTypesAndThis.addAll( subMappingTypes );
		for ( EntityMappingType mappingType : subMappingTypesAndThis ) {
			final EntityPersister persister = (EntityPersister) mappingType;
			final String subclassTableName;
			if ( mappingType.hasSubclasses() ) {
				subclassTableName = persister.getRootTableName();
			}
			else {
				subclassTableName = persister.getTableName();
			}
			if ( tablesToUnion.contains( subclassTableName ) ) {
				if ( buf.length() > 1 ) {
					buf.append(" union ");
					if ( dialect.supportsUnionAll() ) {
						buf.append("all ");
					}
				}
				buf.append( "select " );
				for ( Map<String, SelectableMapping> selectableMappings : selectables.values() ) {
					SelectableMapping selectableMapping = selectableMappings.get( subclassTableName );
					if ( selectableMapping == null ) {
						// If there is no selectable mapping for a table name, we render a null expression
						selectableMapping = selectableMappings.values().iterator().next();
						buf.append( dialect.getSelectClauseNullString( selectableMapping, getFactory().getTypeConfiguration() ) )
								.append( " as " );
					}
					if ( selectableMapping.isFormula() ) {
						buf.append( selectableMapping.getSelectableName() );
					}
					else {
						buf.append( selectableMapping.getSelectionExpression() );
					}
					buf.append( ", " );
				}
				buf.append( persister.getDiscriminatorSQLValue() )
						.append( " as clazz_ from " )
						.append( subclassTableName );
			}
		}
		return buf.append( ")" ).toString();
	}

	private void collectSelectableOwners(LinkedHashMap<String, Map<String, SelectableMapping>> selectables) {
		if ( !isAbstract() ) {
			final SelectableConsumer selectableConsumer = (i, selectable) -> {
				Map<String, SelectableMapping> selectableMapping = selectables.computeIfAbsent(
						selectable.getSelectionExpression(),
						k -> new HashMap<>()
				);
				final String subclassTableName;
				if ( hasSubclasses() ) {
					subclassTableName = getRootTableName();
				}
				else {
					subclassTableName = getTableName();
				}
				selectableMapping.put( subclassTableName, selectable );
			};
			getIdentifierMapping().forEachSelectable( selectableConsumer );
			if ( getVersionMapping() != null ) {
				getVersionMapping().forEachSelectable( selectableConsumer );
			}
			final AttributeMappingsList attributeMappings = getAttributeMappings();
			final int size = attributeMappings.size();
			for ( int i = 0; i < size; i++ ) {
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
