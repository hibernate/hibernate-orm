/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.expression;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.query.spi.QueryParameterBinding;
import org.hibernate.sql.ast.consume.spi.JdbcParameterBinder;
import org.hibernate.sql.ast.produce.sqm.spi.ParameterSpec;
import org.hibernate.sql.ast.tree.spi.select.Selectable;
import org.hibernate.sql.ast.tree.spi.select.SqlSelectable;

/**
 * @author Steve Ebersole
 */
public interface GenericParameter extends ParameterSpec, JdbcParameterBinder, Expression, SqlSelectable, Selectable {
	@Override
	JdbcParameterBinder getParameterBinder();

	@Override
	int bindParameterValue(
			PreparedStatement statement,
			int startPosition,
			ParameterBindingContext context) throws SQLException;

	QueryParameterBinding resolveBinding(ParameterBindingContext context);
}
