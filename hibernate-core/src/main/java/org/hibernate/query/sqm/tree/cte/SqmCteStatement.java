/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.cte;

import java.util.List;

import org.hibernate.CteSearchClauseKind;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.AbstractSqmNode;
import org.hibernate.query.sqm.tree.SqmStatement;
import org.hibernate.query.sqm.tree.SqmVisitableNode;
import org.hibernate.query.sqm.tree.select.SqmQueryPart;

/**
 * @author Steve Ebersole
 * @author Christian Beikov
 */
public class SqmCteStatement<T> extends AbstractSqmNode implements SqmVisitableNode {
	private final SqmCteContainer cteContainer;
	private final SqmCteTable cteTable;
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
			NodeBuilder nodeBuilder) {
		super( nodeBuilder );
		this.cteTable = cteTable;
		this.cteDefinition = cteDefinition;
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
			SqmCteContainer cteContainer) {
		super( cteContainer.nodeBuilder() );
		this.cteTable = cteTable;
		this.cteDefinition = cteDefinition;
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
}
