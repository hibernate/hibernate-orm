/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.cte;

import java.util.List;

import org.hibernate.CteSearchClauseKind;
import org.hibernate.sql.ast.tree.Statement;

/**
 * A statement using a CTE
 *
 * @author Steve Ebersole
 * @author Christian Beikov
 */
public class CteStatement {
	private final CteTable cteTable;
	private final Statement cteDefinition;
	private final CteSearchClauseKind searchClauseKind;
	private final List<SearchClauseSpecification> searchBySpecifications;
	private final List<CteColumn> cycleColumns;
	private final CteColumn cycleMarkColumn;
	private final char cycleValue;
	private final char noCycleValue;

	public CteStatement(CteTable cteTable, Statement cteDefinition) {
		this.cteDefinition = cteDefinition;
		this.cteTable = cteTable;
		this.searchClauseKind = null;
		this.searchBySpecifications = null;
		this.cycleColumns = null;
		this.cycleMarkColumn = null;
		this.cycleValue = '\0';
		this.noCycleValue = '\0';
	}

	public CteStatement(
			CteTable cteTable,
			Statement cteDefinition,
			CteSearchClauseKind searchClauseKind,
			List<SearchClauseSpecification> searchBySpecifications,
			List<CteColumn> cycleColumns,
			CteColumn cycleMarkColumn,
			char cycleValue,
			char noCycleValue) {
		this.cteTable = cteTable;
		this.cteDefinition = cteDefinition;
		this.searchClauseKind = searchClauseKind;
		this.searchBySpecifications = searchBySpecifications;
		this.cycleColumns = cycleColumns;
		this.cycleMarkColumn = cycleMarkColumn;
		this.cycleValue = cycleValue;
		this.noCycleValue = noCycleValue;
	}

	public CteTable getCteTable() {
		return cteTable;
	}

	public Statement getCteDefinition() {
		return cteDefinition;
	}

	public CteSearchClauseKind getSearchClauseKind() {
		return searchClauseKind;
	}

	public List<SearchClauseSpecification> getSearchBySpecifications() {
		return searchBySpecifications;
	}

	public List<CteColumn> getCycleColumns() {
		return cycleColumns;
	}

	public CteColumn getCycleMarkColumn() {
		return cycleMarkColumn;
	}

	public char getCycleValue() {
		return cycleValue;
	}

	public char getNoCycleValue() {
		return noCycleValue;
	}
}
