/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.exec.results.internal;

import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;
import org.hibernate.sql.exec.results.spi.QueryResultAssembler;
import org.hibernate.sql.exec.results.spi.QueryResultScalar;
import org.hibernate.sql.exec.results.spi.SqlSelection;

/**
 * @author Steve Ebersole
 */
public class QueryResultScalarImpl implements QueryResultScalar {
	private final String resultVariable;
	private final BasicValuedExpressableType expressableType;

	private final QueryResultAssembler assembler;

	public QueryResultScalarImpl(
			String resultVariable,
			SqlSelection sqlSelection,
			BasicValuedExpressableType expressableType) {
		this.resultVariable = resultVariable;
		this.expressableType = expressableType;

		this.assembler = new QueryResultAssemblerScalar( sqlSelection, this );
	}

	@Override
	public String getResultVariable() {
		return resultVariable;
	}

	@Override
	public BasicValuedExpressableType getType() {
		return expressableType;
	}

	@Override
	public QueryResultAssembler getResultAssembler() {
		return assembler;
	}
}
