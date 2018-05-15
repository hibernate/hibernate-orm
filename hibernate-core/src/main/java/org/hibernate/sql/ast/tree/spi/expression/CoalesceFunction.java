/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.expression;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;

/**
 * @author Steve Ebersole
 */
public class CoalesceFunction extends AbstractStandardFunction {
	private List<Expression> values = new ArrayList<>();

	public List<Expression> getValues() {
		return values;
	}

	public void value(Expression expression) {
		values.add( expression );
	}

	@Override
	public SqlExpressableType getExpressableType() {
		return getType();
	}

	@Override
	public SqlExpressableType getType() {
		return values.get( 0 ).getType();
	}

	@Override
	public void accept(SqlAstWalker  walker) {
		walker.visitCoalesceFunction( this );
	}
}
