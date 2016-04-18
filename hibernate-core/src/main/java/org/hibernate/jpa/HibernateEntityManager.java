/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa;

import javax.persistence.EntityManager;

import org.hibernate.Session;

/**
 * Additional contract for Hibernate implementations of {@link javax.persistence.EntityManager} providing access to various Hibernate
 * specific functionality.
 *
 * @author Gavin King
 */
public interface HibernateEntityManager extends EntityManager {
	/**
	 * Retrieve a reference to the Hibernate {@link org.hibernate.Session} used by this {@link javax.persistence.EntityManager}.
	 *
	 * @return The session
	 */
	public Session getSession();
}
