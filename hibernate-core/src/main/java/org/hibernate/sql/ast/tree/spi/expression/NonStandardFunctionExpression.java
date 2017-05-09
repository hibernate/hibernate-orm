/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.ast.tree.spi.expression;

import java.util.Arrays;
import java.util.List;

import org.hibernate.sql.ast.tree.spi.select.Selectable;
import org.hibernate.sql.ast.tree.spi.select.SelectableBasicTypeImpl;
import org.hibernate.sql.ast.tree.spi.select.SqlSelectable;
import org.hibernate.sql.ast.consume.results.internal.SqlSelectionReaderImpl;
import org.hibernate.sql.ast.consume.results.spi.SqlSelectionReader;
import org.hibernate.sql.ast.consume.spi.SqlSelectAstToJdbcSelectConverter;
import org.hibernate.type.spi.BasicType;
import org.hibernate.type.spi.Type;

/**
 * Represents a call to a function other than one of the standardized ones.
 *
 * @author Steve Ebersole
 */
public class NonStandardFunctionExpression implements Expression, SqlSelectable {
	private final String functionName;
	private final List<Expression> arguments;
	private final BasicType resultType;

	private final Selectable selectable;

	public NonStandardFunctionExpression(
			String functionName,
			List<Expression> arguments,
			BasicType resultType) {
		this.functionName = functionName;
		this.arguments = arguments;
		this.resultType = resultType;

		this.selectable = new SelectableBasicTypeImpl( this, this, resultType );
	}

	public NonStandardFunctionExpression(
			String functionName,
			BasicType resultType,
			Expression... arguments) {
		this(
				functionName,
				Arrays.asList( arguments ),
				resultType
		);
	}

	public String getFunctionName() {
		return functionName;
	}

	public List<Expression> getArguments() {
		return arguments;
	}

	@Override
	public Type getType() {
		return resultType;
	}

	@Override
	public void accept(SqlSelectAstToJdbcSelectConverter walker) {
		walker.visitNonStandardFunctionExpression( this );
	}

	@Override
	public Selectable getSelectable() {
		return selectable;
	}

	@Override
	public SqlSelectionReader getSqlSelectionReader() {
		return new SqlSelectionReaderImpl( (BasicType) getType() );
	}
}
