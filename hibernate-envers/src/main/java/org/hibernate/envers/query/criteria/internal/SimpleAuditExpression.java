/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.query.criteria.internal;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.internal.entities.RelationDescription;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;
import org.hibernate.envers.internal.tools.query.Parameters;
import org.hibernate.envers.internal.tools.query.QueryBuilder;
import org.hibernate.envers.query.internal.property.PropertyNameGetter;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.ComponentType;
import org.hibernate.type.Type;

/**
 * @author Adam Warski (adam at warski dot org)
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
			EnversService enversService,
			AuditReaderImplementor versionsReader,
			String entityName,
			String alias,
			QueryBuilder qb,
			Parameters parameters) {
		String propertyName = CriteriaTools.determinePropertyName(
				enversService,
				versionsReader,
				entityName,
				propertyNameGetter
		);

		RelationDescription relatedEntity = CriteriaTools.getRelatedEntity( enversService, entityName, propertyName );

		if ( relatedEntity == null ) {
			// HHH-9178 - Add support to component type equality.
			// This basically will allow = and <> operators to perform component-based equality checks.
			// Any other operator for a component type will not be supported.
			// Non-component types will continue to behave normally.
			final SessionImplementor session = versionsReader.getSessionImplementor();
			final Type type = getPropertyType( session, entityName, propertyName );
			if ( type != null && type.isComponentType() ) {
				if ( !"=".equals( op ) && !"<>".equals( op ) ) {
					throw new AuditException( "Component-based criterion is not supported for op: " + op );
				}
				final ComponentType componentType = (ComponentType) type;
				for ( int i = 0; i < componentType.getPropertyNames().length; i++ ) {
					final Object componentValue = componentType.getPropertyValue( value, i, session );
					parameters.addWhereWithParam(
							alias,
							propertyName + "_" + componentType.getPropertyNames()[ i ],
							op,
							componentValue
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
	 * Get the property type of a given property in the specified entity.
	 *
	 * @param session      the session
	 * @param entityName   the entity name
	 * @param propertyName the property name
	 * @return the property type of the property or {@code null} if the property name isn't found.
	 */
	private Type getPropertyType(SessionImplementor session, String entityName, String propertyName) {
		// rather than rely on QueryException from calling getPropertyType(), this allows a non-failure way
		// to determine whether to return null or lookup the value safely.
		final EntityPersister persister = session.getSessionFactory().getMetamodel().entityPersister( entityName );
		for ( String name : persister.getPropertyNames() ) {
			if ( name.equals( propertyName ) ) {
				return persister.getPropertyType( propertyName );
			}
		}
		return null;
	}
}
