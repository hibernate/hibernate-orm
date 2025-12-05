/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.cte;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.query.criteria.JpaCteCriteria;
import org.hibernate.query.criteria.JpaCteCriteriaAttribute;
import org.hibernate.query.criteria.JpaCteCriteriaType;
import org.hibernate.query.criteria.JpaSearchOrder;
import org.hibernate.query.SortDirection;
import org.hibernate.query.sqm.tree.SqmCacheable;
import org.hibernate.query.sqm.tree.SqmRenderContext;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmLiteral;
import org.hibernate.query.sqm.tree.select.SqmSelectQuery;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.query.sqm.tree.select.SqmSubQuery;
import org.hibernate.sql.ast.tree.cte.CteMaterialization;
import org.hibernate.sql.ast.tree.cte.CteSearchClauseKind;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.AbstractSqmNode;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmVisitableNode;

import jakarta.persistence.criteria.AbstractQuery;
import jakarta.persistence.criteria.Subquery;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;

/**
 * @author Steve Ebersole
 * @author Christian Beikov
 */
public class SqmCteStatement<T> extends AbstractSqmNode implements SqmVisitableNode, JpaCteCriteria<T> {
	private final SqmCteContainer cteContainer;
	private final SqmCteTable<T> cteTable;
	private SqmSelectQuery<?> cteDefinition;
	private CteMaterialization materialization;
	private @Nullable CteSearchClauseKind searchClauseKind;
	private List<SqmSearchClauseSpecification> searchBySpecifications;
	private @Nullable String searchAttributeName;
	private List<SqmCteTableColumn> cycleAttributes;
	private @Nullable String cycleMarkAttributeName;
	private @Nullable String cyclePathAttributeName;
	private @Nullable SqmLiteral<Object> cycleValue;
	private @Nullable SqmLiteral<Object> noCycleValue;

	// Need to suppress some Checker Framework errors, because passing the `this` reference is unsafe,
	// though we make it safe by not calling any methods on it until initialization finishes
	@SuppressWarnings({"uninitialized", "argument"})
	public SqmCteStatement(
			String name,
			SqmSelectQuery<T> cteDefinition,
			SqmCteContainer cteContainer,
			NodeBuilder nodeBuilder) {
		super( nodeBuilder );
		this.cteDefinition = cteDefinition;
		this.cteContainer = cteContainer;
		this.materialization = CteMaterialization.UNDEFINED;
		this.searchBySpecifications = Collections.emptyList();
		this.cycleAttributes = Collections.emptyList();
		this.cteTable = SqmCteTable.createStatementTable( name, this, cteDefinition );
	}

	// Need to suppress some Checker Framework errors, because passing the `this` reference is unsafe,
	// though we make it safe by not calling any methods on it until initialization finishes
	@SuppressWarnings({"uninitialized", "argument"})
	public SqmCteStatement(
			String name,
			SqmSelectQuery<T> nonRecursiveQueryPart,
			boolean unionDistinct,
			Function<JpaCteCriteria<T>, AbstractQuery<T>> finalCriteriaProducer,
			SqmCteContainer cteContainer,
			NodeBuilder nodeBuilder) {
		super( nodeBuilder );
		this.cteContainer = cteContainer;
		this.materialization = CteMaterialization.UNDEFINED;
		this.searchBySpecifications = Collections.emptyList();
		this.cycleAttributes = Collections.emptyList();
		this.cteTable = SqmCteTable.createStatementTable( name, this, nonRecursiveQueryPart );
		final AbstractQuery<T> recursiveQueryPart = finalCriteriaProducer.apply( this );
		if ( nonRecursiveQueryPart instanceof Subquery<?> ) {
			if ( unionDistinct ) {
				this.cteDefinition = (SqmSelectQuery<?>) nodeBuilder.union(
						(SqmSubQuery<T>) nonRecursiveQueryPart,
						(SqmSubQuery<T>) recursiveQueryPart
				);
			}
			else {
				this.cteDefinition = (SqmSelectQuery<?>) nodeBuilder.unionAll(
						(SqmSubQuery<T>) nonRecursiveQueryPart,
						(SqmSubQuery<T>) recursiveQueryPart
				);
			}
		}
		else {
			if ( unionDistinct ) {
				this.cteDefinition = (SqmSelectQuery<?>) nodeBuilder.union(
						(SqmSelectStatement<T>) nonRecursiveQueryPart,
						(SqmSelectStatement<T>) recursiveQueryPart
				);
			}
			else {
				this.cteDefinition = (SqmSelectQuery<?>) nodeBuilder.unionAll(
						(SqmSelectStatement<T>) nonRecursiveQueryPart,
						(SqmSelectStatement<T>) recursiveQueryPart
				);
			}
		}
	}

