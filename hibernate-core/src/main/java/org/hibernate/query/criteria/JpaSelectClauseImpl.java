/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria;

import org.hibernate.sqm.parser.criteria.tree.select.JpaSelectClause;
import org.hibernate.sqm.parser.criteria.tree.select.JpaSelection;

/**
 * @author Steve Ebersole
 */
public class JpaSelectClauseImpl<T> implements JpaSelectClause<T> {
	private boolean distinct;
	private JpaSelectionImplementor<? extends T> jpaSelection;

	@Override
	public boolean isDistinct() {
		return distinct;
	}

	public void setDistinct(boolean distinct) {
		this.distinct = distinct;
	}

	@Override
	public JpaSelection<? extends T> getSelection() {
		return jpaSelection;
	}

	public void setJpaSelection(JpaSelectionImplementor<? extends T> jpaSelection) {
		this.jpaSelection = jpaSelection;
	}
}
