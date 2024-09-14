/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.criteria;

import jakarta.persistence.criteria.CriteriaSelect;

/**
 * Extension of the JPA {@link CriteriaSelect}.
 *
 * @apiNote This is different from {@link JpaSelectCriteria}.  JPA added
 * {@link CriteriaSelect} in version 3.2 while {@link JpaSelectCriteria} has
 * existed for many releases prior.  JPA's {@link CriteriaSelect} is intended
 * for supporting its newly added set operations (union, interest, except).
 *
 * @author Steve Ebersole
 */
public interface JpaCriteriaSelect<T> extends CriteriaSelect<T> {

}
