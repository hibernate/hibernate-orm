/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.internal;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.EntityNameResolver;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metamodel.spi.EntityInstantiator;

/**
 * Support for instantiating entity values as dynamic-map representation
 *
 * @author Steve Ebersole
 */
public class EntityInstantiatorDynamicMap
		extends AbstractDynamicMapInstantiator
		implements EntityInstantiator {
	private final Set<String> entityRoleNames = new HashSet<>();

	public EntityInstantiatorDynamicMap(PersistentClass bootDescriptor) {
		super( bootDescriptor.getEntityName() );

		entityRoleNames.add( getRoleName() );
		if ( bootDescriptor.hasSubclasses() ) {
			for ( PersistentClass subclassInfo : bootDescriptor.getSubclassClosure() ) {
				entityRoleNames.add( subclassInfo.getEntityName() );
			}
		}
	}

	@Override
	public Object instantiate(SessionFactoryImplementor sessionFactory) {
		return generateDataMap();
	}

	@Override
	protected boolean isSameRole(String type) {
		return super.isSameRole( type ) || isPartOfHierarchy( type );
	}

	private boolean isPartOfHierarchy(String type) {
		return entityRoleNames.contains( type );
	}

	public static final EntityNameResolver ENTITY_NAME_RESOLVER = entity -> {
		if ( ! (entity instanceof Map ) ) {
			return null;
		}
		final String entityName = extractEmbeddedEntityName( (Map<?,?>) entity );
		if ( entityName == null ) {
			throw new HibernateException( "Could not determine type of dynamic map entity" );
		}
		return entityName;
	};

	public static String extractEmbeddedEntityName(Map<?,?> entity) {
		if ( entity == null ) {
			return null;
		}
		final String entityName = (String) entity.get( TYPE_KEY );
		if ( entityName == null ) {
			throw new HibernateException( "Could not determine type of dynamic map entity" );
		}
		return entityName;
	}
}
