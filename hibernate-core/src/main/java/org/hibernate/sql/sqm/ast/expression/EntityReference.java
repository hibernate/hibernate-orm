/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.sqm.ast.expression;

import org.hibernate.sql.sqm.ast.from.ColumnBinding;
import org.hibernate.sql.sqm.convert.spi.SqlTreeWalker;
import org.hibernate.type.spi.Type;

/**
 * @author Andrea Boriero
 */
public class EntityReference extends SelfReadingExpressionSupport {
	private final Type ormType;
	private final ColumnBinding[]  columnBindings;

	public EntityReference(Type ormType, ColumnBinding[] columnBindings) {
		this.ormType = ormType;
		this.columnBindings = columnBindings;
	}

	@Override
	public Type getType() {
		return ormType;
	}

	public ColumnBinding[] getColumnBindings() {
		return columnBindings;
	}

	@Override
	public void accept(SqlTreeWalker sqlTreeWalker) {
		sqlTreeWalker.visitEntityExpression( this );
	}
}
