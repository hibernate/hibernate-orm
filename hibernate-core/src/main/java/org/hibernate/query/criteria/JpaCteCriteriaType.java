/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.criteria;

import java.util.List;

import org.hibernate.Incubating;
import org.hibernate.metamodel.model.domain.DomainType;

/**
 * A CTE (common table expression) criteria type.
 */
@Incubating
public interface JpaCteCriteriaType<T> extends JpaCriteriaNode {

	/**
	 * The name under which this CTE is registered.
	 */
	String getName();

	/**
	 * The domain type of the CTE.
	 */
	DomainType<T> getType();

	/**
	 * The attributes of the CTE type.
	 */
	List<JpaCteCriteriaAttribute> getAttributes();

	/**
	 * Returns the found attribute or null.
	 */
	JpaCteCriteriaAttribute getAttribute(String name);
}
