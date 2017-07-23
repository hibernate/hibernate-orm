/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.internal.select;

import org.hibernate.metamodel.model.domain.spi.EmbeddedTypeDescriptor;
import org.hibernate.sql.ast.tree.spi.select.QueryResultComposite;
import org.hibernate.sql.exec.results.internal.QueryResultAssemblerComposite;
import org.hibernate.sql.exec.results.spi.QueryResultAssembler;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class QueryResultCompositeImpl implements QueryResultComposite {
	private final EmbeddedTypeDescriptor embeddedDescriptor;
	private final String selectedExpressionText;
	private final String resultVariable;

	private final QueryResultAssembler assembler;

	public QueryResultCompositeImpl(
			String selectedExpressionText,
			String resultVariable,
			EmbeddedTypeDescriptor<?> embeddedPersister) {
		this.embeddedDescriptor = embeddedPersister;
		this.selectedExpressionText = selectedExpressionText;
		this.resultVariable = resultVariable;

		this.assembler = new QueryResultAssemblerComposite(
				this,
				null,
				embeddedPersister
		);
	}

	@Override
	public String getSelectedExpressionDescription() {
		return selectedExpressionText;
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return embeddedDescriptor.getJavaTypeDescriptor();
	}

	@Override
	public String getResultVariable() {
		return resultVariable;
	}

	@Override
	public QueryResultAssembler getResultAssembler() {
		return assembler;
	}
}
