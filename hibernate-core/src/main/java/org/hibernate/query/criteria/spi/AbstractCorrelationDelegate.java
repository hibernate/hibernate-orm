/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

import org.hibernate.query.criteria.JpaSubQuery;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractCorrelationDelegate<O,T> extends AbstractFrom<O,T> implements CorrelationDelegate<O,T> {
	private final FromImplementor<O, T> correlationParent;

	public AbstractCorrelationDelegate(
			FromImplementor<O,T> correlationParent,
			CriteriaNodeBuilder criteriaBuilder) {
		super( correlationParent.getManagedType(), correlationParent.getSource(), criteriaBuilder );

		this.correlationParent = correlationParent;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Correlation

	@Override
	public FromImplementor<O, T> getCorrelationParent() {
		return correlationParent;
	}

	@Override
	public boolean isCorrelated() {
		return true;
	}
}
