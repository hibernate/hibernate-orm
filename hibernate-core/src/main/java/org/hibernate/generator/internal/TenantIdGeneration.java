/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.generator.internal;

import org.hibernate.MappingException;
import org.hibernate.PropertyValueException;
import org.hibernate.annotations.TenantId;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.EventType;
import org.hibernate.generator.EventTypeSets;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.GeneratorCreationContext;
import org.hibernate.type.descriptor.java.JavaType;

import java.lang.reflect.Member;
import java.util.EnumSet;

import static org.hibernate.generator.EventTypeSets.INSERT_ONLY;
import static org.hibernate.internal.util.ReflectHelper.getPropertyType;

/**
 * A generator that produces the current tenant identifier
 * to be assigned to the {@link TenantId} property.
 *
 * @author Gavin King
 */
public class TenantIdGeneration implements BeforeExecutionGenerator {

	private final String entityName;
	private final String propertyName;
	private final Class<?> propertyType;

	public TenantIdGeneration(TenantId annotation, Member member, GeneratorCreationContext context) {
		entityName = context.getPersistentClass() == null
				? member.getDeclaringClass().getName() //it's an attribute of an embeddable
				: context.getPersistentClass().getEntityName();
		propertyName = context.getProperty().getName();
		propertyType = getPropertyType( member );
	}

	/**
	 * @return {@link EventTypeSets#INSERT_ONLY}
	 */
	@Override
	public EnumSet<EventType> getEventTypes() {
		return INSERT_ONLY;
	}

	@Override
	public Object generate(SharedSessionContractImplementor session, Object owner, Object currentValue, EventType eventType) {
		SessionFactoryImplementor sessionFactory = session.getSessionFactory();
		JavaType<Object> descriptor = sessionFactory.getTypeConfiguration().getJavaTypeRegistry()
				.findDescriptor(propertyType);
		if ( descriptor==null ) {
			throw new MappingException( "unsupported tenant id property type: " + propertyType.getName() );
		}

		String tenantId = session.getTenantIdentifier(); //unfortunately this is always a string in old APIs
		if ( currentValue != null ) {
			CurrentTenantIdentifierResolver resolver = sessionFactory.getCurrentTenantIdentifierResolver();
			if ( resolver!=null && resolver.isRoot(tenantId) ) {
				// the "root" tenant is allowed to set the tenant id explicitly
				return currentValue;
			}
			String currentTenantId = descriptor.toString(currentValue);
			if ( !currentTenantId.equals(tenantId) ) {
				throw new PropertyValueException(
						"assigned tenant id differs from current tenant id: "
								+ currentTenantId + "!=" + tenantId,
						entityName, propertyName
				);
			}
		}
		return tenantId == null ? null : descriptor.fromString(tenantId); //convert to the model type
	}
}
