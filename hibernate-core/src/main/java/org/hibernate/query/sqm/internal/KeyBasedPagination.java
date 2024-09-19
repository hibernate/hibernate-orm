/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.internal;

import jakarta.persistence.criteria.Expression;
import org.hibernate.AssertionFailure;
import org.hibernate.query.IllegalQueryOperationException;
import org.hibernate.query.Order;
import org.hibernate.query.SortDirection;
import org.hibernate.query.criteria.JpaCompoundSelection;
import org.hibernate.query.criteria.JpaSelection;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.query.sqm.tree.select.SqmQuerySpec;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.hibernate.query.sqm.internal.SqmUtil.sortSpecification;

/**
 * Manipulation of SQM query tree for key-based pagination.
 *
 * @author Gavin King
 */
public class KeyBasedPagination {

	static <R> SqmSelectStatement<KeyedResult<R>> paginate(
			List<Order<? super R>> keyDefinition, List<Comparable<?>> keyValues,
			SqmSelectStatement<KeyedResult<R>> statement, NodeBuilder builder) {
		final SqmQuerySpec<?> querySpec = statement.getQuerySpec();
		final List<? extends JpaSelection<?>> items = querySpec.getSelectClause().getSelectionItems();
		if ( items.size() == 1 ) {
			final JpaSelection<?> selected = items.get(0);
			if ( selected instanceof SqmRoot) {
				statement.orderBy( keyDefinition.stream().map( order -> sortSpecification( statement, order ) )
						.collect( toList() ) );
				final SqmFrom<?,?> root = (SqmFrom<?,?>) selected;
				statement.select( keySelection( keyDefinition, root, selected, builder ) );
				if ( keyValues != null ) {
					final SqmPredicate restriction = keyRestriction( keyDefinition, keyValues, root, builder );
					final SqmPredicate queryWhere = querySpec.getRestriction();
					statement.where( queryWhere == null ? restriction : builder.and( queryWhere, restriction ) );
				}
				return statement;
			}
			else {
				throw new IllegalQueryOperationException("Select item was not an entity type");
			}
		}
		else {
			throw new IllegalQueryOperationException("Query has multiple items in the select list");
		}
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static <R> SqmPredicate keyRestriction(
			List<Order<? super R>> keyDefinition,
			List<Comparable<?>> keyValues,
			SqmFrom<?, ?> root,
			NodeBuilder builder) {
		final List<SqmPath<?>> keyPaths = new ArrayList<>();
		for ( Order<? super R> key : keyDefinition ) {
			keyPaths.add( root.get( key.getAttributeName() ) );
		}
		SqmPredicate restriction = null;
		for (int i = 0; i < keyDefinition.size(); i++ ) {
			// ordering by an attribute of the returned entity
			final SortDirection direction = keyDefinition.get(i).getDirection();
			final SqmPath key = keyPaths.get(i);
			final Comparable keyValue = keyValues.get(i);
			final List<SqmPath<?>> previousKeys = keyPaths.subList(0, i);
			final SqmPredicate predicate = keyPredicate( key, keyValue, direction, previousKeys, keyValues, builder );
			restriction = restriction == null ? predicate : builder.or( restriction, predicate );
		}
		return restriction;
	}

	private static <R> JpaCompoundSelection<KeyedResult<R>> keySelection(
			List<Order<? super R>> keyDefinition,
			SqmFrom<?, ?> root, JpaSelection<?> selected,
			NodeBuilder builder) {
		final List<SqmPath<?>> items = new ArrayList<>();
		for ( Order<? super R> key : keyDefinition ) {
			if ( key.getEntityClass() == null ) {
				throw new IllegalQueryOperationException("Key-based pagination based on select list items is not yet supported");
			}
			else {
				if ( !key.getEntityClass().isAssignableFrom( selected.getJavaType() ) ) {
					throw new IllegalQueryOperationException("Select item was of wrong entity type");
				}
				// ordering by an attribute of the returned entity
				items.add( root.get( key.getAttributeName() ) );
			}
		}
		return keyedResultConstructor( selected, builder, items );
	}

	private static <R> JpaCompoundSelection<KeyedResult<R>> keyedResultConstructor(
			JpaSelection<?> selected, NodeBuilder builder, List<SqmPath<?>> newItems) {
		@SuppressWarnings({"rawtypes", "unchecked"})
		final Class<KeyedResult<R>> resultClass = (Class) KeyedResult.class;
		return builder.construct( resultClass, asList( selected, builder.construct(List.class, newItems ) ) );
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static <C extends Comparable<? super C>> SqmPredicate keyPredicate(
			Expression<? extends C> key, C keyValue, SortDirection direction,
			List<SqmPath<?>> previousKeys, List<Comparable<?>> keyValues,
			NodeBuilder builder) {
		SqmPredicate predicate;
		switch ( direction ) {
			case ASCENDING:
				predicate = builder.greaterThan( key, keyValue );
				break;
			case DESCENDING:
				predicate = builder.lessThan( key, keyValue );
				break;
			default:
				throw new AssertionFailure("Unrecognized key direction");
		}
		for ( int i = 0; i < previousKeys.size(); i++ ) {
			final SqmPath keyPath = previousKeys.get(i);
			predicate = builder.and( predicate, keyPath.equalTo( keyValues.get(i) ) );
		}
		return predicate;
	}

}
