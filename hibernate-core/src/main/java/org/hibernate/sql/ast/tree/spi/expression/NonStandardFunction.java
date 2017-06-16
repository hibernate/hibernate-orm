/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.ast.tree.spi.expression;

import java.util.Arrays;
import java.util.List;

import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.sql.exec.results.internal.SqlSelectionReaderImpl;
import org.hibernate.sql.exec.results.spi.SqlSelectionReader;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.tree.internal.BasicValuedNonNavigableSelection;
import org.hibernate.sql.ast.tree.spi.select.Selectable;
import org.hibernate.sql.ast.tree.spi.select.Selection;
import org.hibernate.type.spi.BasicType;

/**
 * Represents a call to a function other than one of the standardized ones.
 *
 * @author Steve Ebersole
 */
public class NonStandardFunction implements Function {
	private final String functionName;
	private final List<Expression> arguments;
	private final AllowableFunctionReturnType resultType;

	public NonStandardFunction(
			String functionName,
			AllowableFunctionReturnType resultType,
			List<Expression> arguments) {
		this.functionName = functionName;
		this.arguments = arguments;
		this.resultType = resultType;
	}

	public NonStandardFunction(
			String functionName,
			AllowableFunctionReturnType resultType,
			Expression... arguments) {
		this(
				functionName,
				resultType,
				Arrays.asList( arguments )
		);
	}

	public String getFunctionName() {
		return functionName;
	}

	public List<Expression> getArguments() {
		return arguments;
	}

	@Override
	public AllowableFunctionReturnType getType() {
		return resultType;
	}

	@Override
	public void accept(SqlAstWalker  walker) {
		walker.visitNonStandardFunctionExpression( this );
	}

	@Override
	public Selectable getSelectable() {
		return this;
	}

	@Override
	public SqlSelectionReader getSqlSelectionReader() {
		return new SqlSelectionReaderImpl( (BasicType) getType() );
	}

	@Override
	public Selection createSelection(Expression selectedExpression, String resultVariable) {
		return new BasicValuedNonNavigableSelection(
				selectedExpression,
				resultVariable,
				this
		);
	}
}