	private SqmCteStatement(
			NodeBuilder builder,
			SqmCteContainer cteContainer,
			SqmCteTable<T> cteTable,
			CteMaterialization materialization,
			@Nullable CteSearchClauseKind searchClauseKind,
			List<SqmSearchClauseSpecification> searchBySpecifications,
			@Nullable String searchAttributeName,
			List<SqmCteTableColumn> cycleAttributes,
			@Nullable String cycleMarkAttributeName,
			@Nullable String cyclePathAttributeName,
			@Nullable SqmLiteral<Object> cycleValue,
			@Nullable SqmLiteral<Object> noCycleValue) {
		super( builder );
		this.cteContainer = cteContainer;
		this.cteTable = cteTable;
		this.materialization = materialization;
		this.searchClauseKind = searchClauseKind;
		this.searchBySpecifications = searchBySpecifications;
		this.searchAttributeName = searchAttributeName;
		this.cycleAttributes = cycleAttributes;
		this.cycleMarkAttributeName = cycleMarkAttributeName;
		this.cyclePathAttributeName = cyclePathAttributeName;
		this.cycleValue = cycleValue;
		this.noCycleValue = noCycleValue;
	}

	@Override
	public SqmCteStatement<T> copy(SqmCopyContext context) {
		final SqmCteStatement<T> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmCteStatement<T> copy = context.registerCopy(
				this,
				new SqmCteStatement<>(
						nodeBuilder(),
						cteContainer,
						cteTable,
						materialization,
						searchClauseKind,
						searchBySpecifications,
						searchAttributeName,
						cycleAttributes,
						cycleMarkAttributeName,
						cyclePathAttributeName,
						cycleValue == null ? null : cycleValue.copy( context ),
						noCycleValue == null ? null : noCycleValue.copy( context )
				)
		);
		// We have to copy the definition object after registering the copy of this because for recursive CTEs
		// the select query from clause may contain the current cte statement itself
		copy.cteDefinition = cteDefinition.copy( context );
		return copy;
	}

	@Override
	public @Nullable String getName() {
		return cteTable.getName();
	}

	public SqmCteTable<?> getCteTable() {
		return cteTable;
	}

	@Override
	public SqmSelectQuery<?> getCteDefinition() {
		return cteDefinition;
	}

	@Override
	public SqmCteContainer getCteContainer() {
		return cteContainer;
	}

	@Override
	public CteMaterialization getMaterialization() {
		return materialization;
	}

	@Override
	public void setMaterialization(CteMaterialization materialization) {
		this.materialization = materialization;
	}

	@Override
	public @Nullable CteSearchClauseKind getSearchClauseKind() {
		return searchClauseKind;
	}

	@Override
	public List<JpaSearchOrder> getSearchBySpecifications() {
		//noinspection unchecked
		return (List<JpaSearchOrder>) (List<?>) searchBySpecifications;
	}

	@Override
	public @Nullable String getSearchAttributeName() {
		return searchAttributeName;
	}

	@Override
	public List<JpaCteCriteriaAttribute> getCycleAttributes() {
		//noinspection unchecked
		return (List<JpaCteCriteriaAttribute>) (List<?>) cycleAttributes;
	}

	@Override
	public @Nullable String getCycleMarkAttributeName() {
		return cycleMarkAttributeName;
	}

	@Override
	public @Nullable String getCyclePathAttributeName() {
		return cyclePathAttributeName;
	}

	@Override
	public @Nullable Object getCycleValue() {
		return cycleValue == null ? null : cycleValue.getLiteralValue();
	}

	@Override
	public @Nullable Object getNoCycleValue() {
		return noCycleValue == null ? null : noCycleValue.getLiteralValue();
	}

	public @Nullable SqmLiteral<Object> getCycleLiteral() {
		return cycleValue;
	}

	public @Nullable SqmLiteral<Object> getNoCycleLiteral() {
		return noCycleValue;
	}

	@Override
	public JpaCteCriteriaType<T> getType() {
		return cteTable;
	}

