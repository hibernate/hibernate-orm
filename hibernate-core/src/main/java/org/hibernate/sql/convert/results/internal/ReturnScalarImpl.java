/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.convert.results.internal;

import org.hibernate.sql.ast.expression.Expression;
import org.hibernate.sql.ast.select.SqlSelection;
import org.hibernate.sql.convert.results.spi.ReturnScalar;
import org.hibernate.sql.exec.results.process.internal.ReturnAssemblerScalar;
import org.hibernate.sql.exec.results.process.spi.ReturnAssembler;
import org.hibernate.type.spi.Type;

/**
 * @author Steve Ebersole
 */
public class ReturnScalarImpl implements ReturnScalar {
	private final Expression selectedExpression;
	private final String resultVariable;
	private final Type type;

	private final ReturnAssembler assembler;

	public ReturnScalarImpl(
			Expression selectedExpression,
			SqlSelection sqlSelection,
			String resultVariable,
			Type type) {
		this.selectedExpression = selectedExpression;
		this.resultVariable = resultVariable;
		this.type = type;

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
		return getType().getReturnedClass();
	}

	@Override
	public Type getType() {
		return type;
	}

	@Override
	public ReturnAssembler getReturnAssembler() {
		return assembler;
	}
}
