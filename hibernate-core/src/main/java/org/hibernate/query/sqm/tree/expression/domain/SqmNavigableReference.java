/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.domain;

import org.hibernate.query.sqm.tree.domain.SqmPath;

/**
 * Specialization of {@link SqmPath} for paths that are not explicitly defined
 * in the from-clause ({@link org.hibernate.query.sqm.tree.from.SqmFrom}}
 *
 * @author Steve Ebersole
 */
public interface SqmNavigableReference extends SqmPath {
}
