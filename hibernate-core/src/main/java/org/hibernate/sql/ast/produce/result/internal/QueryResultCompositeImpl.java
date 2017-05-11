/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.result.internal;

import java.util.List;

import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.ast.tree.spi.select.SelectableEmbeddedTypeImpl;
import org.hibernate.sql.ast.tree.spi.select.SqlSelection;
import org.hibernate.sql.ast.produce.result.spi.QueryResultComposite;
import org.hibernate.sql.ast.consume.results.internal.QueryResultAssemblerComposite;
import org.hibernate.sql.ast.consume.results.spi.QueryResultAssembler;
import org.hibernate.type.spi.EmbeddedType;

/**
 * @author Steve Ebersole
 */
public class QueryResultCompositeImpl implements QueryResultComposite {
	private final SelectableEmbeddedTypeImpl selectableEmbeddedType;
	private final String resultVariable;
	private final EmbeddedType compositeType;

	private final QueryResultAssembler assembler;

	public QueryResultCompositeImpl(
			SelectableEmbeddedTypeImpl selectableEmbeddedType,
			String resultVariable,
			List<SqlSelection> sqlSelections,
			EmbeddedType compositeType) {
		this.selectableEmbeddedType = selectableEmbeddedType;
		this.resultVariable = resultVariable;
		this.compositeType = compositeType;

		this.assembler = new QueryResultAssemblerComposite(
				this,
				sqlSelections,
				compositeType
		);
	}

	@Override
	public Expression getSelectedExpression() {
		return selectableEmbeddedType.getSelectedExpression();
	}

	@Override
	public String getResultVariable() {
		return resultVariable;
	}

	@Override
	public Class getReturnedJavaType() {
		return compositeType.getReturnedClass();
	}

	@Override
	public QueryResultAssembler getResultAssembler() {
		return assembler;
	}
}
