/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.cte;

import java.util.List;

import org.hibernate.query.sqm.NullPrecedence;
import org.hibernate.query.sqm.SortOrder;
import org.hibernate.sql.ast.tree.cte.CteMaterialization;
import org.hibernate.sql.ast.tree.cte.CteSearchClauseKind;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.AbstractSqmNode;
import org.hibernate.query.sqm.tree.SqmStatement;
import org.hibernate.query.sqm.tree.SqmVisitableNode;

/**
 * @author Steve Ebersole
 * @author Christian Beikov
 */
public class SqmCteStatement<T> extends AbstractSqmNode implements SqmVisitableNode {
	private final SqmCteContainer cteContainer;
	private final SqmCteTable cteTable;
	private final CteMaterialization materialization;
	private final SqmStatement<?> cteDefinition;
	private final CteSearchClauseKind searchClauseKind;
	private final List<SqmSearchClauseSpecification> searchBySpecifications;
	private final List<SqmCteTableColumn> cycleColumns;
	private final SqmCteTableColumn cycleMarkColumn;
	private final char cycleValue;
	private final char noCycleValue;

	public SqmCteStatement(
			SqmCteTable cteTable,
			SqmStatement<?> cteDefinition,
			CteMaterialization materialization,
			NodeBuilder nodeBuilder) {
		super( nodeBuilder );
		this.cteTable = cteTable;
		this.cteDefinition = cteDefinition;
		this.materialization = materialization;
		this.cteContainer = null;
		this.searchClauseKind = null;
		this.searchBySpecifications = null;
		this.cycleColumns = null;
		this.cycleMarkColumn = null;
		this.cycleValue = '\0';
		this.noCycleValue = '\0';
	}

	public SqmCteStatement(
			SqmCteTable cteTable,
			SqmStatement<?> cteDefinition,
			CteMaterialization materialization,
			SqmCteContainer cteContainer) {
		super( cteContainer.nodeBuilder() );
		this.cteTable = cteTable;
		this.cteDefinition = cteDefinition;
		this.materialization = materialization;
		this.cteContainer = cteContainer;
		this.searchClauseKind = null;
		this.searchBySpecifications = null;
		this.cycleColumns = null;
		this.cycleMarkColumn = null;
		this.cycleValue = '\0';
		this.noCycleValue = '\0';
	}

	public SqmCteTable getCteTable() {
		return cteTable;
	}

	public SqmStatement<?> getCteDefinition() {
		return cteDefinition;
	}

	public SqmCteContainer getCteContainer() {
		return cteContainer;
	}

	public CteMaterialization getMaterialization() {
		return materialization;
	}

	public CteSearchClauseKind getSearchClauseKind() {
		return searchClauseKind;
	}

	public List<SqmSearchClauseSpecification> getSearchBySpecifications() {
		return searchBySpecifications;
	}

	public List<SqmCteTableColumn> getCycleColumns() {
		return cycleColumns;
	}

	public SqmCteTableColumn getCycleMarkColumn() {
		return cycleMarkColumn;
	}

	public char getCycleValue() {
		return cycleValue;
	}

	public char getNoCycleValue() {
		return noCycleValue;
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitCteStatement( this );
	}

	public void appendHqlString(StringBuilder sb) {
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
		sb.append( '(' );
		getCteDefinition().appendHqlString( sb );
		sb.append( ')' );

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
			for ( SqmSearchClauseSpecification searchBySpecification : getSearchBySpecifications() ) {
				sb.append( separator );
				sb.append( searchBySpecification.getCteColumn().getColumnName() );
				if ( searchBySpecification.getSortOrder() != null ) {
					if ( searchBySpecification.getSortOrder() == SortOrder.ASCENDING ) {
						sb.append( " asc" );
					}
					else {
						sb.append( " desc" );
					}
					if ( searchBySpecification.getNullPrecedence() != null ) {
						if ( searchBySpecification.getNullPrecedence() == NullPrecedence.FIRST ) {
							sb.append( " nulls first" );
						}
						else {
							sb.append( " nulls last" );
						}
					}
				}
				separator = ", ";
			}
		}
		if ( getCycleMarkColumn() != null ) {
			sb.append( " cycle " );
			separator = "";
			for ( SqmCteTableColumn cycleColumn : getCycleColumns() ) {
				sb.append( separator );
				sb.append( cycleColumn.getColumnName() );
				separator = ", ";
			}
			sb.append( " set " );
			sb.append( getCycleMarkColumn().getColumnName() );
			sb.append( " to '" );
			sb.append( getCycleValue() );
			sb.append( "' default '" );
			sb.append( getNoCycleValue() );
			sb.append( "'" );
		}
	}
}
