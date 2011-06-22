/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
 */
package org.hibernate.metamodel.domain;

import org.hibernate.EntityMode;
import org.hibernate.MappingException;
import org.hibernate.service.classloading.spi.ClassLoaderService;
import org.hibernate.tuple.entity.EntityTuplizer;

/**
 * Models the notion of an entity
 *
 * @author Steve Ebersole
 * @author Hardy Ferentschik
 */
public class Entity extends AbstractAttributeContainer {
	private final PojoEntitySpecifics pojoEntitySpecifics = new PojoEntitySpecifics();
	private final MapEntitySpecifics mapEntitySpecifics = new MapEntitySpecifics();

	/**
	 * Constructor for the entity
	 *
	 * @param name the name of the entity
	 * @param superType the super type for this entity. If there is not super type {@code null} needs to be passed.
	 */
	public Entity(String name, Hierarchical superType) {
		super( name, superType );
	}

	/**
	 * {@inheritDoc}
	 */
	public TypeNature getNature() {
		return TypeNature.ENTITY;
	}

	public PojoEntitySpecifics getPojoEntitySpecifics() {
		return pojoEntitySpecifics;
	}

	public MapEntitySpecifics getMapEntitySpecifics() {
		return mapEntitySpecifics;
	}

	public static interface EntityModeEntitySpecifics {
		public EntityMode getEntityMode();

		public String getTuplizerClassName();

		public Class<EntityTuplizer> getTuplizerClass();
	}

	public static class PojoEntitySpecifics implements EntityModeEntitySpecifics {
		private JavaType tuplizerClass;
		private JavaType entityClass;
		private JavaType proxyInterface;

		@Override
		public EntityMode getEntityMode() {
			return EntityMode.POJO;
		}

		public String getTuplizerClassName() {
			return tuplizerClass.getName();
		}

		public void setTuplizerClassName(String tuplizerClassName, ClassLoaderService classLoaderService) {
			this.tuplizerClass = new JavaType( tuplizerClassName, classLoaderService);
		}

		@SuppressWarnings( {"unchecked"} )
		public Class<EntityTuplizer> getTuplizerClass() {
			Class clazz = tuplizerClass.getClassReference();
			if ( ! EntityTuplizer.class.isAssignableFrom( clazz ) ) {
				throw new MappingException( "Class does not implement EntityTuplizer" );
			}
			return ( Class<EntityTuplizer> ) clazz;
		}

		public String getClassName() {
			return entityClass.getName();
		}

		public void setClassName(String className, ClassLoaderService classLoaderService) {
			this.entityClass = new JavaType( className, classLoaderService );
		}

		public Class<?> getEntityClass() {
			return entityClass.getClassReference();
		}

		public String getProxyInterfaceName() {
			return proxyInterface.getName();
		}

		public void setProxyInterfaceName(String proxyInterfaceName, ClassLoaderService classLoaderService) {
			this.proxyInterface = new JavaType( proxyInterfaceName, classLoaderService );
		}

		public Class<?> getProxyInterfaceClass() {
			return proxyInterface.getClassReference();
		}
	}


	public static class MapEntitySpecifics implements EntityModeEntitySpecifics {
		private JavaType tuplizerClass;

		@Override
		public EntityMode getEntityMode() {
			return EntityMode.MAP;
		}

		public String getTuplizerClassName() {
			return tuplizerClass.getName();
		}

		public void setTuplizerClassName(String tuplizerClassName, ClassLoaderService classLoaderService) {
			this.tuplizerClass = new JavaType( tuplizerClassName, classLoaderService );
		}

		@SuppressWarnings( {"unchecked"} )
		public Class<EntityTuplizer> getTuplizerClass() {
			Class clazz = tuplizerClass.getClassReference();
			if ( ! EntityTuplizer.class.isAssignableFrom( clazz ) ) {
				throw new MappingException( "Class does not implement EntityTuplizer" );
			}
			return ( Class<EntityTuplizer> ) clazz;
		}

	}

}
