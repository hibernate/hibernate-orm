/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.internal.select;

import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.metamodel.model.domain.spi.PluralPersistentAttribute;
import org.hibernate.sql.NotYetImplementedException;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;
import org.hibernate.sql.ast.tree.spi.select.QueryResultCollection;
import org.hibernate.sql.ast.tree.spi.select.QueryResultCreationContext;
import org.hibernate.sql.ast.tree.spi.select.SqlSelectionResolver;
import org.hibernate.sql.exec.results.spi.QueryResultAssembler;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class QueryResultCollectionImpl extends AbstractCollectionReference implements QueryResultCollection {
	public QueryResultCollectionImpl(
			NavigableReference selectedExpression,
			String resultVariable,
			SqlSelectionResolver sqlSelectionResolver,
			QueryResultCreationContext creationContext) {
		super( selectedExpression, resultVariable );

		assert selectedExpression.getNavigable() instanceof PluralPersistentAttribute
				|| selectedExpression.getNavigable() instanceof PersistentCollectionDescriptor;
	}

	@Override
	public String getSelectedExpressionDescription() {
		return getNavigableReference().toString();
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return getNavigableReference().getType().getJavaTypeDescriptor();
	}

	@Override
	public String getResultVariable() {
		return super.getResultVariable();
	}

	@Override
	public QueryResultAssembler getResultAssembler() {
		throw new NotYetImplementedException(  );
	}
}
