/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.naming;

/**
 * Context for determining the implicit name of an entity's primary table
 *
 * @author Steve Ebersole
 */
public interface ImplicitEntityNameSource extends ImplicitNameSource {
	/**
	 * Access to the entity's name information
	 *
	 * @return The entity's name information
	 */
	public EntityNaming getEntityNaming();
}
