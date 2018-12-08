/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

import org.hibernate.query.criteria.JpaFrom;
import org.hibernate.query.criteria.JpaSubQuery;

/**
 * SPI-level contract for {@link org.hibernate.query.criteria.JpaFrom}
 * implementors
 *
 * @author Steve Ebersole
 */
public interface FromImplementor<O,T> extends PathImplementor<T>, JpaFrom<O,T>, PathSourceImplementor<T> {
	@Override
	FromImplementor<O, T> getCorrelationParent();

	@Override
	FromImplementor<O, T> correlateTo(JpaSubQuery<T> subquery);
}
