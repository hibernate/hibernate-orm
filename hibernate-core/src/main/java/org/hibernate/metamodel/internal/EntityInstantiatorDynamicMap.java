package org.hibernate.metamodel.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.EntityNameResolver;
import org.hibernate.HibernateException;
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

	public EntityInstantiatorDynamicMap(@Nonnull PersistentClass bootDescriptor) {
		super( bootDescriptor.getEntityName() );
		entityRoleNames.add( getRoleName() );
		if ( bootDescriptor.hasSubclasses() ) {
			for ( var subclass : bootDescriptor.getSubclassClosure() ) {
				entityRoleNames.add( subclass.getEntityName() );
			}
		}
	}

	@Override
	@Nonnull
	public Object instantiate() {
		return generateDataMap();
	}

	@Override
	protected boolean isSameRole(@Nullable String type) {
		return super.isSameRole( type ) || isPartOfHierarchy( type );
	}

	private boolean isPartOfHierarchy(@Nullable String type) {
		return entityRoleNames.contains( type );
	}

	public static final EntityNameResolver ENTITY_NAME_RESOLVER =
			entity -> entity instanceof Map<?, ?> map ? extractEmbeddedEntityName( map ) : null;

	@Nonnull
	public static String extractEmbeddedEntityName(@Nonnull Map<?,?> entity) {
		if ( entity == null ) {
			throw new HibernateException( "Could not determine type of dynamic map entity" );
		}
		else {
			final String entityName = (String) entity.get( TYPE_KEY );
			if ( entityName == null ) {
				throw new HibernateException( "Could not determine type of dynamic map entity" );
			}
			return entityName;
		}
	}
}