	@Override
	public void search(CteSearchClauseKind kind, String searchAttributeName, List<JpaSearchOrder> searchOrders) {
		if ( kind == null || searchAttributeName == null || searchOrders == null || searchOrders.isEmpty() ) {
			this.searchClauseKind = null;
			this.searchBySpecifications = Collections.emptyList();
			this.searchAttributeName = null;
		}
		else {
			final List<JpaSearchOrder> orders = new ArrayList<>( searchOrders.size() );
			for ( JpaSearchOrder order : searchOrders ) {
				if ( !cteTable.getAttributes().contains( order.getAttribute() ) ) {
					throw new IllegalArgumentException(
							"Illegal search order attribute '" +
									( order.getAttribute() == null ? "null" : order.getAttribute().getName() ) +
									"' passed, which is not part of the JpaCteCriteria!"
					);
				}
				orders.add( order );
			}
			this.searchClauseKind = kind;
			this.searchAttributeName = searchAttributeName;
			//noinspection unchecked
			this.searchBySpecifications = (List<SqmSearchClauseSpecification>) (List<?>) orders;
		}
	}

	@Override
	public <X> void cycleUsing(
			String cycleMarkAttributeName,
			String cyclePathAttributeName,
			X cycleValue,
			X noCycleValue,
			List<JpaCteCriteriaAttribute> cycleAttributes) {
		if ( cycleMarkAttributeName == null || cycleAttributes == null || cycleAttributes.isEmpty() ) {
			this.cycleMarkAttributeName = null;
			this.cyclePathAttributeName = null;
			this.cycleValue = null;
			this.noCycleValue = null;
			this.cycleAttributes = Collections.emptyList();
		}
		else {
			if ( cycleValue == null || noCycleValue == null ) {
				throw new IllegalArgumentException( "Null is an illegal value for cycle mark values!" );
			}
			final SqmExpression<X> cycleValueLiteral = nodeBuilder().literal( cycleValue );
			final SqmExpression<X> noCycleValueLiteral = nodeBuilder().literal( noCycleValue );
			if ( cycleValueLiteral.getNodeType() != noCycleValueLiteral.getNodeType() ) {
				throw new IllegalArgumentException( "Inconsistent types for cycle mark values: [" + cycleValueLiteral.getNodeType() + ", " + noCycleValueLiteral.getNodeType() + "]" );
			}
			final List<SqmCteTableColumn> attributes = new ArrayList<>( cycleAttributes.size() );
			for ( JpaCteCriteriaAttribute cycleAttribute : cycleAttributes ) {
				if ( !cteTable.getAttributes().contains( cycleAttribute ) ) {
					throw new IllegalArgumentException(
							"Illegal cycle attribute '" +
									( cycleAttribute == null ? "null" : cycleAttribute.getName() ) +
									"' passed, which is not part of the JpaCteCriteria!"
					);
				}
				attributes.add( (SqmCteTableColumn) cycleAttribute );
			}
			this.cycleMarkAttributeName = cycleMarkAttributeName;
			this.cyclePathAttributeName = cyclePathAttributeName;
			this.cycleValue = (SqmLiteral<Object>) cycleValueLiteral;
			this.noCycleValue = (SqmLiteral<Object>) noCycleValueLiteral;
			this.cycleAttributes = attributes;
		}
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitCteStatement( this );
	}

	@Override
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		if ( cteTable.getName() == null ) {
			hql.append( "generated_" );
		}
		hql.append( cteTable.getCteName() );
		hql.append( " (" );
		final List<SqmCteTableColumn> columns = cteTable.getColumns();
		hql.append( columns.get( 0 ).getColumnName() );
		for ( int i = 1; i < columns.size(); i++ ) {
			hql.append( ", " );
			hql.append( columns.get( i ).getColumnName() );
		}

		hql.append( ") as " );

