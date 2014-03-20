/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.envers.configuration.internal.metadata.reader;

import java.util.List;
import java.util.Map;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;

import org.hibernate.envers.Audited;
import org.hibernate.envers.ModificationStore;
import org.hibernate.envers.configuration.spi.AuditConfiguration;
import org.hibernate.envers.event.spi.EnversDotNames;
import org.hibernate.metamodel.spi.domain.Attribute;

/**
 * Reads the audited properties for components.
 *
 * @author Hern&aacut;n Chanfreau
 * @author Michal Skowronek (mskowr at o2 dot pl)
 */
public class ComponentAuditedPropertiesReader extends AuditedPropertiesReader {

	public ComponentAuditedPropertiesReader(
			AuditConfiguration.AuditConfigurationContext context,
			AuditedPropertiesHolder auditedPropertiesHolder,
			PersistentPropertiesSource persistentPropertiesSource,
			String propertyNamePrefix) {
		super(
				context, auditedPropertiesHolder, persistentPropertiesSource, propertyNamePrefix
		);
	}

	@Override
	protected boolean checkAudited(
			Attribute attribute,
			PropertyAuditingData propertyData,
			Audited allClassAudited) {
		// Checking if this property is explicitly audited or if all properties are.
		final Map<DotName, List<AnnotationInstance>> attributeAnnotations =
				getContext().locateAttributeAnnotations( attribute );
		// Checking if this property is explicitly audited or if all properties are.
		if ( attributeAnnotations.containsKey( EnversDotNames.AUDITED ) ) {
			final Audited aud =  getContext().getAnnotationProxy(
					attributeAnnotations.get( EnversDotNames.AUDITED ).get( 0 ),
					Audited.class
			);
			propertyData.setStore( aud.modStore() );
			propertyData.setRelationTargetAuditMode( aud.targetAuditMode() );
			propertyData.setUsingModifiedFlag( checkUsingModifiedFlag( aud ) );
		}
		else {
			propertyData.setStore( ModificationStore.FULL );
		}
		return true;
	}

}
