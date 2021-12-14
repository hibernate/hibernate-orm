/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.from;

import java.util.List;
import java.util.function.Function;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.tree.select.QueryPart;

/**
 * @author Christian Beikov
 */
public class QueryPartTableReference extends DerivedTableReference {

	private final QueryPart queryPart;

	public QueryPartTableReference(
			QueryPart queryPart,
			String identificationVariable,
			List<String> columnNames,
			SessionFactoryImplementor sessionFactory) {
		super( identificationVariable, columnNames, sessionFactory );
		this.queryPart = queryPart;
	}

	public QueryPart getQueryPart() {
		return queryPart;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitQueryPartTableReference( this );
	}

	@Override
	public Boolean visitAffectedTableNames(Function<String, Boolean> nameCollector) {
		final Function<TableReference, Boolean> tableReferenceBooleanFunction =
				tableReference -> tableReference.visitAffectedTableNames( nameCollector );
		return queryPart.queryQuerySpecs(
			querySpec -> querySpec.getFromClause().queryTableReferences( tableReferenceBooleanFunction )
		);
	}
}
