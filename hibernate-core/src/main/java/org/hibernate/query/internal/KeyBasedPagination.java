package org.hibernate.query.internal;

import jakarta.annotation.Nonnull;
import org.hibernate.query.IllegalQueryOperationException;
import org.hibernate.query.Order;
import org.hibernate.query.criteria.JpaCompoundSelection;
import org.hibernate.query.criteria.JpaSelection;
import org.hibernate.query.sqm.spi.NodeBuilder;
import org.hibernate.query.sqm.tree.spi.domain.SqmPath;
import org.hibernate.query.sqm.tree.spi.expression.SqmExpression;
import org.hibernate.query.sqm.tree.spi.from.SqmFrom;
import org.hibernate.query.sqm.tree.spi.predicate.SqmPredicate;
import org.hibernate.query.sqm.tree.spi.select.SqmOrderByClause;
import org.hibernate.query.sqm.tree.spi.select.SqmSelectStatement;

import java.util.ArrayList;
import java.util.List;

import static org.hibernate.query.sqm.ComparisonOperator.GREATER_THAN;
import static org.hibernate.query.sqm.ComparisonOperator.LESS_THAN;
import static org.hibernate.query.sqm.internal.SqmUtil.sortSpecification;

/**
 * Manipulation of SQM query tree for key-based pagination.
 *
 * @author Gavin King
 */
public class KeyBasedPagination {

	@Nonnull
	public static <R> SqmSelectStatement<KeyedResult<R>> paginate(
			List<Order<? super R>> keyDefinition,
			List<Comparable<?>> keyValues,
			@Nonnull SqmSelectStatement<KeyedResult<R>> statement,
			@Nonnull NodeBuilder builder) {
		final var querySpec = statement.getQuerySpec();
		final var items = querySpec.getSelectClause().getSelectionItems();
		final var selected = switch ( items.size() ) {
			case 0 -> querySpec.getImplicitSelection();
			case 1 -> items.get( 0 );
			default -> throw new IllegalQueryOperationException("Query has multiple items in the select list");
		};
		if ( !( selected instanceof SqmFrom<?, ?> ) ) {
			throw new IllegalQueryOperationException( "Select item was not an entity type" );
		}
		final var orderBy = new SqmOrderByClause( keyDefinition.size() );
		final var keyPaths = new ArrayList<SqmPath<?>>( keyDefinition.size() );
		for ( var order : keyDefinition ) {
			final var sort = sortSpecification( statement, order );
			if ( !( sort.getSortExpression() instanceof SqmPath<?> path ) ) {
				throw new IllegalQueryOperationException( "Key-based pagination based on select list items is not yet supported" );
			}
			orderBy.addSortSpecification( sort );
			keyPaths.add( path );
		}
		querySpec.setOrderByClause( orderBy );
		statement.select( keySelection( selected, builder, keyPaths ) );
		if ( keyValues != null ) {
			querySpec.applyPredicate( keyRestriction( keyDefinition, keyValues, keyPaths, builder ) );
		}
		return statement;
	}

	private static SqmPredicate keyRestriction(
			@Nonnull List<? extends Order<?>> keyDefinition,
			List<Comparable<?>> keyValues,
			List<SqmPath<?>> keyPaths,
			NodeBuilder builder) {
		SqmPredicate restriction = null;
		for ( int i = keyDefinition.size() - 1; i >= 0; i-- ) {
			final var order = keyDefinition.get( i );
			final var path = keyPaths.get( i );
			final var keyValue = keyValues.get( i );
			final SqmExpression<?> key;
			final SqmExpression<?> value;
			if ( order.caseSensitive() ) {
				key = path;
				value = builder.value( keyValue, path );
			}
			else {
				final var engine = builder.getQueryEngine();
				final var lower = engine.getSqmFunctionRegistry().findFunctionDescriptor( "lower" );
				key = lower.generateSqmExpression( path, null, engine );
				// Bind the value with the path's mapping so its converter also applies inside lower().
				value = lower.generateSqmExpression( builder.valueParameter( keyValue, path ), null, engine );
			}
			final var operator = switch ( order.direction() ) {
				case ASCENDING -> GREATER_THAN;
				case DESCENDING -> LESS_THAN;
			};
			final var after =
					builder.comparison( key, operator, value );
			restriction =
					restriction == null
							? after
							: builder.or( after, builder.and( key.equalTo( value ), restriction ) );
		}
		return restriction;
	}

	@Nonnull
	private static <R> JpaCompoundSelection<KeyedResult<R>> keySelection(
			JpaSelection<?> selected, @Nonnull NodeBuilder builder, @Nonnull List<SqmPath<?>> keyPaths) {
		// KeyedResult<R> has the same runtime class for every result type R.
		@SuppressWarnings("unchecked")
		final var resultClass = (Class<KeyedResult<R>>) (Class<?>) KeyedResult.class;
		return builder.construct( resultClass, selected, builder.construct( List.class, keyPaths ) );
	}

}
