/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.spi;

import org.hibernate.jpa.HibernateEntityManagerFactory;

/**
 * Internal contact for things that have {@link HibernateEntityManagerFactory} access.
 *
 * @author <a href="mailto:stliu@hibernate.org">Strong Liu</a>
 *
 * @deprecated (since 5.2) Why do we need an over-arching access to HibernateEntityManagerFactory across
 * multiple contract hierarchies?
 */
@Deprecated
public interface HibernateEntityManagerFactoryAware {
	/**
	 * Get access to the Hibernate extended EMF contract.
	 *
	 * @return The Hibernate EMF contract for this EM.
	 */
	HibernateEntityManagerFactory getFactory();
}
