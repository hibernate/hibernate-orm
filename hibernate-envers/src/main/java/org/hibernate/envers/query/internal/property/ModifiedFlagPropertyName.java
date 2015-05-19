/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.query.internal.property;

import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.configuration.internal.metadata.MetadataTools;

/**
 * PropertyNameGetter for modified flags
 *
 * @author Michal Skowronek (mskowr at o2 dot pl)
 */
public class ModifiedFlagPropertyName implements PropertyNameGetter {
	private final PropertyNameGetter propertyNameGetter;

	public ModifiedFlagPropertyName(PropertyNameGetter propertyNameGetter) {
		this.propertyNameGetter = propertyNameGetter;
	}

	public String get(EnversService enversService) {
		return MetadataTools.getModifiedFlagPropertyName(
				propertyNameGetter.get( enversService ),
				enversService.getGlobalConfiguration().getModifiedFlagSuffix()
		);
	}
}
