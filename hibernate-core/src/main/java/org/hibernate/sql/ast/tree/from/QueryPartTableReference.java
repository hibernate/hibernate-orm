/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.tree.from;

import java.util.List;
import java.util.function.Function;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.tree.select.QueryPart;
import org.hibernate.sql.ast.tree.select.SelectStatement;

/**
 * A table reference for a query part.
 *
 * @author Christian Beikov
 */
public class QueryPartTableReference extends DerivedTableReference {

	private final SelectStatement selectStatement;

	public QueryPartTableReference(
			SelectStatement selectStatement,
			String identificationVariable,
			List<String> columnNames,
			boolean lateral,
			SessionFactoryImplementor sessionFactory) {
		super( identificationVariable, columnNames, lateral, sessionFactory );
		this.selectStatement = selectStatement;
	}

	public QueryPart getQueryPart() {
		return selectStatement.getQueryPart();
	}

	public SelectStatement getStatement() {
		return selectStatement;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitQueryPartTableReference( this );
	}

	@Override
	public Boolean visitAffectedTableNames(Function<String, Boolean> nameCollector) {
		final Function<TableReference, Boolean> tableReferenceBooleanFunction =
				tableReference -> tableReference.visitAffectedTableNames( nameCollector );
		return selectStatement.getQueryPart().queryQuerySpecs(
			querySpec -> querySpec.getFromClause().queryTableReferences( tableReferenceBooleanFunction )
		);
	}
}
