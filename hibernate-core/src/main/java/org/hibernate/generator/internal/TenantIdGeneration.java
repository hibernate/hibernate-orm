/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.generator.internal;

import java.lang.reflect.Member;
import java.util.EnumSet;

import org.hibernate.PropertyValueException;
import org.hibernate.annotations.TenantId;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.generator.EventTypeSets;
import org.hibernate.generator.GeneratorCreationContext;
import org.hibernate.id.factory.spi.CustomIdGeneratorCreationContext;
import org.hibernate.type.descriptor.java.JavaType;

import static org.hibernate.generator.EventTypeSets.INSERT_ONLY;

/**
 * A generator that produces the current tenant identifier
 * to be assigned to the {@link TenantId} property.
 *
 * @author Gavin King
 */
public class TenantIdGeneration implements BeforeExecutionGenerator {

	private final String entityName;
	private final String propertyName;

	public TenantIdGeneration(TenantId annotation, Member member, CustomIdGeneratorCreationContext context) {
		this(annotation, member, (GeneratorCreationContext) context);
	}

	public TenantIdGeneration(TenantId annotation, Member member, GeneratorCreationContext context) {
		entityName = context.getPersistentClass() == null
				? member.getDeclaringClass().getName() //it's an attribute of an embeddable
				: context.getPersistentClass().getEntityName();
		propertyName = context.getProperty().getName();
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
		final SessionFactoryImplementor sessionFactory = session.getSessionFactory();
		final JavaType<Object> tenantIdentifierJavaType = sessionFactory.getTenantIdentifierJavaType();

		final Object tenantId = session.getTenantIdentifierValue();
		if ( currentValue != null ) {
			final CurrentTenantIdentifierResolver<Object> resolver = sessionFactory.getCurrentTenantIdentifierResolver();
			if ( resolver != null && resolver.isRoot( tenantId ) ) {
				// the "root" tenant is allowed to set the tenant id explicitly
				return currentValue;
			}
			if ( !tenantIdentifierJavaType.areEqual( currentValue, tenantId ) ) {
				throw new PropertyValueException(
						"assigned tenant id differs from current tenant id: " +
								tenantIdentifierJavaType.toString( currentValue ) +
								"!=" +
								tenantIdentifierJavaType.toString( tenantId ),
						entityName,
						propertyName
				);
			}
		}
		return tenantId;
	}
}
