/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria;

import javax.persistence.criteria.CriteriaBuilder;

import org.hibernate.Incubating;

/**
 * Hibernate ORM specialization of the JPA {@link CriteriaBuilder.SimpleCase}
 * contract.
 *
 * @author Steve Ebersole
 *
 * @since 6.0
 */
@Incubating
public interface JpaSimpleCase<C, R> extends CriteriaBuilder.SimpleCase<C, R>, JpaExpressionImplementor<R> {
}
