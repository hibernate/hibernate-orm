/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.query.internal.property;

import org.hibernate.envers.boot.AuditService;
import org.hibernate.envers.configuration.internal.metadata.MetadataTools;

/**
 * PropertyNameGetter for modified flags
 *
 * @author Michal Skowronek (mskowr at o2 dot pl)
 * @author Chris Cranford
 */
public class ModifiedFlagPropertyName implements PropertyNameGetter {
	private final PropertyNameGetter propertyNameGetter;

	public ModifiedFlagPropertyName(PropertyNameGetter propertyNameGetter) {
		this.propertyNameGetter = propertyNameGetter;
	}

	@Override
	public String get(AuditService auditService) {
		return MetadataTools.getModifiedFlagPropertyName(
				propertyNameGetter.get( auditService ),
				auditService.getOptions().getModifiedFlagSuffix()
		);
	}
}
