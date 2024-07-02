/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.jaxb.hbm.transform;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.boot.jaxb.hbm.spi.EntityInfo;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmHibernateMapping;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmTypeDefinitionType;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEmbeddableImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;

/**
 * @author Steve Ebersole
 */
public class TransformationState {
	private final Map<JaxbHbmHibernateMapping,JaxbEntityMappingsImpl> jaxbRootMap = new HashMap<>();

	private final Map<String,JaxbHbmHibernateMapping> entityToHbmXmlMap = new HashMap<>();
	private final Map<String, JaxbEntityMappingsImpl> entityToMappingXmlMap = new HashMap<>();

	private final Map<String, JaxbEntityImpl> mappingEntityByName = new HashMap<>();
	private final Map<String, EntityInfo> hbmEntityByName = new HashMap<>();
	private final Map<String, EntityTypeInfo> entityInfoByName = new HashMap<>();

	private final Map<String, JaxbEmbeddableImpl> embeddableByName = new HashMap<>();
	private final Map<String, JaxbEmbeddableImpl> embeddableByRole = new HashMap<>();
	private final Map<String, ComponentTypeInfo> embeddableInfoByRole = new HashMap<>();

	private final Map<String, JaxbHbmTypeDefinitionType> typeDefMap = new HashMap<>();

	public TransformationState() {
	}

	public Map<JaxbHbmHibernateMapping, JaxbEntityMappingsImpl> getJaxbRootMap() {
		return jaxbRootMap;
	}

	public Map<String, JaxbHbmHibernateMapping> getEntityToHbmXmlMap() {
		return entityToHbmXmlMap;
	}

	public Map<String, JaxbEntityMappingsImpl> getEntityToMappingXmlMap() {
		return entityToMappingXmlMap;
	}

	public Map<String, JaxbEntityImpl> getMappingEntityByName() {
		return mappingEntityByName;
	}

	public Map<String, EntityInfo> getHbmEntityByName() {
		return hbmEntityByName;
	}

	public Map<String, EntityTypeInfo> getEntityInfoByName() {
		return entityInfoByName;
	}

	public Map<String, JaxbEmbeddableImpl> getEmbeddableByName() {
		return embeddableByName;
	}

	public Map<String, JaxbEmbeddableImpl> getEmbeddableByRole() {
		return embeddableByRole;
	}

	public Map<String, ComponentTypeInfo> getEmbeddableInfoByRole() {
		return embeddableInfoByRole;
	}

	public Map<String, JaxbHbmTypeDefinitionType> getTypeDefMap() {
		return typeDefMap;
	}
}
