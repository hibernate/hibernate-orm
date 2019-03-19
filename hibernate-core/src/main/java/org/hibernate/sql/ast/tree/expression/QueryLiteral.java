/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.expression;

import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;

/**
 * A literal specified in the source query.
 *
 * @author Steve Ebersole
 */
public class QueryLiteral extends AbstractLiteral {
	public QueryLiteral(Object value, SqlExpressableType expressableType, Clause clause) {
		super( value, expressableType, clause );
	}

	@Override
	public int getNumberOfJdbcParametersNeeded() {
		return 1;
	}

	@Override
	public void accept(SqlAstWalker  walker) {
		walker.visitQueryLiteral( this );
	}
}
