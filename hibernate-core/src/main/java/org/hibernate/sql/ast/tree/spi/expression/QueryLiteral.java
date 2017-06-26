/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.expression;

import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.type.spi.BasicType;

/**
 * A literal specified in the source query.
 *
 * @author Steve Ebersole
 */
public class QueryLiteral extends AbstractLiteral {
	public QueryLiteral(Object value, BasicValuedExpressableType expressableType, boolean inSelect) {
		super( value, expressableType, inSelect );
	}

	@Override
	public BasicType getType() {
		return (BasicType) super.getType();
	}

	@Override
	public void accept(SqlAstWalker  walker) {
		walker.visitQueryLiteral( this );
	}
}
