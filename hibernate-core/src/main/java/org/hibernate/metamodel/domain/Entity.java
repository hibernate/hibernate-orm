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

/**
 * Models the notion of an entity
 *
 * @author Steve Ebersole
 */
public class Entity extends AbstractAttributeContainer {
	private final PojoEntitySpecifics pojoEntitySpecifics = new PojoEntitySpecifics();
	private final Dom4jEntitySpecifics dom4jEntitySpecifics = new Dom4jEntitySpecifics();
	private final MapEntitySpecifics mapEntitySpecifics = new MapEntitySpecifics();


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

	public Dom4jEntitySpecifics getDom4jEntitySpecifics() {
		return dom4jEntitySpecifics;
	}

	public MapEntitySpecifics getMapEntitySpecifics() {
		return mapEntitySpecifics;
	}

	public static interface EntityModeEntitySpecifics {
		public EntityMode getEntityMode();
		public String getTuplizerClassName();
	}

	public static class PojoEntitySpecifics implements EntityModeEntitySpecifics {
		private String tuplizerClassName;
		private String className;
		private String proxyInterfaceName;

		@Override
		public EntityMode getEntityMode() {
			return EntityMode.POJO;
		}

		public String getTuplizerClassName() {
			return tuplizerClassName;
		}

		public void setTuplizerClassName(String tuplizerClassName) {
			this.tuplizerClassName = tuplizerClassName;
		}

		public String getClassName() {
			return className;
		}

		public void setClassName(String className) {
			this.className = className;
		}

		public String getProxyInterfaceName() {
			return proxyInterfaceName;
		}

		public void setProxyInterfaceName(String proxyInterfaceName) {
			this.proxyInterfaceName = proxyInterfaceName;
		}
	}

	public static class Dom4jEntitySpecifics implements EntityModeEntitySpecifics {
		private String tuplizerClassName;
		private String nodeName;

		@Override
		public EntityMode getEntityMode() {
			return EntityMode.DOM4J;
		}

		public String getTuplizerClassName() {
			return tuplizerClassName;
		}

		public void setTuplizerClassName(String tuplizerClassName) {
			this.tuplizerClassName = tuplizerClassName;
		}

		public String getNodeName() {
			return nodeName;
		}

		public void setNodeName(String nodeName) {
			this.nodeName = nodeName;
		}
	}

	public static class MapEntitySpecifics implements EntityModeEntitySpecifics {
		private String tuplizerClassName;

		@Override
		public EntityMode getEntityMode() {
			return EntityMode.MAP;
		}

		public String getTuplizerClassName() {
			return tuplizerClassName;
		}

		public void setTuplizerClassName(String tuplizerClassName) {
			this.tuplizerClassName = tuplizerClassName;
		}
	}

}
