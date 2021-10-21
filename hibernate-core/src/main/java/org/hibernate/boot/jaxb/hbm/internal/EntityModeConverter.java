/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.jaxb.hbm.internal;

import org.hibernate.EntityMode;
import org.hibernate.internal.log.DeprecationLogger;
import org.hibernate.internal.util.StringHelper;

/**
 * @deprecated for removal in 6.0
 */
@Deprecated
public class EntityModeConverter {
	public static EntityMode fromXml(String name) {
		final EntityMode entityMode = EntityMode.parse( name );
		if ( StringHelper.isNotEmpty( name ) ) {
			DeprecationLogger.DEPRECATION_LOGGER.info(
					"XML mapping specified an entity-mode - `%s`.  Starting in 6.0 this is simply inferred from the entity/composite mapping"
			);
		}
		return entityMode;
	}

	public static String toXml(EntityMode entityMode) {
		if ( entityMode == null ) {
			return null;
		}
		DeprecationLogger.DEPRECATION_LOGGER.info(
				"XML mapping specified an entity-mode - `%s`.  Starting in 6.0 this is simply inferred from the entity/composite mapping"
		);
		return entityMode.getExternalName();
	}
}
