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
package org.hibernate.test.dynamicentity.tuplizer2;

import org.hibernate.tuple.entity.PojoEntityTuplizer;
import org.hibernate.tuple.entity.EntityMetamodel;
import org.hibernate.tuple.Instantiator;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.proxy.ProxyFactory;
import org.hibernate.property.Getter;
import org.hibernate.property.Setter;
import org.hibernate.test.dynamicentity.tuplizer.MyEntityInstantiator;
import org.hibernate.test.dynamicentity.ProxyHelper;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.EntityNameResolver;

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