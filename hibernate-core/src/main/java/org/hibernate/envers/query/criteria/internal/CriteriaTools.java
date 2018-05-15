/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.query.criteria.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.envers.boot.AuditService;
import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.internal.entities.RelationDescription;
import org.hibernate.envers.internal.entities.RelationType;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;
import org.hibernate.envers.query.criteria.AuditId;
import org.hibernate.envers.query.internal.property.PropertyNameGetter;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityIdentifier;
import org.hibernate.metamodel.model.domain.spi.EntityIdentifierCompositeNonAggregated;
import org.hibernate.metamodel.model.domain.spi.PersistentAttributeDescriptor;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Chris Cranford
 */
public abstract class CriteriaTools {
	public static void checkPropertyNotARelation(
			AuditService auditService,
			String entityName,
			String propertyName) throws AuditException {
		if ( auditService.getEntityBindings().get( entityName ).isRelation( propertyName ) ) {
			throw new AuditException(
					"This criterion cannot be used on a property that is " +
							"a relation to another property."
			);
		}
	}

	public static RelationDescription getRelatedEntity(
			AuditService auditService,
			String entityName,
			String propertyName) throws AuditException {
		RelationDescription relationDesc = auditService.getEntityBindings().getRelationDescription( entityName, propertyName );

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

	public static String determinePropertyName(
			AuditReaderImplementor versionsReader,
			String entityName,
			PropertyNameGetter propertyNameGetter) {
		final AuditService auditService = versionsReader.getAuditService();
		return determinePropertyName( versionsReader, entityName, propertyNameGetter.get( auditService ) );
	}

	/**
	 * @param versionsReader Versions reader.
	 * @param entityName Original entity name (not audited).
	 * @param propertyName Property name or placeholder.
	 *
	 * @return Path to property. Handles identifier placeholder used by {@link org.hibernate.envers.query.criteria.AuditId}.
	 */
	public static String determinePropertyName(
			AuditReaderImplementor versionsReader,
			String entityName,
			String propertyName) {

		final SessionFactoryImplementor sessionFactory = versionsReader.getSessionImplementor().getFactory();
		final AuditService auditService = versionsReader.getAuditService();

		if ( AuditId.IDENTIFIER_PLACEHOLDER.equals( propertyName ) ) {
			final String identifierPropertyName = sessionFactory.getMetamodel()
					.findEntityDescriptor( entityName ).getIdentifierPropertyName();
			propertyName = auditService.getOptions().getOriginalIdPropName() + "." + identifierPropertyName;
		}
		else {
			final List<String> identifierPropertyNames = identifierPropertyNames( sessionFactory, entityName );
			if ( identifierPropertyNames.contains( propertyName ) ) {
				propertyName = auditService.getOptions().getOriginalIdPropName() + "." + propertyName;
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
		final String identifierPropertyName = sessionFactory.getMetamodel()
				.findEntityDescriptor( entityName ).getIdentifierPropertyName();
		if ( identifierPropertyName != null ) {
			// Single id.
			return Arrays.asList( identifierPropertyName );
		}
		final EntityTypeDescriptor<?> entityDescriptor = sessionFactory.getMetamodel().findEntityDescriptor( entityName );
		final EntityIdentifier entityIdentifier = entityDescriptor.getIdentifierDescriptor();

		if ( entityIdentifier instanceof EntityIdentifierCompositeNonAggregated ) {
			// Multiple ids.
			final EntityIdentifierCompositeNonAggregated<?,?> embeddedId = EntityIdentifierCompositeNonAggregated.class.cast( entityIdentifier );
			final ArrayList<String> result = new ArrayList<>();
			for ( PersistentAttributeDescriptor<?, ?> attribute : embeddedId.getEmbeddedDescriptor().getPersistentAttributes() ) {
				result.add( attribute.getAttributeName() );
			}
			return result;
		}
		return Collections.emptyList();
	}
}
