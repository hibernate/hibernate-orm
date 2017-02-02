/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa;

import javax.persistence.Query;

/**
 * Marker interface for Hibernate generated JPA queries so that we can access the underlying Hibernate query objects.
 *
 * @author Gavin King
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
public interface HibernateQuery extends Query {
	/**
	 * Gives access to the underlying Hibernate query object..
	 *
	 * @return THe Hibernate query object.
	 */
	public org.hibernate.Query getHibernateQuery();
}
