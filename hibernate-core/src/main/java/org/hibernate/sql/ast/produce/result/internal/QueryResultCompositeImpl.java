/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.result.internal;

import org.hibernate.metamodel.model.domain.spi.EmbeddedTypeDescriptor;
import org.hibernate.sql.ast.consume.results.internal.QueryResultAssemblerComposite;
import org.hibernate.sql.ast.consume.results.spi.QueryResultAssembler;
import org.hibernate.sql.ast.produce.result.spi.QueryResultComposite;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;

/**
 * @author Steve Ebersole
 */
public class QueryResultCompositeImpl implements QueryResultComposite {
	private final NavigableReference selectedExpression;
	private final String resultVariable;

	private final EmbeddedTypeDescriptor persister;

	private final QueryResultAssembler assembler;

	public QueryResultCompositeImpl(
			NavigableReference selectedExpression,
			String resultVariable,
			EmbeddedTypeDescriptor<?> embeddedPersister) {
		this.selectedExpression = selectedExpression;
		this.resultVariable = resultVariable;
		this.persister = embeddedPersister;

		this.assembler = new QueryResultAssemblerComposite(
				this,
				null,
				embeddedPersister
		);
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
		return persister.getJavaType();
	}

	@Override
	public QueryResultAssembler getResultAssembler() {
		return assembler;
	}
}
