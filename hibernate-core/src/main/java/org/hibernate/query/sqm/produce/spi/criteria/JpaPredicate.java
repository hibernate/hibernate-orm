/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.spi.criteria;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;

import org.hibernate.query.sqm.ParsingException;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;

/**
 * @author Steve Ebersole
 */
public interface JpaPredicate extends Predicate, JpaExpression<Boolean> {
	CriteriaBuilder criteriaBuilder();

	@Override
	JpaPredicate not();

	SqmPredicate visitPredicate(CriteriaVisitor visitor);

	@Override
	default SqmExpression visitExpression(CriteriaVisitor visitor) {
		throw new ParsingException( "Unexpected call to visit predicate as an expression" );
	}
}
