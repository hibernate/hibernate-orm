/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.spi;

import org.hibernate.boot.model.naming.EntityNaming;

/**
 * Naming information about an entity.
 *
 * @author Steve Ebersole
 */
public interface EntityNamingSource extends EntityNaming {
	/**
	 * Decode the name that we should expect to be used elsewhere to reference
	 * the modeled entity by decoding the entity-name/class-name combo.
	 *
	 * @return The reference-able type name
	 */
	public String getTypeName();
}
