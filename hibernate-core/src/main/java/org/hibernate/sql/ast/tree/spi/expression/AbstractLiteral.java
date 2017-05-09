/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.ast.tree.spi.expression;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.queryable.spi.BasicValuedExpressableType;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.sql.ast.tree.spi.select.Selectable;
import org.hibernate.sql.ast.tree.spi.select.SqlSelectable;
import org.hibernate.sql.ast.produce.result.internal.ReturnScalarImpl;
import org.hibernate.sql.ast.produce.result.spi.Return;
import org.hibernate.sql.ast.produce.result.spi.ReturnResolutionContext;
import org.hibernate.sql.ast.consume.results.internal.SqlSelectionReaderImpl;
import org.hibernate.sql.ast.consume.results.spi.SqlSelectionReader;
import org.hibernate.sql.ast.consume.spi.JdbcParameterBinder;
import org.hibernate.type.spi.BasicType;

/**
 * We classify literals different based on their source so that we can handle then differently
 * when rendering SQL.  This class offers convenience for those implementations
 * <p/>
 * Can function as a ParameterBinder for cases where we want to treat literals using bind parameters.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractLiteral
		implements JdbcParameterBinder, Expression, SqlSelectable, Selectable {
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
	public Selectable getSelectable() {
		return this;
	}

	@Override
	public Expression getSelectedExpression() {
		return this;
	}

	@Override
	public Return toQueryReturn(ReturnResolutionContext returnResolutionContext, String resultVariable) {
		return new ReturnScalarImpl(
				this,
				returnResolutionContext.resolveSqlSelection( this ),
				resultVariable,
				getType()
		);
	}

	@Override
	public SqlSelectionReader getSqlSelectionReader() {
		return new SqlSelectionReaderImpl( (BasicType) getType() );
	}

	@Override
	public int bindParameterValue(
			PreparedStatement statement,
			int startPosition,
			QueryParameterBindings queryParameterBindings,
			SharedSessionContractImplementor session) throws SQLException {
		getType().
		getType().nullSafeSet( statement, getValue(), startPosition, session );
		return getType().getColumnSpan();
	}
}
