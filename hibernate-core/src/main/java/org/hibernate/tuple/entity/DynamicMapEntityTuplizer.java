/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.tuple.entity;
import java.util.Map;

import org.jboss.logging.Logger;

import org.hibernate.EntityMode;
import org.hibernate.EntityNameResolver;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.binding.AttributeBinding;
import org.hibernate.metamodel.binding.EntityBinding;
import org.hibernate.property.Getter;
import org.hibernate.property.PropertyAccessor;
import org.hibernate.property.PropertyAccessorFactory;
import org.hibernate.property.Setter;
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

    private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class,
                                                                       DynamicMapEntityTuplizer.class.getName());

	DynamicMapEntityTuplizer(EntityMetamodel entityMetamodel, PersistentClass mappedEntity) {
		super(entityMetamodel, mappedEntity);
	}

	DynamicMapEntityTuplizer(EntityMetamodel entityMetamodel, EntityBinding mappedEntity) {
		super(entityMetamodel, mappedEntity);
	}

	/**
	 * {@inheritDoc}
	 */
	public EntityMode getEntityMode() {
		return EntityMode.MAP;
	}

	private PropertyAccessor buildPropertyAccessor(Property mappedProperty) {
		if ( mappedProperty.isBackRef() ) {
			return mappedProperty.getPropertyAccessor(null);
		}
		else {
			return PropertyAccessorFactory.getDynamicMapPropertyAccessor();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
    protected Getter buildPropertyGetter(Property mappedProperty, PersistentClass mappedEntity) {
		return buildPropertyAccessor(mappedProperty).getGetter( null, mappedProperty.getName() );
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
    protected Setter buildPropertySetter(Property mappedProperty, PersistentClass mappedEntity) {
		return buildPropertyAccessor(mappedProperty).getSetter( null, mappedProperty.getName() );
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
    protected Instantiator buildInstantiator(PersistentClass mappingInfo) {
        return new DynamicMapInstantiator( mappingInfo );
	}

	/**
	 * {@inheritDoc}
	 */
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
		catch ( HibernateException he ) {
			LOG.unableToCreateProxyFactory( getEntityName(), he );
			pf = null;
		}
		return pf;
	}

	private PropertyAccessor buildPropertyAccessor(AttributeBinding mappedProperty) {
		// TODO: fix when backrefs are working in new metamodel
		//if ( mappedProperty.isBackRef() ) {
		//	return mappedProperty.getPropertyAccessor( null );
		//}
		//else {
			return PropertyAccessorFactory.getDynamicMapPropertyAccessor();
		//}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Getter buildPropertyGetter(AttributeBinding mappedProperty) {
		return buildPropertyAccessor( mappedProperty ).getGetter( null, mappedProperty.getAttribute().getName() );
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Setter buildPropertySetter(AttributeBinding mappedProperty) {
		return buildPropertyAccessor( mappedProperty ).getSetter( null, mappedProperty.getAttribute().getName() );
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Instantiator buildInstantiator(EntityBinding mappingInfo) {
		return new DynamicMapInstantiator( mappingInfo );
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected ProxyFactory buildProxyFactory(EntityBinding mappingInfo, Getter idGetter, Setter idSetter) {

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
		catch ( HibernateException he ) {
			LOG.unableToCreateProxyFactory(getEntityName(), he);
			pf = null;
		}
		return pf;
	}

	/**
	 * {@inheritDoc}
	 */
	public Class getMappedClass() {
		return Map.class;
	}

	/**
	 * {@inheritDoc}
	 */
	public Class getConcreteProxyClass() {
		return Map.class;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isInstrumented() {
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	public EntityNameResolver[] getEntityNameResolvers() {
		return new EntityNameResolver[] { BasicEntityNameResolver.INSTANCE };
	}

	/**
	 * {@inheritDoc}
	 */
	public String determineConcreteSubclassEntityName(Object entityInstance, SessionFactoryImplementor factory) {
		return extractEmbeddedEntityName( ( Map ) entityInstance );
	}

	public static String extractEmbeddedEntityName(Map entity) {
		return ( String ) entity.get( DynamicMapInstantiator.KEY );
	}

	public static class BasicEntityNameResolver implements EntityNameResolver {
		public static final BasicEntityNameResolver INSTANCE = new BasicEntityNameResolver();

		/**
		 * {@inheritDoc}
		 */
		public String resolveEntityName(Object entity) {
			if ( ! Map.class.isInstance( entity ) ) {
				return null;
			}
			final String entityName = extractEmbeddedEntityName( ( Map ) entity );
			if ( entityName == null ) {
				throw new HibernateException( "Could not determine type of dynamic map entity" );
			}
			return entityName;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
        public boolean equals(Object obj) {
			return getClass().equals( obj.getClass() );
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
        public int hashCode() {
			return getClass().hashCode();
		}
	}
}
