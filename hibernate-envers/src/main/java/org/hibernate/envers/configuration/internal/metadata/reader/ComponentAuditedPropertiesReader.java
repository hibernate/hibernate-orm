/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.configuration.internal.metadata.reader;

import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.envers.Audited;
import org.hibernate.envers.ModificationStore;
import org.hibernate.envers.configuration.internal.GlobalConfiguration;
import org.hibernate.envers.configuration.internal.metadata.MetadataTools;

/**
 * Reads the audited properties for components.
 *
 * @author Hern&aacut;n Chanfreau
 * @author Michal Skowronek (mskowr at o2 dot pl)
 */
public class ComponentAuditedPropertiesReader extends AuditedPropertiesReader {

	public ComponentAuditedPropertiesReader(
			ModificationStore defaultStore,
			PersistentPropertiesSource persistentPropertiesSource,
			AuditedPropertiesHolder auditedPropertiesHolder,
			GlobalConfiguration globalCfg, ReflectionManager reflectionManager,
			String propertyNamePrefix) {
		super(
				defaultStore, persistentPropertiesSource, auditedPropertiesHolder,
				globalCfg, reflectionManager, propertyNamePrefix
		);
	}

	@Override
	protected boolean checkAudited(
			XProperty property,
			PropertyAuditingData propertyData, String propertyName,
			Audited allClassAudited, String modifiedFlagSuffix) {
		// Checking if this property is explicitly audited or if all properties are.
		final Audited aud = property.getAnnotation( Audited.class );
		if ( aud != null ) {
			propertyData.setStore( aud.modStore() );
			propertyData.setRelationTargetAuditMode( aud.targetAuditMode() );
			propertyData.setUsingModifiedFlag( checkUsingModifiedFlag( aud ) );
			if(aud.modifiedColumnName() != null && !"".equals(aud.modifiedColumnName())) {
				propertyData.setModifiedFlagName(aud.modifiedColumnName());
			}
			else {
				propertyData.setModifiedFlagName(
						MetadataTools.getModifiedFlagPropertyName( propertyName, modifiedFlagSuffix )
				);
			}
		}
		else {
			propertyData.setStore( ModificationStore.FULL );
		}
		return true;
	}

}
