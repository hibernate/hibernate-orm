/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.generator.internal;

import java.lang.reflect.Member;
import java.util.EnumSet;

import org.hibernate.PropertyValueException;
import org.hibernate.annotations.TenantId;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.generator.EventTypeSets;
import org.hibernate.generator.GeneratorCreationContext;
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
		final Object tenantId = session.getTenantIdentifierValue();
		if ( currentValue != null ) {
			final var resolver = sessionFactory.getCurrentTenantIdentifierResolver();
			if ( resolver != null && resolver.isRoot( tenantId ) ) {
				// the "root" tenant is allowed to set the tenant id explicitly
				return currentValue;
			}
			else {
				final JavaType<Object> tenantIdJavaType = sessionFactory.getTenantIdentifierJavaType();
				if ( !tenantIdJavaType.areEqual( currentValue, tenantId ) ) {
					throw new PropertyValueException(
							"assigned tenant id differs from current tenant id ["
									+ tenantIdJavaType.toString( currentValue )
									+ " != "
									+ tenantIdJavaType.toString( tenantId ) + "]",
							entityName,
							propertyName
					);
				}
			}
		}
		return tenantId;
	}
}
