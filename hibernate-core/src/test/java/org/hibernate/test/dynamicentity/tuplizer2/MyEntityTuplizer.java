/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.dynamicentity.tuplizer2;
import org.hibernate.EntityNameResolver;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.property.access.spi.Setter;
import org.hibernate.proxy.ProxyFactory;
import org.hibernate.test.dynamicentity.ProxyHelper;
import org.hibernate.test.dynamicentity.tuplizer.MyEntityInstantiator;
import org.hibernate.tuple.Instantiator;
import org.hibernate.tuple.entity.EntityMetamodel;
import org.hibernate.tuple.entity.PojoEntityTuplizer;

/**
 * @author Steve Ebersole
 */
public class MyEntityTuplizer extends PojoEntityTuplizer {

	public MyEntityTuplizer(EntityMetamodel entityMetamodel, PersistentClass mappedEntity) {
		super( entityMetamodel, mappedEntity );
	}

	public EntityNameResolver[] getEntityNameResolvers() {
		return new EntityNameResolver[] { MyEntityNameResolver.INSTANCE };
	}

	protected Instantiator buildInstantiator(PersistentClass persistentClass) {
		return new MyEntityInstantiator( persistentClass.getEntityName() );
	}

	public String determineConcreteSubclassEntityName(Object entityInstance, SessionFactoryImplementor factory) {
		String entityName = ProxyHelper.extractEntityName( entityInstance );
		if ( entityName == null ) {
			entityName = super.determineConcreteSubclassEntityName( entityInstance, factory );
		}
		return entityName;
	}

	protected ProxyFactory buildProxyFactory(PersistentClass persistentClass, Getter idGetter, Setter idSetter) {
		// allows defining a custom proxy factory, which is responsible for
		// generating lazy proxies for a given entity.
		//
		// Here we simply use the default...
		return super.buildProxyFactory( persistentClass, idGetter, idSetter );
	}

	public static class MyEntityNameResolver implements EntityNameResolver {
		public static final MyEntityNameResolver INSTANCE = new MyEntityNameResolver();

		public String resolveEntityName(Object entity) {
			return ProxyHelper.extractEntityName( entity );
		}

		public boolean equals(Object obj) {
			return getClass().equals( obj.getClass() );
		}

		public int hashCode() {
			return getClass().hashCode();
		}
	}
}
