/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

import org.hibernate.query.criteria.QueryableCriteria;

/**
 * Common SPI-level contract for {@link org.hibernate.query.criteria.JpaCriteriaQuery}
 * and {@link org.hibernate.query.criteria.JpaManipulationCriteria}
 *
 * @author Steve Ebersole
 */
public interface Criteria extends QueryableCriteria, CriteriaNode {
}
