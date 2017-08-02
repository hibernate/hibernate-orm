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
import org.hibernate.sql.ast.tree.internal.BasicValuedNonNavigableSelection;
import org.hibernate.sql.ast.tree.spi.select.Selectable;
import org.hibernate.sql.ast.tree.spi.select.Selection;
import org.hibernate.sql.exec.results.spi.SqlSelectable;
import org.hibernate.sql.exec.results.internal.SqlSelectionReaderImpl;
import org.hibernate.sql.exec.results.spi.SqlSelectionReader;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.sql.exec.spi.ParameterBindingContext;

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
	public Selection createSelection(Expression selectedExpression, String resultVariable) {
		return new BasicValuedNonNavigableSelection( selectedExpression, resultVariable, this );
	}


	@Override
	public SqlSelectionReader getSqlSelectionReader() {
		return new SqlSelectionReaderImpl( getType() );
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
