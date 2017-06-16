/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.result.internal;

import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.ast.tree.spi.select.SqlSelection;
import org.hibernate.sql.ast.produce.result.spi.QueryResultScalar;
import org.hibernate.sql.exec.results.internal.QueryResultAssemblerScalar;
import org.hibernate.sql.exec.results.spi.QueryResultAssembler;

/**
 * @author Steve Ebersole
 */
public class QueryResultScalarImpl implements QueryResultScalar {
	private final Expression selectedExpression;
	private final String resultVariable;
	private final BasicValuedExpressableType expressableType;

	private final QueryResultAssembler assembler;

	public QueryResultScalarImpl(
			Expression selectedExpression,
			SqlSelection sqlSelection,
			String resultVariable,
			BasicValuedExpressableType expressableType) {
		this.selectedExpression = selectedExpression;
		this.resultVariable = resultVariable;
		this.expressableType = expressableType;

		// note : just a single SqlSelection because this describes a scalar return

		this.assembler = new QueryResultAssemblerScalar( sqlSelection, this );
	}

	@Override
	public Expression getSelectedExpression() {
		return selectedExpression;
	}

	@Override
	public String getResultVariable() {
		return resultVariable;
	}

	@Override
	public Class getReturnedJavaType() {
		return getType().getJavaTypeDescriptor().getJavaType();
	}

	@Override
	public ExpressableType getType() {
		return expressableType;
	}

	@Override
	public QueryResultAssembler getResultAssembler() {
		return assembler;
	}
}
