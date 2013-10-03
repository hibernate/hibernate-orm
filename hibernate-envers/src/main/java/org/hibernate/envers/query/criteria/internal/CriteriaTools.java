/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
package org.hibernate.envers.query.criteria.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.envers.configuration.spi.AuditConfiguration;
import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.internal.entities.RelationDescription;
import org.hibernate.envers.internal.entities.RelationType;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;
import org.hibernate.envers.query.criteria.AuditId;
import org.hibernate.envers.query.internal.property.PropertyNameGetter;
import org.hibernate.type.EmbeddedComponentType;
import org.hibernate.type.Type;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public abstract class CriteriaTools {
	public static void checkPropertyNotARelation(
			AuditConfiguration verCfg, String entityName,
			String propertyName) throws AuditException {
		if ( verCfg.getEntCfg().get( entityName ).isRelation( propertyName ) ) {
			throw new AuditException(
					"This criterion cannot be used on a property that is " +
							"a relation to another property."
			);
		}
	}

	public static RelationDescription getRelatedEntity(
			AuditConfiguration verCfg, String entityName,
			String propertyName) throws AuditException {
		RelationDescription relationDesc = verCfg.getEntCfg().getRelationDescription( entityName, propertyName );

		if ( relationDesc == null ) {
			return null;
		}

		if ( relationDesc.getRelationType() == RelationType.TO_ONE ) {
			return relationDesc;
		}

		throw new AuditException(
				"This type of relation (" + entityName + "." + propertyName +
						") isn't supported and can't be used in queries."
		);
	}

	/**
	 * @see #determinePropertyName(AuditConfiguration, AuditReaderImplementor, String, String)
	 */
	public static String determinePropertyName(
			AuditConfiguration auditCfg, AuditReaderImplementor versionsReader,
			String entityName, PropertyNameGetter propertyNameGetter) {
		return determinePropertyName( auditCfg, versionsReader, entityName, propertyNameGetter.get( auditCfg ) );
	}

	/**
	 * @param auditCfg Audit configuration.
	 * @param versionsReader Versions reader.
	 * @param entityName Original entity name (not audited).
	 * @param propertyName Property name or placeholder.
	 *
	 * @return Path to property. Handles identifier placeholder used by {@link org.hibernate.envers.query.criteria.AuditId}.
	 */
	public static String determinePropertyName(
			AuditConfiguration auditCfg, AuditReaderImplementor versionsReader,
			String entityName, String propertyName) {
		final SessionFactoryImplementor sessionFactory = versionsReader.getSessionImplementor().getFactory();

		if ( AuditId.IDENTIFIER_PLACEHOLDER.equals( propertyName ) ) {
			final String identifierPropertyName = sessionFactory.getEntityPersister( entityName ).getIdentifierPropertyName();
			propertyName = auditCfg.getAuditEntCfg().getOriginalIdPropName() + "." + identifierPropertyName;
		}
		else {
			final List<String> identifierPropertyNames = identifierPropertyNames( sessionFactory, entityName );
			if ( identifierPropertyNames.contains( propertyName ) ) {
				propertyName = auditCfg.getAuditEntCfg().getOriginalIdPropName() + "." + propertyName;
			}
		}

		return propertyName;
	}

	/**
	 * @param sessionFactory Session factory.
	 * @param entityName Entity name.
	 *
	 * @return List of property names representing entity identifier.
	 */
	private static List<String> identifierPropertyNames(SessionFactoryImplementor sessionFactory, String entityName) {
		final String identifierPropertyName = sessionFactory.getEntityPersister( entityName ).getIdentifierPropertyName();
		if ( identifierPropertyName != null ) {
			// Single id.
			return Arrays.asList( identifierPropertyName );
		}
		final Type identifierType = sessionFactory.getEntityPersister( entityName ).getIdentifierType();
		if ( identifierType instanceof EmbeddedComponentType ) {
			// Multiple ids.
			final EmbeddedComponentType embeddedComponentType = (EmbeddedComponentType) identifierType;
			return Arrays.asList( embeddedComponentType.getPropertyNames() );
		}
		return Collections.EMPTY_LIST;
	}
}
