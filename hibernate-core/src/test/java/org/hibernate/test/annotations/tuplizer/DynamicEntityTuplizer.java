//$Id$
package org.hibernate.test.annotations.tuplizer;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.property.Getter;
import org.hibernate.property.Setter;
import org.hibernate.proxy.ProxyFactory;
import org.hibernate.tuple.Instantiator;
import org.hibernate.tuple.entity.EntityMetamodel;
import org.hibernate.tuple.entity.PojoEntityTuplizer;

/**
 * @author Emmanuel Bernard
 */
public class DynamicEntityTuplizer extends PojoEntityTuplizer {

	public DynamicEntityTuplizer(EntityMetamodel entityMetamodel, PersistentClass mappedEntity) {
		super( entityMetamodel, mappedEntity );
	}

	public DynamicEntityTuplizer(EntityMetamodel entityMetamodel, EntityBinding mappedEntity) {
		super( entityMetamodel, mappedEntity );
	}

	protected Instantiator buildInstantiator(PersistentClass persistentClass) {
		return new DynamicInstantiator( persistentClass.getEntityName() );
	}

	@Override
	protected Instantiator buildInstantiator(EntityBinding entityBinding) {
		return new DynamicInstantiator( entityBinding.getEntityName() );
	}

	@Override
	protected ProxyFactory buildProxyFactoryInternal(EntityBinding entityBinding, Getter idGetter, Setter idSetter) {
		return super.buildProxyFactoryInternal( entityBinding, idGetter, idSetter );
	}

	protected ProxyFactory buildProxyFactory(PersistentClass persistentClass, Getter idGetter, Setter idSetter) {
			// allows defining a custom proxy factory, which is responsible for
			// generating lazy proxies for a given entity.
			//
			// Here we simply use the default...
			return super.buildProxyFactory( persistentClass, idGetter, idSetter );
		}
	}
