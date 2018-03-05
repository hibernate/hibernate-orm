/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.tuplizer.bytebuddysubclass;

import org.hibernate.EntityNameResolver;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.tuple.Instantiator;
import org.hibernate.tuple.entity.EntityMetamodel;
import org.hibernate.tuple.entity.PojoEntityTuplizer;

/**
 * @author Florian Bien
 */
public class MyTuplizer extends PojoEntityTuplizer {
	public MyTuplizer(
			EntityMetamodel entityMetamodel,
			PersistentClass mappedEntity) {
		super( entityMetamodel, mappedEntity );
	}

	public EntityNameResolver[] getEntityNameResolvers() {
		return new EntityNameResolver[] { MyEntityNameResolver.INSTANCE };
	}

	@Override
	protected Instantiator buildInstantiator(EntityMetamodel entityMetamodel, PersistentClass persistentClass) {
		return new MyEntityInstantiator( persistentClass );
	}

	public static class MyEntityNameResolver implements EntityNameResolver {
		public static final MyEntityNameResolver INSTANCE = new MyEntityNameResolver();

		public String resolveEntityName(Object entity) {
			if ( entity.getClass().getName().contains( "$ByteBuddy$" ) ) {
				return entity.getClass().getSuperclass().getName();
			}
			else {
				return entity.getClass().getName();
			}

		}

		public boolean equals(Object obj) {
			return getClass().equals( obj.getClass() );
		}

		public int hashCode() {
			return getClass().hashCode();
		}
	}
}
