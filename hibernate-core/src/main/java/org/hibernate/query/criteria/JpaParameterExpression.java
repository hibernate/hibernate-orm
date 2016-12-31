/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria;

import javax.persistence.criteria.ParameterExpression;

import org.hibernate.Incubating;
import org.hibernate.query.QueryParameter;
import org.hibernate.sqm.parser.criteria.tree.JpaExpression;

/**
 * Hibernate ORM specialization of the JPA {@link javax.persistence.criteria.ParameterExpression}
 * contract.
 *
 * @author Steve Ebersole
 *
 * @since 6.0
 */
@Incubating
public interface JpaParameterExpression<T> extends QueryParameter<T>, JpaExpression<T>, ParameterExpression<T> {
}
