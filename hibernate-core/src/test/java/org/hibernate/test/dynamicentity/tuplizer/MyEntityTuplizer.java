package org.hibernate.test.dynamicentity.tuplizer;

import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.property.Getter;
import org.hibernate.property.Setter;
import org.hibernate.proxy.ProxyFactory;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tuple.Instantiator;
import org.hibernate.tuple.entity.EntityMetamodel;
import org.hibernate.tuple.entity.PojoEntityTuplizer;

/**
 * @author Steve Ebersole
 */
public class MyEntityTuplizer extends PojoEntityTuplizer {
	public MyEntityTuplizer(ServiceRegistry serviceRegistry, EntityMetamodel entityMetamodel, EntityBinding entityBinding) {
		super( serviceRegistry, entityMetamodel, entityBinding);
	}

	@Override
	protected Instantiator buildInstantiator(EntityBinding entityBinding) {
		return new MyEntityInstantiator( entityBinding.getEntityName() );
	}

	@Override
	protected ProxyFactory buildProxyFactory(EntityBinding entityBinding, Getter idGetter, Setter idSetter) {
		return super.buildProxyFactory( entityBinding, idGetter, idSetter );
	}

}
