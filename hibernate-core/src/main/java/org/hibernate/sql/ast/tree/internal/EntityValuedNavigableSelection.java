/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.internal;

import org.hibernate.persister.entity.spi.EntityPersister;
import org.hibernate.sql.ast.produce.result.internal.QueryResultEntityImpl;
import org.hibernate.sql.ast.produce.result.spi.QueryResultGenerator;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReferenceExpression;

/**
 * @author Steve Ebersole
 */
public class EntityValuedNavigableSelection extends AbstractSelection implements NavigableSelection {
	private final EntityPersister persister;

	public EntityValuedNavigableSelection(
			NavigableReferenceExpression selectedExpression,
			EntityPersister persister,
			String resultVariable) {
		super( selectedExpression, resultVariable );
		this.persister = persister;
	}

	@Override
	public NavigableReferenceExpression getSelectedExpression() {
		return (NavigableReferenceExpression) super.getSelectedExpression();
	}

	@Override
	protected QueryResultGenerator getQueryResultGenerator() {
		return (c,s,r) -> new QueryResultEntityImpl(
				getSelectedExpression(),
				persister,
				getResultVariable(),
				// todo (6.0) : build this Map<?,SqlSelectionGroup>
				null,
				getSelectedExpression().getNavigablePath(),
				getSelectedExpression().getSourceTableGroup().getUid()
		);
	}
}
