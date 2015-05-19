/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.naming;

/**
 * Context for determining the implicit name of an entity's discriminator column.
 *
 * @author Steve Ebersole
 */
public interface ImplicitDiscriminatorColumnNameSource extends ImplicitNameSource {
	/**
	 * Access the naming for the entity
	 *
	 * @return The naming for the entity
	 */
	public EntityNaming getEntityNaming();
}
