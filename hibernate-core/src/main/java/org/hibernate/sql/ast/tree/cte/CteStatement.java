/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.tree.cte;

import java.util.List;

import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.expression.Literal;

/**
 * A statement using a CTE
 *
 * @author Steve Ebersole
 * @author Christian Beikov
 */
public class CteStatement {
	private final CteTable cteTable;
	private final Statement cteDefinition;
	private final CteMaterialization materialization;
	private final CteSearchClauseKind searchClauseKind;
	private final List<SearchClauseSpecification> searchBySpecifications;
	private final CteColumn searchColumn;
	private final List<CteColumn> cycleColumns;
	private final CteColumn cycleMarkColumn;
	private final CteColumn cyclePathColumn;
	private final Literal cycleValue;
	private final Literal noCycleValue;
	private boolean recursive;

	public CteStatement(CteTable cteTable, Statement cteDefinition) {
		this( cteTable, cteDefinition, CteMaterialization.UNDEFINED );
	}

	public CteStatement(CteTable cteTable, Statement cteDefinition, CteMaterialization materialization) {
		this.cteDefinition = cteDefinition;
		this.cteTable = cteTable;
		this.materialization = materialization;
		this.searchClauseKind = null;
		this.searchBySpecifications = null;
		this.searchColumn = null;
		this.cycleColumns = null;
		this.cycleMarkColumn = null;
		this.cyclePathColumn = null;
		this.cycleValue = null;
		this.noCycleValue = null;
	}

	public CteStatement(
			CteTable cteTable,
			Statement cteDefinition,
			CteMaterialization materialization,
			CteSearchClauseKind searchClauseKind,
			List<SearchClauseSpecification> searchBySpecifications,
			CteColumn searchColumn,
			List<CteColumn> cycleColumns,
			CteColumn cycleMarkColumn,
			CteColumn cyclePathColumn,
			Literal cycleValue,
			Literal noCycleValue) {
		this.cteTable = cteTable;
		this.cteDefinition = cteDefinition;
		this.materialization = materialization;
		this.searchClauseKind = searchClauseKind;
		this.searchBySpecifications = searchBySpecifications;
		this.searchColumn = searchColumn;
		this.cycleColumns = cycleColumns;
		this.cycleMarkColumn = cycleMarkColumn;
		this.cyclePathColumn = cyclePathColumn;
		this.cycleValue = cycleValue;
		this.noCycleValue = noCycleValue;
	}

	public CteTable getCteTable() {
		return cteTable;
	}

	public Statement getCteDefinition() {
		return cteDefinition;
	}

	public CteMaterialization getMaterialization() {
		return materialization;
	}

	public CteSearchClauseKind getSearchClauseKind() {
		return searchClauseKind;
	}

	public List<SearchClauseSpecification> getSearchBySpecifications() {
		return searchBySpecifications;
	}

	public CteColumn getSearchColumn() {
		return searchColumn;
	}

	public List<CteColumn> getCycleColumns() {
		return cycleColumns;
	}

	public CteColumn getCycleMarkColumn() {
		return cycleMarkColumn;
	}

	public CteColumn getCyclePathColumn() {
		return cyclePathColumn;
	}

	public Literal getCycleValue() {
		return cycleValue;
	}

	public Literal getNoCycleValue() {
		return noCycleValue;
	}

	public boolean isRecursive() {
		return recursive;
	}

	public void setRecursive() {
		this.recursive = true;
	}
}
