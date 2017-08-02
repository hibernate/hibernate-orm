/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.exec.results.internal;

import org.hibernate.metamodel.model.domain.spi.EmbeddedValuedNavigable;
import org.hibernate.sql.ast.produce.metamodel.spi.EmbeddedValueExpressableType;
import org.hibernate.sql.exec.results.spi.QueryResultAssembler;
import org.hibernate.sql.exec.results.spi.QueryResultComposite;

/**
 * @author Steve Ebersole
 */
public class QueryResultCompositeImpl implements QueryResultComposite {
	private final EmbeddedValuedNavigable navigable;
	private final String resultVariable;

	private final QueryResultAssembler assembler;

	public QueryResultCompositeImpl(
			String resultVariable,
			EmbeddedValuedNavigable navigable) {
		this.navigable = navigable;
		this.resultVariable = resultVariable;

		this.assembler = new QueryResultAssemblerComposite(
				this,
				null,
				navigable.getEmbeddedDescriptor()
		);
	}

	@Override
	public String getResultVariable() {
		return resultVariable;
	}

	@Override
	public EmbeddedValueExpressableType getType() {
		return navigable;
	}

	@Override
	public QueryResultAssembler getResultAssembler() {
		return assembler;
	}
}
