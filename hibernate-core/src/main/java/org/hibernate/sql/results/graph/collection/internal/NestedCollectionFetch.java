package org.hibernate.sql.results.graph.collection.internal;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import jakarta.annotation.Nullable;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.FetchOptions;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.internal.EntityCollectionPart;
import org.hibernate.query.sqm.function.FunctionRenderer;
import org.hibernate.query.sqm.function.SelfRenderingFunctionSqlAstExpression;
import org.hibernate.query.sqm.function.SelfRenderingOrderedSetAggregateFunctionSqlAstExpression;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.SqlAstNode;
import org.hibernate.sql.ast.spi.query.expression.ColumnReference;
import org.hibernate.sql.ast.spi.query.expression.JsonNullBehavior;
import org.hibernate.sql.ast.spi.query.from.SqlAstJoinType;
import org.hibernate.sql.ast.spi.query.from.TableGroup;
import org.hibernate.sql.ast.spi.query.select.QuerySpec;
import org.hibernate.sql.ast.spi.query.select.SelectStatement;
import org.hibernate.sql.ast.spi.query.select.SortSpecification;
import org.hibernate.sql.ast.spi.query.select.SqlSelection;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchList;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.InitializerParent;
import org.hibernate.sql.results.graph.collection.CollectionInitializer;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.type.BasicType;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * A collection fetch with {@link org.hibernate.FetchMethod#NESTED}: the
 * elements of the collection are aggregated into a single value of the row
 * of the owner, using a correlated subquery in the projection list:
 * <pre>
 * (select json_arrayagg(json_array(b.id, b.title, b.author_id)) from Book b where b.author_id = a.id)
 * </pre>
 * Each element is a positional JSON array, laid out by {@link #getNestedSelectableIndex},
 * and is read by the {@link NestedCollectionInitializer} using the regular
 * element initializers.
 *
 * @author Khurelkhuyag
 *
 * @since 8.1
 */
public class NestedCollectionFetch extends CollectionFetch {
	private final @Nullable DomainResult<?> collectionKeyResult;
	private final List<SelectableMapping> rowSelectables;
	private final SqlSelection aggregateSelection;
	private final FetchList fetches;

	/**
	 * Creates a nested fetch for the given collection, or returns {@code null}
	 * if the collection or the dialect does not support it, in which case the
	 * collection should be fetched using a secondary select.
	 */
	public static @Nullable NestedCollectionFetch create(
			NavigablePath fetchedPath,
			PluralAttributeMapping fetchedAttribute,
			FetchParent fetchParent,
			FetchOptions fetchOptions,
			DomainResultCreationState creationState) {
		final var creationContext = creationState.getSqlAstCreationState().getCreationContext();
		if ( !creationContext.getDialect().supportsNestedCollectionFetch() || isNested( fetchParent ) ) {
			return null;
		}
		final var functionRegistry = creationContext.getSqmFunctionRegistry();
		if ( !( functionRegistry.findFunctionDescriptor( "json_array" ) instanceof FunctionRenderer jsonArray )
				|| !( functionRegistry.findFunctionDescriptor( "json_arrayagg" ) instanceof FunctionRenderer jsonArrayAgg ) ) {
			return null;
		}
		final var rowSelectables = rowSelectables( fetchedAttribute );
		if ( rowSelectables == null ) {
			return null;
		}
		return new NestedCollectionFetch(
				fetchedPath,
				fetchedAttribute,
				fetchParent,
				fetchOptions,
				rowSelectables,
				jsonArray,
				jsonArrayAgg,
				creationState
		);
	}

	/**
	 * Is the given fetch parent part of the element of a nested collection?
	 */
	public static boolean isNested(FetchParent fetchParent) {
		for ( var parent = fetchParent; parent instanceof Fetch fetch; parent = fetch.getFetchParent() ) {
			if ( parent instanceof NestedCollectionFetch ) {
				return true;
			}
		}
		return false;
	}

	private NestedCollectionFetch(
			NavigablePath fetchedPath,
			PluralAttributeMapping fetchedAttribute,
			FetchParent fetchParent,
			FetchOptions fetchOptions,
			List<SelectableMapping> rowSelectables,
			FunctionRenderer jsonArray,
			FunctionRenderer jsonArrayAgg,
			DomainResultCreationState creationState) {
		super( fetchedPath, fetchedAttribute, fetchParent, fetchOptions );
		this.rowSelectables = rowSelectables;

		final var sqlAstCreationState = creationState.getSqlAstCreationState();
		final var fromClauseAccess = sqlAstCreationState.getFromClauseAccess();
		final var typeConfiguration = sqlAstCreationState.getCreationContext().getTypeConfiguration();
		final var ownerTableGroup = fromClauseAccess.getTableGroup( fetchParent.getNavigablePath() );

		final var keyDescriptor = fetchedAttribute.getKeyDescriptor();
		collectionKeyResult = keyDescriptor.getTargetPart().isEntityIdentifierMapping()
				? null
				: keyDescriptor.createTargetDomainResult( fetchedPath, ownerTableGroup, fetchParent, creationState );

		// The collection table group is not joined to the owner, it is
		// the root of the subquery, and the join predicate its restriction
		final var tableGroupJoin = fetchedAttribute.createTableGroupJoin(
				fetchedPath,
				ownerTableGroup,
				null,
				null,
				SqlAstJoinType.INNER,
				false,
				false,
				sqlAstCreationState
		);
		final var collectionTableGroup = tableGroupJoin.getJoinedGroup();
		fromClauseAccess.registerTableGroup( fetchedPath, collectionTableGroup );

		// the element columns are resolved to positions in the row, see getNestedSelectableIndex()
		fetches = creationState.visitNestedFetches( this );

		final List<SqlAstNode> row = new ArrayList<>( rowSelectables.size() + 1 );
		for ( var selectable : rowSelectables ) {
			row.add( new ColumnReference(
					collectionTableGroup.resolveTableReference( selectable.getContainingTableExpression() ),
					selectable
			) );
		}
		row.add( JsonNullBehavior.NULL );

		final BasicType<String> jsonType = typeConfiguration.getBasicTypeRegistry().resolve( String.class, SqlTypes.JSON );
		final var aggregate = new SelfRenderingOrderedSetAggregateFunctionSqlAstExpression<>(
				"json_arrayagg",
				jsonArrayAgg,
				List.of( new SelfRenderingFunctionSqlAstExpression<>( "json_array", jsonArray, row, jsonType, jsonType ) ),
				null,
				orderBy( fetchedAttribute, collectionTableGroup, creationState ),
				jsonType,
				// read the aggregated value as text
				typeConfiguration.getBasicTypeForJavaType( String.class )
		);

		final var subquery = new QuerySpec( false, 1 );
		subquery.getFromClause().addRoot( collectionTableGroup );
		subquery.applyPredicate( tableGroupJoin.getPredicate() );
		subquery.getSelectClause().addSqlSelection( new SqlSelectionImpl( 0, aggregate ) );

		aggregateSelection = sqlAstCreationState.getSqlExpressionResolver().resolveSqlSelection(
				new SelectStatement( subquery ),
				typeConfiguration.getJavaTypeRegistry().resolveDescriptor( String.class ),
				fetchParent,
				typeConfiguration
		);
	}

	private static List<SortSpecification> orderBy(
			PluralAttributeMapping attribute,
			TableGroup collectionTableGroup,
			DomainResultCreationState creationState) {
		final var orderByFragment = attribute.getOrderByFragment();
		if ( orderByFragment == null ) {
			// a list is ordered by its index column when it is read
			return List.of();
		}
		else {
			final var querySpec = new QuerySpec( false );
			orderByFragment.apply( querySpec, collectionTableGroup, creationState.getSqlAstCreationState() );
			return querySpec.getSortSpecifications();
		}
	}

	/**
	 * The columns making up a row of the nested collection, or {@code null} if the
	 * collection cannot be fetched nested.
	 */
	private static @Nullable List<SelectableMapping> rowSelectables(PluralAttributeMapping attribute) {
		final var collectionDescriptor = attribute.getCollectionDescriptor();
		switch ( attribute.getMappedType().getCollectionSemantics().getCollectionClassification() ) {
			case BAG, SET, ORDERED_SET, SORTED_SET, LIST:
				break;
			default:
				return null;
		}
		final List<SelectableMapping> selectables = new ArrayList<>();
		final var elementDescriptor = attribute.getElementDescriptor();
		if ( elementDescriptor instanceof EntityCollectionPart entityPart ) {
			if ( !collectionDescriptor.isOneToMany() ) {
				return null;
			}
			final var entity = entityPart.getAssociatedEntityMappingType();
			if ( entity.hasSubclasses() || entity.getSuperMappingType() != null ) {
				return null;
			}
			entityRowSelectables( entity, selectables );
		}
		else {
			elementDescriptor.forEachSelectable( (index, selectable) -> addRowSelectable( selectables, selectable ) );
		}
		final var indexDescriptor = attribute.getIndexDescriptor();
		if ( indexDescriptor != null ) {
			indexDescriptor.forEachSelectable( (index, selectable) -> addRowSelectable( selectables, selectable ) );
		}
		final String table = selectables.get( 0 ).getContainingTableExpression();
		for ( var selectable : selectables ) {
			if ( selectable.isFormula()
					|| !table.equals( selectable.getContainingTableExpression() )
					|| !isSupportedType( selectable.getJdbcMapping() ) ) {
				return null;
			}
		}
		return selectables;
	}

	private static void entityRowSelectables(EntityMappingType entity, List<SelectableMapping> selectables) {
		entity.getIdentifierMapping().forEachSelectable( (index, selectable) -> addRowSelectable( selectables, selectable ) );
		final var discriminatorMapping = entity.getDiscriminatorMapping();
		if ( discriminatorMapping != null && discriminatorMapping.hasPhysicalColumn() ) {
			discriminatorMapping.forEachSelectable( (index, selectable) -> addRowSelectable( selectables, selectable ) );
		}
		final var versionMapping = entity.getVersionMapping();
		if ( versionMapping != null ) {
			versionMapping.forEachSelectable( (index, selectable) -> addRowSelectable( selectables, selectable ) );
		}
		entity.forEachSelectable( (index, selectable) -> addRowSelectable( selectables, selectable ) );
	}

	private static void addRowSelectable(List<SelectableMapping> selectables, SelectableMapping selectable) {
		if ( indexOf( selectables, selectable.getSelectableName() ) == -1 ) {
			selectables.add( selectable );
		}
	}

	private static int indexOf(List<SelectableMapping> selectables, String selectableName) {
		for ( int i = 0; i < selectables.size(); i++ ) {
			if ( selectables.get( i ).getSelectableName().equals( selectableName ) ) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * The types of which every dialect renders and reads a JSON representation.
	 */
	private static boolean isSupportedType(JdbcMapping jdbcMapping) {
		return switch ( jdbcMapping.getJdbcType().getDefaultSqlTypeCode() ) {
			case SqlTypes.BIT, SqlTypes.BOOLEAN,
					SqlTypes.TINYINT, SqlTypes.SMALLINT, SqlTypes.INTEGER, SqlTypes.BIGINT,
					SqlTypes.REAL, SqlTypes.FLOAT, SqlTypes.DOUBLE, SqlTypes.NUMERIC, SqlTypes.DECIMAL,
					SqlTypes.CHAR, SqlTypes.NCHAR, SqlTypes.VARCHAR, SqlTypes.NVARCHAR, SqlTypes.ENUM,
					SqlTypes.LONGVARCHAR, SqlTypes.LONGNVARCHAR, SqlTypes.LONG32VARCHAR, SqlTypes.LONG32NVARCHAR,
					SqlTypes.DATE, SqlTypes.TIME, SqlTypes.TIMESTAMP,
					SqlTypes.LOCAL_DATE, SqlTypes.LOCAL_TIME, SqlTypes.LOCAL_DATE_TIME -> true;
			default -> false;
		};
	}

	List<SelectableMapping> getRowSelectables() {
		return rowSelectables;
	}

	@Override
	public int getNestedSelectableIndex(String selectableName) {
		return indexOf( rowSelectables, selectableName );
	}

	@Override
	public FetchTiming getTiming() {
		// the collection is not joined, so it does not affect the rows of the result set,
		// just like a SelectEagerCollectionFetch
		return FetchTiming.DELAYED;
	}

	@Override
	public boolean hasTableGroup() {
		return false;
	}

	@Override
	public FetchList getFetches() {
		return fetches;
	}

	@Override
	public @Nullable Fetch findFetch(Fetchable fetchable) {
		return fetches.get( fetchable );
	}

	@Override
	public CollectionInitializer<?> createInitializer(InitializerParent<?> parent, AssemblerCreationState creationState) {
		final var attribute = getFetchedMapping();
		final var indexDescriptor = attribute.getIndexDescriptor();
		return new NestedCollectionInitializer(
				getNavigablePath(),
				attribute,
				parent,
				collectionKeyResult,
				aggregateSelection,
				rowSelectables,
				fetches.get( attribute.getElementDescriptor() ),
				indexDescriptor == null ? null : fetches.get( indexDescriptor ),
				getFetchOptions(),
				creationState
		);
	}

	@Override
	public JavaType<?> getResultJavaType() {
		return getFetchedMapping().getJavaType();
	}

	@Override
	public void collectValueIndexesToCache(BitSet valueIndexes) {
		if ( collectionKeyResult != null ) {
			collectionKeyResult.collectValueIndexesToCache( valueIndexes );
		}
		valueIndexes.set( aggregateSelection.getValuesArrayPosition() );
	}
}
