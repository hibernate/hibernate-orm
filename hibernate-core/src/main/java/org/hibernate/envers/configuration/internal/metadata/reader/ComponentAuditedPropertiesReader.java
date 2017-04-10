/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.configuration.internal.metadata.reader;

import javax.persistence.MappedSuperclass;

import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.envers.AuditOverride;
import org.hibernate.envers.Audited;
import org.hibernate.envers.boot.spi.AuditMetadataBuildingOptions;
import org.hibernate.envers.configuration.internal.metadata.MetadataTools;

/**
 * Reads the audited properties for components.
 *
 * @author Hern&aacut;n Chanfreau
 * @author Michal Skowronek (mskowr at o2 dot pl)
 * @author Chris Cranford
 */
public class ComponentAuditedPropertiesReader extends AuditedPropertiesReader {

	public ComponentAuditedPropertiesReader(
			PersistentPropertiesSource persistentPropertiesSource,
			AuditedPropertiesHolder auditedPropertiesHolder,
			AuditMetadataBuildingOptions options,
			ReflectionManager reflectionManager,
			String propertyNamePrefix) {
		super( persistentPropertiesSource, auditedPropertiesHolder, options, reflectionManager, propertyNamePrefix );
	}

	@Override
	protected void addPropertiesFromClass(XClass clazz) {
		final Audited allClassAudited = computeAuditConfiguration( clazz );

		//look in the class
		addFromProperties(
				clazz.getDeclaredProperties( "field" ),
				"field",
				fieldAccessedPersistentProperties,
				allClassAudited
		);
		addFromProperties(
				clazz.getDeclaredProperties( "property" ),
				"property",
				propertyAccessedPersistentProperties,
				allClassAudited
		);

		final XClass superclazz = clazz.getSuperclass();
		if ( !clazz.isInterface() && !"java.lang.Object".equals( superclazz.getName() ) ) {
			addPropertiesFromClass( superclazz );
		}
	}

	@Override
	protected boolean checkAudited(
			XProperty property,
			PropertyAuditingData propertyData, String propertyName,
			Audited allClassAudited, String modifiedFlagSuffix) {
		// Checking if this property is explicitly audited or if all properties are.
		final Audited aud = property.getAnnotation( Audited.class );
		if ( aud != null ) {
			propertyData.setRelationTargetAuditMode( aud.targetAuditMode() );
			propertyData.setUsingModifiedFlag( checkUsingModifiedFlag( aud ) );
			if ( aud.modifiedColumnName() != null && !"".equals( aud.modifiedColumnName() ) ) {
				propertyData.setModifiedFlagName( aud.modifiedColumnName() );
			}
			else {
				propertyData.setModifiedFlagName(
						MetadataTools.getModifiedFlagPropertyName( propertyName, modifiedFlagSuffix )
				);
			}
		}
		else {

			// get declaring class for property.
			final XClass declaringClazz = property.getDeclaringClass();
			final ComponentAuditingData auditingData = (ComponentAuditingData) auditedPropertiesHolder;

			// check component data to make sure that no audit overrides were not defined
			// parent class of the embeddable at the property level.
			boolean clazzAuditedOverride = false;
			boolean clazzNotAuditedOverride = false;
			for ( AuditOverride auditOverride : auditingData.getAuditingOverrides() ) {
				if ( auditOverride.forClass() != void.class ) {
					final String clazzName = auditOverride.forClass().getName();
					if ( declaringClazz.getName().equals( clazzName ) ) {
						if ( !auditOverride.isAudited() ) {
							if ( "".equals( auditOverride.name() ) ) {
								clazzNotAuditedOverride = true;
							}
							if ( property.getName().equals( auditOverride.name() ) ) {
								return false;
							}
						}
						else {
							if ( "".equals( auditOverride.name() ) ) {
								clazzAuditedOverride = true;
							}
							if ( property.getName().equals( auditOverride.name() ) ) {
								return true;
							}
						}
					}
				}
			}

			// makes sure that if the class or property are explicitly 'isAudited=false', use that.
			if ( clazzNotAuditedOverride
					|| overriddenNotAuditedProperties.contains( property )
					|| overriddenNotAuditedClasses.contains( declaringClazz ) ) {
				return false;
			}

			// makes sure that if the class or property are explicitly 'isAudited=true', use that.
			if ( clazzAuditedOverride
					|| overriddenAuditedProperties.contains( property )
					|| overriddenAuditedClasses.contains( declaringClazz ) ) {
				return true;
			}

			// assumption here is if a component reader is looking at a @MappedSuperclass, it should
			// be treated as not being audited if we reach this point, allowing components and any
			// @Embeddable class being audited by default.
			if ( declaringClazz.isAnnotationPresent( MappedSuperclass.class ) ) {
				return false;
			}
		}
		return true;
	}

}
