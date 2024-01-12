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
import java.util.function.BiConsumer;

import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.tree.AbstractMutationStatement;
import org.hibernate.sql.ast.tree.cte.CteContainer;
import org.hibernate.sql.ast.tree.cte.CteStatement;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.select.QueryPart;

/**
 * todo (6.2) - Would much prefer to split insert-values and
 * 		insert-select into individual contracts - something like
 * 		`InsertStatement` and `InsertSelectStatement` e.g.
 * 		Would help alleviate much of the duplication in handling
 * 		between inserts from SQM and those from model mutation
 *
 * @author Steve Ebersole
 */
public class InsertSelectStatement extends AbstractMutationStatement implements InsertStatement {

	public static final String DEFAULT_ALIAS = "to_insert_";
	private List<ColumnReference> targetColumnReferences;
	private QueryPart sourceSelectStatement;
	private List<Values> valuesList = new ArrayList<>();
	private ConflictClause conflictClause;

	public InsertSelectStatement(NamedTableReference targetTable) {
		this( null, targetTable, Collections.emptyList() );
	}

	public InsertSelectStatement(NamedTableReference targetTable, List<ColumnReference> returningColumns) {
		this( null, targetTable, returningColumns );
	}

	public InsertSelectStatement(
			CteContainer cteContainer,
			NamedTableReference targetTable,
			List<ColumnReference> returningColumns) {
		super( cteContainer, targetTable, returningColumns );
	}

	@Override
	public List<ColumnReference> getTargetColumns() {
		return targetColumnReferences == null ? Collections.emptyList() : targetColumnReferences;
	}

	@Override
	public void forEachTargetColumn(BiConsumer<Integer, ColumnReference> consumer) {
		if ( targetColumnReferences == null ) {
			return;
		}

		for ( int i = 0; i < targetColumnReferences.size(); i++ ) {
			consumer.accept( i, targetColumnReferences.get( i ) );
		}
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

	public ConflictClause getConflictClause() {
		return conflictClause;
	}

	public void setConflictClause(ConflictClause conflictClause) {
		this.conflictClause = conflictClause;
	}

	@Override
	public void accept(SqlAstWalker walker) {
		walker.visitInsertStatement( this );
	}
}
