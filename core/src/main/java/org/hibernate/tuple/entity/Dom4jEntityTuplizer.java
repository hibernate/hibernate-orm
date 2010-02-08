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
 *
 */
package org.hibernate.tuple.entity;

import org.hibernate.engine.SessionImplementor;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.proxy.ProxyFactory;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.dom4j.Dom4jProxyFactory;
import org.hibernate.property.PropertyAccessor;
import org.hibernate.property.PropertyAccessorFactory;
import org.hibernate.property.Getter;
import org.hibernate.property.Setter;
import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.EntityNameResolver;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.tuple.Instantiator;
import org.hibernate.tuple.Dom4jInstantiator;
import org.hibernate.type.AbstractComponentType;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;

/**
 * An {@link EntityTuplizer} specific to the dom4j entity mode.
 *
 * @author Steve Ebersole
 * @author Gavin King
 */
public class Dom4jEntityTuplizer extends AbstractEntityTuplizer {

	static final Logger log = LoggerFactory.getLogger( Dom4jEntityTuplizer.class );

	private Map inheritenceNodeNameMap = new HashMap();

	Dom4jEntityTuplizer(EntityMetamodel entityMetamodel, PersistentClass mappedEntity) {
		super( entityMetamodel, mappedEntity );
		inheritenceNodeNameMap.put( mappedEntity.getNodeName(), mappedEntity.getEntityName() );
		Iterator itr = mappedEntity.getSubclassClosureIterator();
		while( itr.hasNext() ) {
			final PersistentClass mapping = ( PersistentClass ) itr.next();
			inheritenceNodeNameMap.put( mapping.getNodeName(), mapping.getEntityName() );
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	public EntityMode getEntityMode() {
		return EntityMode.DOM4J;
	}

	private PropertyAccessor buildPropertyAccessor(Property mappedProperty) {
		if ( mappedProperty.isBackRef() ) {
			return mappedProperty.getPropertyAccessor(null);
		}
		else {
			return PropertyAccessorFactory.getDom4jPropertyAccessor( 
					mappedProperty.getNodeName(), 
					mappedProperty.getType(),
					getEntityMetamodel().getSessionFactory()
				);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	protected Getter buildPropertyGetter(Property mappedProperty, PersistentClass mappedEntity) {
		return buildPropertyAccessor(mappedProperty).getGetter( null, mappedProperty.getName() );
	}

	/**
	 * {@inheritDoc}
	 */
	protected Setter buildPropertySetter(Property mappedProperty, PersistentClass mappedEntity) {
		return buildPropertyAccessor(mappedProperty).getSetter( null, mappedProperty.getName() );
	}

	/**
	 * {@inheritDoc}
	 */
	protected Instantiator buildInstantiator(PersistentClass persistentClass) {
		return new Dom4jInstantiator( persistentClass );
	}

	/**
	 * {@inheritDoc}
	 */
	public Serializable getIdentifier(Object entityOrId) throws HibernateException {
		return getIdentifier( entityOrId, null );
	}

	/**
	 * {@inheritDoc}
	 */
	public Serializable getIdentifier(Object entityOrId, SessionImplementor session) {
		if ( entityOrId instanceof Element ) {
			return super.getIdentifier( entityOrId, session );
		}
		else {
			//it was not embedded, so the argument is just an id
			return (Serializable) entityOrId;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	protected ProxyFactory buildProxyFactory(PersistentClass mappingInfo, Getter idGetter, Setter idSetter) {
		HashSet proxyInterfaces = new HashSet();
		proxyInterfaces.add( HibernateProxy.class );
		proxyInterfaces.add( Element.class );

		ProxyFactory pf = new Dom4jProxyFactory();
		try {
			pf.postInstantiate(
					getEntityName(),
					Element.class,
					proxyInterfaces,
					null,
					null,
					mappingInfo.hasEmbeddedIdentifier() ?
			                (AbstractComponentType) mappingInfo.getIdentifier().getType() :
			                null
			);
		}
		catch ( HibernateException he ) {
			log.warn( "could not create proxy factory for:" + getEntityName(), he );
			pf = null;
		}
		return pf;
	}

	/**
	 * {@inheritDoc}
	 */
	public Class getMappedClass() {
		return Element.class;
	}

	/**
	 * {@inheritDoc}
	 */
	public Class getConcreteProxyClass() {
		return Element.class;
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
		return new EntityNameResolver[] { new BasicEntityNameResolver( getEntityName(), inheritenceNodeNameMap ) };
	}

	/**
	 * {@inheritDoc}
	 */
	public String determineConcreteSubclassEntityName(Object entityInstance, SessionFactoryImplementor factory) {
		return ( String ) inheritenceNodeNameMap.get( extractNodeName( ( Element ) entityInstance ) );
	}

	public static String extractNodeName(Element element) {
		return element.getName();
	}

	public static class BasicEntityNameResolver implements EntityNameResolver {
		private final String rootEntityName;
		private final Map nodeNameToEntityNameMap;

		public BasicEntityNameResolver(String rootEntityName, Map nodeNameToEntityNameMap) {
			this.rootEntityName = rootEntityName;
			this.nodeNameToEntityNameMap = nodeNameToEntityNameMap;
		}

		/**
		 * {@inheritDoc}
		 */
		public String resolveEntityName(Object entity) {
		return ( String ) nodeNameToEntityNameMap.get( extractNodeName( ( Element ) entity ) );
		}

		/**
		 * {@inheritDoc}
		 */
		public boolean equals(Object obj) {
			return rootEntityName.equals( ( ( BasicEntityNameResolver ) obj ).rootEntityName );
		}

		/**
		 * {@inheritDoc}
		 */
		public int hashCode() {
			return rootEntityName.hashCode();
		}
	}
}
