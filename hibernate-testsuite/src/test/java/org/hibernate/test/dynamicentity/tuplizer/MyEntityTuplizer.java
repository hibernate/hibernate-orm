package org.hibernate.test.dynamicentity.tuplizer;

import org.hibernate.tuple.entity.PojoEntityTuplizer;
import org.hibernate.tuple.entity.EntityMetamodel;
import org.hibernate.tuple.Instantiator;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.proxy.ProxyFactory;
import org.hibernate.property.Getter;
import org.hibernate.property.Setter;

/**
 * @author Steve Ebersole
 */
public class MyEntityTuplizer extends PojoEntityTuplizer {

	public MyEntityTuplizer(EntityMetamodel entityMetamodel, PersistentClass mappedEntity) {
		super( entityMetamodel, mappedEntity );
	}

	protected Instantiator buildInstantiator(PersistentClass persistentClass) {
		return new MyEntityInstantiator( persistentClass.getEntityName() );
	}

	protected ProxyFactory buildProxyFactory(PersistentClass persistentClass, Getter idGetter, Setter idSetter) {
		// allows defining a custom proxy factory, which is responsible for
		// generating lazy proxies for a given entity.
		//
		// Here we simply use the default...
		return super.buildProxyFactory( persistentClass, idGetter, idSetter );
	}
}
