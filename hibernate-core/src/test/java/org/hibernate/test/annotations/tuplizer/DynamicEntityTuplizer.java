/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.tuplizer;

import org.hibernate.mapping.PersistentClass;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.property.access.spi.Setter;
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

		protected Instantiator buildInstantiator(PersistentClass persistentClass) {
			return new DynamicInstantiator( persistentClass.getEntityName() );
		}

		protected ProxyFactory buildProxyFactory(PersistentClass persistentClass, Getter idGetter, Setter idSetter) {
			// allows defining a custom proxy factory, which is responsible for
			// generating lazy proxies for a given entity.
			//
			// Here we simply use the default...
			return super.buildProxyFactory( persistentClass, idGetter, idSetter );
		}
	}
