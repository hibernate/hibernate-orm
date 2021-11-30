/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.insert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.tree.AbstractMutationStatement;
import org.hibernate.sql.ast.tree.MutationStatement;
import org.hibernate.sql.ast.tree.cte.CteStatement;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.select.QueryPart;

/**
 * @author Steve Ebersole
 */
public class InsertStatement extends AbstractMutationStatement {

	private List<ColumnReference> targetColumnReferences;
	private QueryPart sourceSelectStatement;
	private List<Values> valuesList = new ArrayList<>();

	public InsertStatement(TableReference targetTable) {
		super( targetTable );
	}

	public InsertStatement(TableReference targetTable, List<ColumnReference> returningColumns) {
		super( new LinkedHashMap<>(), targetTable, returningColumns );
	}

	public InsertStatement(boolean withRecursive, Map<String, CteStatement> cteStatements, TableReference targetTable, List<ColumnReference> returningColumns) {
		super( cteStatements, targetTable, returningColumns );
		setWithRecursive( withRecursive );
	}

	public List<ColumnReference> getTargetColumnReferences() {
		return targetColumnReferences == null ? Collections.emptyList() : targetColumnReferences;
	}

	public void addTargetColumnReferences(ColumnReference... references) {
		if ( targetColumnReferences == null ) {
			targetColumnReferences = new ArrayList<>();
		}

		Collections.addAll( this.targetColumnReferences, references );
	}

	public void addTargetColumnReferences(List<ColumnReference> references) {
		if ( targetColumnReferences == null ) {
			targetColumnReferences = new ArrayList<>();
		}

		this.targetColumnReferences.addAll( references );
	}

	public QueryPart getSourceSelectStatement() {
		return sourceSelectStatement;
	}

	public void setSourceSelectStatement(QueryPart sourceSelectStatement) {
		this.sourceSelectStatement = sourceSelectStatement;
	}

	public List<Values> getValuesList() {
		return valuesList;
	}

	public void setValuesList(List<Values> valuesList) {
		this.valuesList = valuesList;
	}

	@Override
	public void accept(SqlAstWalker walker) {
		walker.visitInsertStatement( this );
	}
}
