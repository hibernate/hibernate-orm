/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.expression;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;
import org.hibernate.sql.ast.produce.spi.SqlExpressable;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.sql.exec.spi.ParameterBindingContext;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.sql.results.spi.SqlSelection;

/**
 * We classify literals different based on their source so that we can handle then differently
 * when rendering SQL.  This class offers convenience for those implementations
 * <p/>
 * Can function as a ParameterBinder for cases where we want to treat literals using bind parameters.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractLiteral
		implements JdbcParameterBinder, Expression, SqlExpressable {
	private final Object value;
	private final BasicValuedExpressableType type;
	private final boolean inSelect;

	public AbstractLiteral(Object value, BasicValuedExpressableType type, boolean inSelect) {
		this.value = value;
		this.type = type;
		this.inSelect = inSelect;
	}

	public Object getValue() {
		return value;
	}

	@Override
	public BasicValuedExpressableType getType() {
		return type;
	}

	public boolean isInSelect() {
		return inSelect;
	}

	@Override
	public SqlSelection createSqlSelection(int jdbcPosition) {
		// todo (6.0) : for literals and parameters consider simply pushing these values directly into the "current JDBC values" array
		//		rather than reading them (the same value over and over) from the ResultSet.
		//
		//		see `org.hibernate.sql.ast.tree.spi.expression.AbstractParameter.createSqlSelection`

		return new SqlSelectionImpl(
				getType().getBasicType().getSqlSelectionReader(),
				jdbcPosition
		);
	}

	@Override
	@SuppressWarnings("unchecked")
	public int bindParameterValue(
			PreparedStatement statement,
			int startPosition,
			ParameterBindingContext context) throws SQLException {
		getType().getBasicType()
				.getColumnDescriptor()
				.getSqlTypeDescriptor()
				.getBinder( getType().getJavaTypeDescriptor() )
				.bind( statement, value, startPosition, context.getSession() );
		return getType().getNumberOfJdbcParametersToBind();
	}
}
