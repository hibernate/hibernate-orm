/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

import java.util.Collection;
import javax.persistence.criteria.Expression;

import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaInPredicate;
import org.hibernate.query.criteria.JpaPredicate;

/**
 * SPI-level contract for JpaExpression
 *
 * @author Steve Ebersole
 */
public interface ExpressionImplementor<T> extends SelectionImplementor<T>, JpaExpression<T> {

	@Override
	default JpaPredicate isNull() {
		return nodeBuilder().isNull( this );
	}

	@Override
	default JpaPredicate isNotNull() {
		return nodeBuilder().isNotNull( this );
	}

	@Override
	default JpaInPredicate in(Object... values) {
		return nodeBuilder().in( this, values );
	}

	default JpaInPredicate in(JpaExpression<?>... values) {
		return nodeBuilder().in( this, values );
	}

	@Override
	default JpaInPredicate in(Expression<?>... values) {
		return in( (JpaExpression<?>[]) values );
	}

	@Override
	default JpaInPredicate in(Collection<?> values) {
		return nodeBuilder().in( this, values );
	}

	default JpaInPredicate in(JpaExpression<Collection<?>> values) {
		return nodeBuilder().in( this, values );
	}

	@Override
	default JpaInPredicate in(Expression<Collection<?>> values) {
		return in( (JpaExpression<Collection<?>>) values );
	}
}
