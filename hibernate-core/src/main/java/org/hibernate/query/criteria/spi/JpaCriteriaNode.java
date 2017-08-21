/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

/**
 * Base for all JPA Criteria tree nodes (anything from selections, expressions,
 * predicates, etc).
 *
 * @author Steve Ebersole
 */
public interface JpaCriteriaNode {
	JpaCriteriaBuilderImplementor getCriteriaBuilder();
}
