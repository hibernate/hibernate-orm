/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tuple.entity;

import java.util.Map;

import org.hibernate.EntityMode;
import org.hibernate.EntityNameResolver;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.property.access.internal.PropertyAccessStrategyMapImpl;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.property.access.spi.Setter;
import org.hibernate.proxy.ProxyFactory;
import org.hibernate.proxy.map.MapProxyFactory;
import org.hibernate.tuple.DynamicMapInstantiator;
import org.hibernate.tuple.Instantiator;

/**
 * An {@link EntityTuplizer} specific to the dynamic-map entity mode.
 *
 * @author Steve Ebersole
 * @author Gavin King
 */
public class DynamicMapEntityTuplizer extends AbstractEntityTuplizer {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( DynamicMapEntityTuplizer.class );

	DynamicMapEntityTuplizer(EntityMetamodel entityMetamodel, PersistentClass mappedEntity) {
		super( entityMetamodel, mappedEntity );
	}

	@Override
	public EntityMode getEntityMode() {
		return EntityMode.MAP;
	}

	private PropertyAccess buildPropertyAccess(Property mappedProperty) {
		if ( mappedProperty.isBackRef() ) {
			return mappedProperty.getPropertyAccessStrategy( null ).buildPropertyAccess( null, mappedProperty.getName() );
		}
		else {
			return PropertyAccessStrategyMapImpl.INSTANCE.buildPropertyAccess( null, mappedProperty.getName() );
		}
	}

	@Override
	protected Getter buildPropertyGetter(Property mappedProperty, PersistentClass mappedEntity) {
		return buildPropertyAccess( mappedProperty ).getGetter();
	}

	@Override
	protected Setter buildPropertySetter(Property mappedProperty, PersistentClass mappedEntity) {
		return buildPropertyAccess( mappedProperty ).getSetter();
	}

	@Override
	protected Instantiator buildInstantiator(EntityMetamodel entityMetamodel, PersistentClass mappingInfo) {
		return new DynamicMapInstantiator( mappingInfo );
	}

	@Override
	protected ProxyFactory buildProxyFactory(PersistentClass mappingInfo, Getter idGetter, Setter idSetter) {

		ProxyFactory pf = new MapProxyFactory();
		try {
			//TODO: design new lifecycle for ProxyFactory
			pf.postInstantiate(
					getEntityName(),
					null,
					null,
					null,
					null,
					null
			);
		}
		catch (HibernateException he) {
			LOG.unableToCreateProxyFactory( getEntityName(), he );
			pf = null;
		}
		return pf;
	}

	@Override
	public Class getMappedClass() {
		return Map.class;
	}

	@Override
	public Class getConcreteProxyClass() {
		return Map.class;
	}

	@Override
	public EntityNameResolver[] getEntityNameResolvers() {
		return new EntityNameResolver[] {BasicEntityNameResolver.INSTANCE};
	}

	@Override
	public String determineConcreteSubclassEntityName(Object entityInstance, SessionFactoryImplementor factory) {
		return extractEmbeddedEntityName( (Map) entityInstance );
	}

	public static String extractEmbeddedEntityName(Map entity) {
		return (String) entity.get( DynamicMapInstantiator.KEY );
	}

	public static class BasicEntityNameResolver implements EntityNameResolver {
		public static final BasicEntityNameResolver INSTANCE = new BasicEntityNameResolver();

		@Override
		public String resolveEntityName(Object entity) {
			if ( !Map.class.isInstance( entity ) ) {
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
}
