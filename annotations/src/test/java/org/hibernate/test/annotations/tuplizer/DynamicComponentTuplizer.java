//$Id$
package org.hibernate.test.annotations.tuplizer;

import org.hibernate.tuple.entity.PojoEntityTuplizer;
import org.hibernate.tuple.entity.EntityMetamodel;
import org.hibernate.tuple.Instantiator;
import org.hibernate.tuple.component.PojoComponentTuplizer;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Component;
import org.hibernate.proxy.ProxyFactory;
import org.hibernate.property.Getter;
import org.hibernate.property.Setter;
import org.hibernate.HibernateException;

/**
 * @author Emmanuel Bernard
 */
public class DynamicComponentTuplizer extends PojoComponentTuplizer {

	public DynamicComponentTuplizer(Component component) {
		super( component );
	}


	protected Instantiator buildInstantiator(Component component) {
		return new DynamicInstantiator( component.getComponentClassName() );	//To change body of overridden methods use File | Settings | File Templates.
	}

}
