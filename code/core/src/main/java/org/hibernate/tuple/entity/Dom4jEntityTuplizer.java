// $Id: Dom4jEntityTuplizer.java 9210 2006-02-03 22:15:19Z steveebersole $
package org.hibernate.tuple.entity;

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
import org.hibernate.tuple.Instantiator;
import org.hibernate.tuple.Dom4jInstantiator;
import org.hibernate.type.AbstractComponentType;
import org.dom4j.Element;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.Iterator;

/**
 * An {@link EntityTuplizer} specific to the dom4j entity mode.
 *
 * @author Steve Ebersole
 * @author Gavin King
 */
public class Dom4jEntityTuplizer extends AbstractEntityTuplizer {

	static final Log log = LogFactory.getLog( Dom4jEntityTuplizer.class );

	private Set subclassNodeNames = new HashSet();

	Dom4jEntityTuplizer(EntityMetamodel entityMetamodel, PersistentClass mappedEntity) {
		super(entityMetamodel, mappedEntity);
		Iterator itr = mappedEntity.getSubclassClosureIterator();
		while( itr.hasNext() ) {
			final PersistentClass mapping = ( PersistentClass ) itr.next();
			subclassNodeNames.add( mapping.getNodeName() );
		}
	}
	
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

	protected Getter buildPropertyGetter(Property mappedProperty, PersistentClass mappedEntity) {
		return buildPropertyAccessor(mappedProperty).getGetter( null, mappedProperty.getName() );
	}

	protected Setter buildPropertySetter(Property mappedProperty, PersistentClass mappedEntity) {
		return buildPropertyAccessor(mappedProperty).getSetter( null, mappedProperty.getName() );
	}

	protected Instantiator buildInstantiator(PersistentClass persistentClass) {
		return new Dom4jInstantiator( persistentClass );
	}

	public Serializable getIdentifier(Object entityOrId) throws HibernateException {
		if (entityOrId instanceof Element) {
			return super.getIdentifier(entityOrId);
		}
		else {
			//it was not embedded, so the argument is just an id
			return (Serializable) entityOrId;
		}
	}
	
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

	public Class getMappedClass() {
		return Element.class;
	}

	public Class getConcreteProxyClass() {
		return Element.class;
	}

	public boolean isInstrumented() {
		return false;
	}
}
