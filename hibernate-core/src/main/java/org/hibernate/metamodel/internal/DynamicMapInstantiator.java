/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.internal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.hibernate.EntityNameResolver;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metamodel.spi.Instantiator;

/**
 * @author Steve Ebersole
 */
public class DynamicMapInstantiator implements Instantiator<Map> {
	public static final EntityNameResolver ENTITY_NAME_RESOLVER = entity -> {
		if ( ! (entity instanceof Map) ) {
			return null;
		}
		final String entityName = extractEmbeddedEntityName( (Map) entity );
		if ( entityName == null ) {
			throw new HibernateException( "Could not determine type of dynamic map entity" );
		}
		return entityName;
	};

	public static final String KEY = "$type$";

	private final String roleName;
	private final Set<String> isInstanceEntityNames = new HashSet<>();

	public DynamicMapInstantiator(Component bootMapping) {
		this.roleName = bootMapping.getRoleName();
	}

	public DynamicMapInstantiator(PersistentClass bootMapping) {
		this.roleName = bootMapping.getEntityName();

		isInstanceEntityNames.add( roleName );
		if ( bootMapping.hasSubclasses() ) {
			Iterator itr = bootMapping.getSubclassClosureIterator();
			while ( itr.hasNext() ) {
				final PersistentClass subclassInfo = ( PersistentClass ) itr.next();
				isInstanceEntityNames.add( subclassInfo.getEntityName() );
			}
		}
	}

	@Override
	public Map instantiate(SharedSessionContractImplementor session) {
		Map map = generateMap();
		if ( roleName != null ) {
			//noinspection unchecked
			map.put( KEY, roleName );
		}
		return map;
	}

	@SuppressWarnings("WeakerAccess")
	protected Map generateMap() {
		return new HashMap();
	}

	@Override
	public boolean isInstance(Object object, SessionFactoryImplementor sessionFactory) {
		if ( object instanceof Map ) {
			if ( roleName == null ) {
				return true;
			}
			final String type = (String) ( (Map) object ).get( KEY );
			return type == null || isInstanceEntityNames.contains( type );
		}
		else {
			return false;
		}
	}

	public static class BasicEntityNameResolver implements EntityNameResolver {
		public static final BasicEntityNameResolver INSTANCE = new BasicEntityNameResolver();

		@Override
		public String resolveEntityName(Object entity) {
			if ( ! (entity instanceof Map) ) {
				return null;
			}
			final String entityName = extractEmbeddedEntityName( (Map) entity );
			if ( entityName == null ) {
				throw new HibernateException( "Could not determine type of dynamic map entity" );
			}
			return entityName;
		}

		@Override
		public boolean equals(Object obj) {
			return obj != null && getClass().equals( obj.getClass() );
		}

		@Override
		public int hashCode() {
			return getClass().hashCode();
		}
	}

	public static String extractEmbeddedEntityName(Map entity) {
		if ( entity == null ) {
			return null;
		}
		final String entityName = (String) entity.get( KEY );
		if ( entityName == null ) {
			throw new HibernateException( "Could not determine type of dynamic map entity" );
		}
		return entityName;
	}

}
