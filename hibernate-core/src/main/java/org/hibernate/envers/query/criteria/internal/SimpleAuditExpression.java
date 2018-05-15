/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.query.criteria.internal;

import java.util.Collection;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.internal.entities.RelationDescription;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;
import org.hibernate.envers.internal.tools.query.Parameters;
import org.hibernate.envers.internal.tools.query.QueryBuilder;
import org.hibernate.envers.query.internal.property.PropertyNameGetter;
import org.hibernate.metamodel.model.domain.internal.SingularPersistentAttributeEmbedded;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.PersistentAttributeDescriptor;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Chris Cranford
 */
public class SimpleAuditExpression extends AbstractAtomicExpression {
	private PropertyNameGetter propertyNameGetter;
	private Object value;
	private String op;

	public SimpleAuditExpression(String alias, PropertyNameGetter propertyNameGetter, Object value, String op) {
		super( alias );
		this.propertyNameGetter = propertyNameGetter;
		this.value = value;
		this.op = op;
	}

	@Override
	protected void addToQuery(
			AuditReaderImplementor versionsReader,
			String entityName,
			String alias,
			QueryBuilder qb,
			Parameters parameters) {
		String propertyName = CriteriaTools.determinePropertyName(
				versionsReader,
				entityName,
				propertyNameGetter
		);

		RelationDescription relatedEntity = CriteriaTools.getRelatedEntity(
				versionsReader.getAuditService(),
				entityName,
				propertyName
		);

		if ( relatedEntity == null ) {
			// HHH-9178 - Add support to component type equality.
			// This basically will allow = and <> operators to perform component-based equality checks.
			// Any other operator for a component type will not be supported.
			// Non-component types will continue to behave normally.
			final SessionImplementor session = versionsReader.getSessionImplementor();
			final PersistentAttributeDescriptor persistentAttribute = getPersistentAttribute( session, entityName, propertyName );
			if ( persistentAttribute != null && persistentAttribute instanceof SingularPersistentAttributeEmbedded ) {
				if ( !"=".equals( op ) && !"<>".equals( op ) ) {
					throw new AuditException( "Component-based criterion is not supported for op: " + op );
				}
				final SingularPersistentAttributeEmbedded embeddedAttribute = (SingularPersistentAttributeEmbedded) persistentAttribute;
				final Collection<PersistentAttributeDescriptor> attributes = embeddedAttribute.getEmbeddedDescriptor().getPersistentAttributes();
				for ( PersistentAttributeDescriptor attribute : attributes ) {
					final Object attributeValue = embeddedAttribute.getEmbeddedDescriptor().getPropertyValue( value, attribute.getName() );
					parameters.addWhereWithParam(
							alias,
							propertyName + "_" + attribute.getName(),
							op,
							attributeValue
					);
				}
			}
			else {
				parameters.addWhereWithParam( alias, propertyName, op, value );
			}
		}
		else {
			if ( !"=".equals( op ) && !"<>".equals( op ) ) {
				throw new AuditException(
						"This type of operation: " + op + " (" + entityName + "." + propertyName +
								") isn't supported and can't be used in queries."
				);
			}
			Object id = relatedEntity.getIdMapper().mapToIdFromEntity( value );
			relatedEntity.getIdMapper().addIdEqualsToQuery( parameters, id, alias, null, "=".equals( op ) );
		}
	}

	/**
	 * Get the attribute by name from the specified entity.
	 *
	 * @param session      the session
	 * @param entityName   the entity name
	 * @param attributeName the property attribute name
	 *
	 * @return the persistent attribute or {@code null} if the attribute isn't found.
	 */
	private PersistentAttributeDescriptor getPersistentAttribute(
			SessionImplementor session,
			String entityName,
			String attributeName) {
		final EntityTypeDescriptor entityDescriptor = session
				.getSessionFactory()
				.getMetamodel()
				.findEntityDescriptor( entityName );
		return entityDescriptor.findPersistentAttribute( attributeName );
	}
}
