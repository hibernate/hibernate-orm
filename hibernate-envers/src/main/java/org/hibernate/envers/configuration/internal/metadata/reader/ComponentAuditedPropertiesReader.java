/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.configuration.internal.metadata.reader;

import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.envers.Audited;
import org.hibernate.envers.boot.internal.ModifiedColumnNameResolver;
import org.hibernate.envers.boot.spi.EnversMetadataBuildingContext;
import org.hibernate.internal.util.StringHelper;

/**
 * Reads the audited properties for components.
 *
 * @author Hern&aacut;n Chanfreau
 * @author Michal Skowronek (mskowr at o2 dot pl)
 * @author Chris Cranford
 */
public class ComponentAuditedPropertiesReader extends AuditedPropertiesReader {

	public ComponentAuditedPropertiesReader(
			EnversMetadataBuildingContext metadataBuildingContext,
			PersistentPropertiesSource persistentPropertiesSource,
			AuditedPropertiesHolder auditedPropertiesHolder) {
		this( metadataBuildingContext, persistentPropertiesSource, auditedPropertiesHolder, NO_PREFIX );
	}

	public ComponentAuditedPropertiesReader(
			EnversMetadataBuildingContext metadataBuildingContext,
			PersistentPropertiesSource persistentPropertiesSource,
			AuditedPropertiesHolder auditedPropertiesHolder,
			String propertyNamePrefix) {
		super(
				metadataBuildingContext,
				persistentPropertiesSource,
				auditedPropertiesHolder,
				propertyNamePrefix
		);
	}

	@Override
	protected boolean checkAudited(
			XProperty property,
			PropertyAuditingData propertyData,
			String propertyName,
			Audited allClassAudited,
			String modifiedFlagSuffix) {
		// Checking if this property is explicitly audited or if all properties are.
		final Audited aud = property.getAnnotation( Audited.class );
		if ( aud != null ) {
			propertyData.setRelationTargetAuditMode( aud.targetAuditMode() );
			propertyData.setUsingModifiedFlag( checkUsingModifiedFlag( aud ) );
			propertyData.setModifiedFlagName( ModifiedColumnNameResolver.getName( propertyName, modifiedFlagSuffix ) );
			if ( StringHelper.isNotEmpty( aud.modifiedColumnName() ) ) {
				propertyData.setExplicitModifiedFlagName( aud.modifiedColumnName() );
			}
		}
		return true;
	}

}