		if ( getMaterialization() != CteMaterialization.UNDEFINED ) {
			hql.append( getMaterialization() ).append( ' ' );
		}
		if ( getCteDefinition() instanceof SqmSubQuery<?> subQuery ) {
			subQuery.appendHqlString( hql, context );
		}
		else if ( getCteDefinition() instanceof SqmSelectStatement<?> selectStatement ) {
			hql.append( '(' );
			selectStatement.appendHqlString( hql, context );
			hql.append( ')' );
		}
		String separator;
		if ( getSearchClauseKind() != null ) {
			hql.append( " search " );
			if ( getSearchClauseKind() == CteSearchClauseKind.DEPTH_FIRST ) {
				hql.append( " depth " );
			}
			else {
				hql.append( " breadth " );
			}
			hql.append( " first by " );
			separator = "";
			for ( JpaSearchOrder searchBySpecification : getSearchBySpecifications() ) {
				hql.append( separator );
				hql.append( searchBySpecification.getAttribute().getName() );
				if ( searchBySpecification.getSortOrder() != null ) {
					if ( searchBySpecification.getSortOrder() == SortDirection.ASCENDING ) {
						hql.append( " asc" );
					}
					else {
						hql.append( " desc" );
					}
					if ( searchBySpecification.getNullPrecedence() != null ) {
						switch ( searchBySpecification.getNullPrecedence() ) {
							case FIRST:
								hql.append( " nulls first" );
								break;
							case LAST:
								hql.append( " nulls last" );
								break;
						}
					}
				}
				separator = ", ";
			}
			hql.append( " set " );
			hql.append( getSearchAttributeName() );
		}
		if ( getCycleMarkAttributeName() != null ) {
			hql.append( " cycle " );
			separator = "";
			for ( JpaCteCriteriaAttribute cycleColumn : getCycleAttributes() ) {
				hql.append( separator );
				hql.append( cycleColumn.getName() );
				separator = ", ";
			}
			hql.append( " set " );
			hql.append( getCycleMarkAttributeName() );
			hql.append( " to " );
			castNonNull( getCycleLiteral() ).appendHqlString( hql, context );
			hql.append( " default " );
			castNonNull( getNoCycleLiteral() ).appendHqlString( hql, context );
			final String cyclePathAttributeName = getCyclePathAttributeName();
			if ( cyclePathAttributeName != null ) {
				hql.append( " using " );
				hql.append( cyclePathAttributeName );
			}
		}
	}

	@Override
	public boolean equals(@Nullable Object object) {
		return object instanceof SqmCteStatement<?> that
			&& cteTable.equals( that.cteTable )
			&& cteDefinition.equals( that.cteDefinition )
			&& materialization == that.materialization
			&& searchClauseKind == that.searchClauseKind
			&& Objects.equals( searchBySpecifications, that.searchBySpecifications )
			&& Objects.equals( searchAttributeName, that.searchAttributeName )
			&& Objects.equals( cycleAttributes, that.cycleAttributes )
			&& Objects.equals( cycleMarkAttributeName, that.cycleMarkAttributeName )
			&& Objects.equals( cyclePathAttributeName, that.cyclePathAttributeName )
			&& Objects.equals( cycleValue, that.cycleValue )
			&& Objects.equals( noCycleValue, that.noCycleValue );
	}

	@Override
	public int hashCode() {
		int result = cteTable.hashCode();
		result = 31 * result + cteDefinition.hashCode();
		result = 31 * result + materialization.hashCode();
		result = 31 * result + Objects.hashCode( searchClauseKind );
		result = 31 * result + Objects.hashCode( searchBySpecifications );
		result = 31 * result + Objects.hashCode( searchAttributeName );
		result = 31 * result + Objects.hashCode( cycleAttributes );
		result = 31 * result + Objects.hashCode( cycleMarkAttributeName );
		result = 31 * result + Objects.hashCode( cyclePathAttributeName );
		result = 31 * result + Objects.hashCode( cycleValue );
		result = 31 * result + Objects.hashCode( noCycleValue );
		return result;
	}

	@Override
	public boolean isCompatible(Object object) {
		return object instanceof SqmCteStatement<?> that
			&& cteTable.isCompatible( that.cteTable )
			&& cteDefinition.isCompatible( that.cteDefinition )
			&& materialization == that.materialization
			&& searchClauseKind == that.searchClauseKind
			&& SqmCacheable.areCompatible( searchBySpecifications, that.searchBySpecifications )
			&& Objects.equals( searchAttributeName, that.searchAttributeName )
			&& SqmCacheable.areCompatible( cycleAttributes, that.cycleAttributes )
			&& Objects.equals( cycleMarkAttributeName, that.cycleMarkAttributeName )
			&& Objects.equals( cyclePathAttributeName, that.cyclePathAttributeName )
			&& SqmCacheable.areCompatible( cycleValue, that.cycleValue )
			&& SqmCacheable.areCompatible( noCycleValue, that.noCycleValue );
	}

	@Override
	public int cacheHashCode() {
		int result = cteTable.cacheHashCode();
		result = 31 * result + cteDefinition.cacheHashCode();
		result = 31 * result + materialization.hashCode();
		result = 31 * result + Objects.hashCode( searchClauseKind );
		result = 31 * result + SqmCacheable.cacheHashCode( searchBySpecifications );
		result = 31 * result + Objects.hashCode( searchAttributeName );
		result = 31 * result + SqmCacheable.cacheHashCode( cycleAttributes );
		result = 31 * result + Objects.hashCode( cycleMarkAttributeName );
		result = 31 * result + Objects.hashCode( cyclePathAttributeName );
		result = 31 * result + SqmCacheable.cacheHashCode( cycleValue );
		result = 31 * result + SqmCacheable.cacheHashCode( noCycleValue );
		return result;
	}

}
