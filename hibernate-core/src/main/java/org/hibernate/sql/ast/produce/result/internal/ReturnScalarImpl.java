/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.result.internal;

import org.hibernate.persister.queryable.spi.BasicValuedExpressableType;
import org.hibernate.persister.queryable.spi.ExpressableType;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.ast.tree.spi.select.SqlSelection;
import org.hibernate.sql.ast.produce.result.spi.ReturnScalar;
import org.hibernate.sql.ast.consume.results.internal.ReturnAssemblerScalar;
import org.hibernate.sql.ast.consume.results.spi.ReturnAssembler;

/**
 * @author Steve Ebersole
 */
public class ReturnScalarImpl implements ReturnScalar {
	private final Expression selectedExpression;
	private final String resultVariable;
	private final BasicValuedExpressableType expressableType;

	private final ReturnAssembler assembler;

	public ReturnScalarImpl(
			Expression selectedExpression,
			SqlSelection sqlSelection,
			String resultVariable,
			BasicValuedExpressableType expressableType) {
		this.selectedExpression = selectedExpression;
		this.resultVariable = resultVariable;
		this.expressableType = expressableType;

		this.assembler = new ReturnAssemblerScalar( sqlSelection, this );
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
	public ReturnAssembler getReturnAssembler() {
		return assembler;
	}
}
