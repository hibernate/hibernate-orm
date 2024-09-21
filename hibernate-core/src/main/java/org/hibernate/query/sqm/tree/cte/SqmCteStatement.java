/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.cte;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import org.hibernate.query.criteria.JpaCteCriteria;
import org.hibernate.query.criteria.JpaCteCriteriaAttribute;
import org.hibernate.query.criteria.JpaCteCriteriaType;
import org.hibernate.query.criteria.JpaSearchOrder;
import org.hibernate.query.SortDirection;
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

/**
 * @author Steve Ebersole
 * @author Christian Beikov
 */
public class SqmCteStatement<T> extends AbstractSqmNode implements SqmVisitableNode, JpaCteCriteria<T> {
	private final SqmCteContainer cteContainer;
	private final SqmCteTable<T> cteTable;
	private SqmSelectQuery<?> cteDefinition;
	private CteMaterialization materialization;
	private CteSearchClauseKind searchClauseKind;
	private List<JpaSearchOrder> searchBySpecifications;
	private String searchAttributeName;
	private List<JpaCteCriteriaAttribute> cycleAttributes;
	private String cycleMarkAttributeName;
	private String cyclePathAttributeName;
	private SqmLiteral<Object> cycleValue;
	private SqmLiteral<Object> noCycleValue;

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
			SqmSelectQuery<?> cteDefinition,
			CteMaterialization materialization,
			CteSearchClauseKind searchClauseKind,
			List<JpaSearchOrder> searchBySpecifications,
			String searchAttributeName,
			List<JpaCteCriteriaAttribute> cycleAttributes,
			String cycleMarkAttributeName,
			String cyclePathAttributeName,
			SqmLiteral<Object> cycleValue,
			SqmLiteral<Object> noCycleValue) {
		super( builder );
		this.cteContainer = cteContainer;
		this.cteTable = cteTable;
		this.cteDefinition = cteDefinition;
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
						null,
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
	public String getName() {
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
	public CteSearchClauseKind getSearchClauseKind() {
		return searchClauseKind;
	}

	@Override
	public List<JpaSearchOrder> getSearchBySpecifications() {
		return searchBySpecifications;
	}

	@Override
	public String getSearchAttributeName() {
		return searchAttributeName;
	}

	@Override
	public List<JpaCteCriteriaAttribute> getCycleAttributes() {
		return cycleAttributes;
	}

	@Override
	public String getCycleMarkAttributeName() {
		return cycleMarkAttributeName;
	}

	@Override
	public String getCyclePathAttributeName() {
		return cyclePathAttributeName;
	}

	@Override
	public Object getCycleValue() {
		return cycleValue == null ? null : cycleValue.getLiteralValue();
	}

	@Override
	public Object getNoCycleValue() {
		return noCycleValue == null ? null : noCycleValue.getLiteralValue();
	}

	public SqmLiteral<Object> getCycleLiteral() {
		return cycleValue;
	}

	public SqmLiteral<Object> getNoCycleLiteral() {
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
			this.searchBySpecifications = orders;
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
			final List<JpaCteCriteriaAttribute> attributes = new ArrayList<>( cycleAttributes.size() );
			for ( JpaCteCriteriaAttribute cycleAttribute : cycleAttributes ) {
				if ( !cteTable.getAttributes().contains( cycleAttribute ) ) {
					throw new IllegalArgumentException(
							"Illegal cycle attribute '" +
									( cycleAttribute == null ? "null" : cycleAttribute.getName() ) +
									"' passed, which is not part of the JpaCteCriteria!"
					);
				}
				attributes.add( cycleAttribute );
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
	public void appendHqlString(StringBuilder sb) {
		if ( cteTable.getName() == null ) {
			sb.append( "generated_" );
		}
		sb.append( cteTable.getCteName() );
		sb.append( " (" );
		final List<SqmCteTableColumn> columns = cteTable.getColumns();
		sb.append( columns.get( 0 ).getColumnName() );
		for ( int i = 1; i < columns.size(); i++ ) {
			sb.append( ", " );
			sb.append( columns.get( i ).getColumnName() );
		}

		sb.append( ") as " );

		if ( getMaterialization() != CteMaterialization.UNDEFINED ) {
			sb.append( getMaterialization() ).append( ' ' );
		}
		if ( getCteDefinition() instanceof SqmSubQuery<?> ) {
			( (SqmSubQuery<?>) getCteDefinition() ).appendHqlString( sb );
		}
		else {
			sb.append( '(' );
			( (SqmSelectStatement<?>) getCteDefinition() ).appendHqlString( sb );
			sb.append( ')' );
		}
		String separator;
		if ( getSearchClauseKind() != null ) {
			sb.append( " search " );
			if ( getSearchClauseKind() == CteSearchClauseKind.DEPTH_FIRST ) {
				sb.append( " depth " );
			}
			else {
				sb.append( " breadth " );
			}
			sb.append( " first by " );
			separator = "";
			for ( JpaSearchOrder searchBySpecification : getSearchBySpecifications() ) {
				sb.append( separator );
				sb.append( searchBySpecification.getAttribute().getName() );
				if ( searchBySpecification.getSortOrder() != null ) {
					if ( searchBySpecification.getSortOrder() == SortDirection.ASCENDING ) {
						sb.append( " asc" );
					}
					else {
						sb.append( " desc" );
					}
					if ( searchBySpecification.getNullPrecedence() != null ) {
						switch ( searchBySpecification.getNullPrecedence() ) {
							case FIRST:
								sb.append( " nulls first" );
								break;
							case LAST:
								sb.append( " nulls last" );
								break;
						}
					}
				}
				separator = ", ";
			}
			sb.append( " set " );
			sb.append( getSearchAttributeName() );
		}
		if ( getCycleMarkAttributeName() != null ) {
			sb.append( " cycle " );
			separator = "";
			for ( JpaCteCriteriaAttribute cycleColumn : getCycleAttributes() ) {
				sb.append( separator );
				sb.append( cycleColumn.getName() );
				separator = ", ";
			}
			sb.append( " set " );
			sb.append( getCycleMarkAttributeName() );
			sb.append( " to " );
			getCycleLiteral().appendHqlString( sb );
			sb.append( " default " );
			getNoCycleLiteral().appendHqlString( sb );
			if ( getCyclePathAttributeName() != null ) {
				sb.append( " using " );
				sb.append( getCyclePathAttributeName() );
			}
		}
	}
}
